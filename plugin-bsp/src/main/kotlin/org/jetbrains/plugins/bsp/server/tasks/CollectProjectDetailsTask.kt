package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.OutputPathsParams
import ch.epfl.scala.bsp4j.OutputPathsResult
import ch.epfl.scala.bsp4j.PythonOptionsParams
import ch.epfl.scala.bsp4j.ScalacOptionsParams
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import kotlinx.coroutines.runInterruptible
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.DirectoryItem
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import org.jetbrains.bsp.protocol.utils.extractAndroidBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.jetbrains.bsp.protocol.utils.extractPythonBuildTarget
import org.jetbrains.bsp.protocol.utils.extractScalaBuildTarget
import org.jetbrains.plugins.bsp.android.AndroidSdk
import org.jetbrains.plugins.bsp.android.AndroidSdkGetterExtension
import org.jetbrains.plugins.bsp.android.androidSdkGetterExtension
import org.jetbrains.plugins.bsp.android.androidSdkGetterExtensionExists
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.extension.points.PythonSdkGetterExtension
import org.jetbrains.plugins.bsp.extension.points.pythonSdkGetterExtension
import org.jetbrains.plugins.bsp.extension.points.pythonSdkGetterExtensionExists
import org.jetbrains.plugins.bsp.flow.sync.BaseTargetInfo
import org.jetbrains.plugins.bsp.flow.sync.BaseTargetInfos
import org.jetbrains.plugins.bsp.flow.sync.queryIf
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.TargetIdToModuleEntitiesMap
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.LibraryGraph
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.androidJarToAndroidSdkName
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.projectNameToJdkName
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.scalaVersionToScalaSdkName
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesJava
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesPython
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesScala
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toBuildTargetInfo
import org.jetbrains.plugins.bsp.performance.testing.bspTracer
import org.jetbrains.plugins.bsp.scala.sdk.ScalaSdk
import org.jetbrains.plugins.bsp.scala.sdk.scalaSdkExtension
import org.jetbrains.plugins.bsp.scala.sdk.scalaSdkExtensionExists
import org.jetbrains.plugins.bsp.server.client.importSubtaskId
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.ui.console.syncConsole
import org.jetbrains.plugins.bsp.ui.console.withSubtask
import org.jetbrains.plugins.bsp.utils.SdkUtils
import org.jetbrains.plugins.bsp.utils.findLibraryNameProvider
import org.jetbrains.plugins.bsp.utils.findModuleNameProvider
import org.jetbrains.plugins.bsp.utils.orDefault
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlin.io.path.Path

public data class PythonSdk(
  val name: String,
  val interpreterUri: String,
  val dependencies: List<DependencySourcesItem>,
)

public class CollectProjectDetailsTask(
  project: Project,
  private val taskId: String,
  private val diff: MutableEntityStorage
) :
  BspServerTask<ProjectDetails>("collect project details", project) {

  private var uniqueJavaHomes: Set<String>? = null

  private var pythonSdks: Set<PythonSdk>? = null

  private var scalaSdks: Set<ScalaSdk>? = null

  private var androidSdks: Set<AndroidSdk>? = null

  suspend fun execute(
    server: JoinedBuildServer,
    capabilities: BazelBuildServerCapabilities,
    progressReporter: SequentialProgressReporter,
    baseTargetInfos: BaseTargetInfos,
    cancelOn: CompletableFuture<Void>,
    errorCallback: (Throwable) -> Unit,
  ) {
    val projectDetails =
      progressReporter.sizedStep(
        workSize = 50,
        text = BspPluginBundle.message("progress.bar.collect.project.details")
      ) {
        runInterruptible { collectModel(server, capabilities, baseTargetInfos, cancelOn, errorCallback) }
      } ?: return

    progressReporter.indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.jdk.infos")) {
      calculateAllUniqueJdkInfosSubtask(projectDetails)
      uniqueJavaHomes.orEmpty().also {
        if (it.isNotEmpty())
          projectDetails.defaultJdkName = project.name.projectNameToJdkName(it.first())
        else
          projectDetails.defaultJdkName = SdkUtils.getProjectJdkOrMostRecentJdk(project)?.name
      }
    }

    if (BspFeatureFlags.isPythonSupportEnabled && pythonSdkGetterExtensionExists()) {
      progressReporter.indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.python.sdk.infos")) {
        calculateAllPythonSdkInfosSubtask(projectDetails)
      }
    }

    if (BspFeatureFlags.isScalaSupportEnabled && scalaSdkExtensionExists()) {
      progressReporter.indeterminateStep(text = "Calculating all unique scala sdk infos") {
        calculateAllScalaSdkInfosSubtask(projectDetails)
      }
    }

    if (BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists()) {
      progressReporter.indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.android.sdk.infos")) {
        calculateAllAndroidSdkInfosSubtask(projectDetails)
      }
    }

    progressReporter.sizedStep(workSize = 25, text = BspPluginBundle.message("progress.bar.update.internal.model")) {
      updateInternalModelSubtask(projectDetails)
    }
  }

  private fun collectModel(
    server: JoinedBuildServer,
    capabilities: BazelBuildServerCapabilities,
    baseTargetInfos: BaseTargetInfos,
    cancelOn: CompletableFuture<Void>,
    errorCallback: (Throwable) -> Unit,
  ): ProjectDetails? {
    project.syncConsole.startSubtask(
      this.taskId, importSubtaskId,
      BspPluginBundle.message("console.task.model.collect.in.progress")
    )

    val projectDetails =
      calculateProjectDetailsWithCapabilities(
        project = project,
        server = server,
        buildServerCapabilities = capabilities,
        baseTargetInfos = baseTargetInfos,
        projectRootDir = project.rootDir.url,
        errorCallback = { errorCallback(it) },
        cancelOn = cancelOn,
      )

    project.syncConsole.finishSubtask(importSubtaskId, BspPluginBundle.message("console.task.model.collect.success"))

    return projectDetails
  }

  private suspend fun calculateAllUniqueJdkInfosSubtask(projectDetails: ProjectDetails) =
    project.syncConsole.withSubtask(
      taskId,
      "calculate-all-unique-jdk-infos",
      BspPluginBundle.message("console.task.model.calculate.jdks.infos")
    ) {
      uniqueJavaHomes = bspTracer.spanBuilder("calculate.all.unique.jdk.infos.ms").use {
        calculateAllUniqueJavaHomes(projectDetails)
      }
    }

  private fun calculateAllUniqueJavaHomes(projectDetails: ProjectDetails): Set<String> =
    projectDetails.targets.mapNotNull(::extractJvmBuildTarget).map { it.javaHome }.toSet()

  private suspend fun calculateAllScalaSdkInfosSubtask(projectDetails: ProjectDetails) =       project.syncConsole.withSubtask(
    taskId,
    "calculate-all-scala-sdk-infos",
    BspPluginBundle.message("console.task.model.calculate.scala.sdk.infos")
  ) {
    scalaSdks = bspTracer.spanBuilder("calculate.all.scala.sdk.infos.ms").use {
      calculateAllScalaSdkInfos(projectDetails)
    }
  }

  private fun calculateAllScalaSdkInfos(projectDetails: ProjectDetails): Set<ScalaSdk> =
    projectDetails.targets
      .mapNotNull {
        createScalaSdk(it)
      }
      .toSet()

  private fun createScalaSdk(target: BuildTarget): ScalaSdk? =
    extractScalaBuildTarget(target)
      ?.let {
        ScalaSdk(
          name = it.scalaVersion.scalaVersionToScalaSdkName(),
          scalaVersion = it.scalaVersion,
          sdkJars = it.jars
        )
      }

  private suspend fun calculateAllPythonSdkInfosSubtask(projectDetails: ProjectDetails) =       project.syncConsole.withSubtask(
    taskId,
    "calculate-all-python-sdk-infos",
    BspPluginBundle.message("console.task.model.calculate.python.sdks.done")
  ) {
    runInterruptible {
      pythonSdks = bspTracer.spanBuilder("calculate.all.python.sdk.infos.ms").use {
        calculateAllPythonSdkInfos(projectDetails)
      }
    }
  }

  private fun createPythonSdk(target: BuildTarget, dependenciesSources: List<DependencySourcesItem>): PythonSdk? =
    extractPythonBuildTarget(target)?.let {
      if (it.interpreter != null && it.version != null)
        PythonSdk(
          name = "${target.id.uri}-${it.version}",
          interpreterUri = it.interpreter,
          dependencies = dependenciesSources,
        )
      else
        pythonSdkGetterExtension()
          ?.getSystemSdk()
          ?.let { sdk ->
            PythonSdk(
              name = "${target.id.uri}-detected-PY3",
              interpreterUri = Path(sdk.homePath!!).toUri().toString(),
              dependencies = dependenciesSources
            )
          }
    }

  private fun calculateAllPythonSdkInfos(projectDetails: ProjectDetails): Set<PythonSdk> {
    return projectDetails.targets.mapNotNull {
      createPythonSdk(it, projectDetails.dependenciesSources.filter { a -> a.target.uri == it.id.uri })
    }
      .toSet()
  }

  private suspend fun calculateAllAndroidSdkInfosSubtask(projectDetails: ProjectDetails) =
    project.syncConsole.withSubtask(
      taskId,
      "calculate-all-android-sdk-infos",
      BspPluginBundle.message("progress.bar.calculate.android.sdk.infos"),
    ) {
      androidSdks = bspTracer.spanBuilder("calculate.all.android.sdk.infos.ms").use {
        calculateAllAndroidSdkInfos(projectDetails)
      }
    }

  private fun calculateAllAndroidSdkInfos(projectDetails: ProjectDetails): Set<AndroidSdk> =
    projectDetails.targets
      .mapNotNull { createAndroidSdk(it) }
      .toSet()

  private fun createAndroidSdk(target: BuildTarget): AndroidSdk? =
    extractAndroidBuildTarget(target)?.androidJar?.let { androidJar ->
      AndroidSdk(
        name = androidJar.androidJarToAndroidSdkName(),
        androidJar = androidJar,
      )
    }

  private suspend fun updateInternalModelSubtask(projectDetails: ProjectDetails) {
    project.syncConsole.withSubtask(
      taskId, "calculate-project-structure", BspPluginBundle.message("console.task.model.calculate.structure")
    ) {
      runInterruptible {
        val projectBasePath = project.rootDir.toNioPath()
        val moduleNameProvider = project.findModuleNameProvider().orDefault()
        val libraryNameProvider = project.findLibraryNameProvider().orDefault()
        val libraryGraph = LibraryGraph(projectDetails.libraries.orEmpty())

        val libraries = bspTracer.spanBuilder("create.libraries.ms").use {
          libraryGraph.createLibraries(libraryNameProvider)
        }

        val libraryModules = bspTracer.spanBuilder("create.library.modules.ms").use {
          libraryGraph.createLibraryModules(libraryNameProvider, projectDetails.defaultJdkName)
        }

        val targetIdToModuleDetails = bspTracer.spanBuilder("create.module.details.ms").use {
          val transformer = ProjectDetailsToModuleDetailsTransformer(projectDetails, libraryGraph)
          projectDetails.targetIds.associateWith { transformer.moduleDetailsForTargetId(it) }
        }

        val targetIdToModuleEntitiesMap = bspTracer.spanBuilder("create.target.id.to.module.entities.map.ms").use {
          val targetIdToTargetInfo = projectDetails.targets.associate { it.id to it.toBuildTargetInfo() }
          val targetIdToModuleEntityMap = TargetIdToModuleEntitiesMap(
            projectDetails = projectDetails,
            targetIdToModuleDetails = targetIdToModuleDetails,
            targetIdToTargetInfo = targetIdToTargetInfo,
            projectBasePath = projectBasePath,
            moduleNameProvider = moduleNameProvider,
            libraryNameProvider = libraryNameProvider,
            hasDefaultPythonInterpreter = BspFeatureFlags.isPythonSupportEnabled,
            isAndroidSupportEnabled = BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
          )

          project.temporaryTargetUtils.saveTargets(
            targetIdToTargetInfo,
            targetIdToModuleEntityMap,
            targetIdToModuleDetails,
            libraries,
            libraryModules,
          )

          targetIdToModuleEntityMap
        }

        bspTracer.spanBuilder("load.modules.ms").use {
          val workspaceModel = WorkspaceModel.getInstance(project)
          val virtualFileUrlManager = workspaceModel.getVirtualFileUrlManager()

          val workspaceModelUpdater = WorkspaceModelUpdater.create(
            diff,
            virtualFileUrlManager,
            projectBasePath,
            project,
            BspFeatureFlags.isPythonSupportEnabled,
            BspFeatureFlags.isAndroidSupportEnabled && androidSdkGetterExtensionExists(),
          )

          val modulesToLoad = targetIdToModuleEntitiesMap.values.toList()

          workspaceModelUpdater.loadModules(modulesToLoad + project.temporaryTargetUtils.getAllLibraryModules())
          workspaceModelUpdater.loadLibraries(project.temporaryTargetUtils.getAllLibraries())
          workspaceModelUpdater
            .loadDirectories(projectDetails.directories, projectDetails.outputPathUris, virtualFileUrlManager)
        }
      }
    }
  }

  private fun WorkspaceModelUpdater.loadDirectories(
    directories: WorkspaceDirectoriesResult,
    outputPathUris: List<String>,
    virtualFileUrlManager: VirtualFileUrlManager,
  ) {
    val includedDirectories = directories.includedDirectories.map { it.toVirtualFileUrl(virtualFileUrlManager) }
    val excludedDirectories = directories.excludedDirectories.map { it.toVirtualFileUrl(virtualFileUrlManager) }
    val outputPaths = outputPathUris.map { virtualFileUrlManager.getOrCreateFromUrl(it) }

    loadDirectories(includedDirectories, excludedDirectories + outputPaths)
  }

  private fun DirectoryItem.toVirtualFileUrl(virtualFileUrlManager: VirtualFileUrlManager): VirtualFileUrl =
    virtualFileUrlManager.getOrCreateFromUrl(uri)


  suspend fun postprocessingSubtask(progressReporter: SequentialProgressReporter) {
    progressReporter.sizedStep(workSize = 25, text = BspPluginBundle.message("progress.bar.post.processing")) {

      // This order is strict as now SDKs also use the workspace model,
      // updating jdks before applying the project model will render the action to fail.
      // This will be handled properly after this ticket:
      // https://youtrack.jetbrains.com/issue/BAZEL-426/Configure-JDK-using-workspace-model-API-instead-of-ProjectJdkTable

      project.temporaryTargetUtils.fireListeners()
      addBspFetchedJdks()

      if (BspFeatureFlags.isPythonSupportEnabled) {
        addBspFetchedPythonSdks()
      }

      if (BspFeatureFlags.isScalaSupportEnabled) {
        addBspFetchedScalaSdks()
      }

      if (BspFeatureFlags.isAndroidSupportEnabled) {
        addBspFetchedAndroidSdks()
      }
    }
  }

  private suspend fun addBspFetchedJdks() =
    project.syncConsole.withSubtask(
      taskId,
      "add-bsp-fetched-jdks",
      BspPluginBundle.message("console.task.model.add.fetched.jdks")
    ) {
      bspTracer.spanBuilder("add.bsp.fetched.jdks.ms").useWithScope {
        uniqueJavaHomes?.forEach {
          SdkUtils.addJdkIfNeeded(
            projectName = project.name,
            javaHomeUri = it
          )
        }
      }
    }

  private suspend fun addBspFetchedScalaSdks() {
    scalaSdkExtension()?.let { extension ->
      project.syncConsole.withSubtask(
        taskId, "add-bsp-fetched-scala-sdks", BspPluginBundle.message("console.task.model.add.scala.fetched.sdks")
      ) {
        val modifiableProvider = IdeModifiableModelsProviderImpl(project)

        writeAction {
          scalaSdks?.forEach { extension.addScalaSdk(it, modifiableProvider) }
          modifiableProvider.commit()
        }
      }
    }
  }

  private suspend fun addBspFetchedPythonSdks() {
    pythonSdkGetterExtension()?.let { extension ->
      project.syncConsole.withSubtask(
        taskId,
        "add-bsp-fetched-python-sdks",
        BspPluginBundle.message("console.task.model.add.python.fetched.sdks")
      ) {
        bspTracer.spanBuilder("add.bsp.fetched.python.sdks.ms").useWithScope {
          pythonSdks?.forEach { addPythonSdkIfNeeded(it, extension) }
        }
      }
    }
  }

  private suspend fun addPythonSdkIfNeeded(pythonSdk: PythonSdk, pythonSdkGetterExtension: PythonSdkGetterExtension) {
    val sdk = runInterruptible {
      pythonSdkGetterExtension.getPythonSdk(
        pythonSdk,
        WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
      )
    }

    SdkUtils.addSdkIfNeeded(sdk)
  }

  private suspend fun addBspFetchedAndroidSdks() {
    androidSdkGetterExtension()?.let { extension ->
      project.syncConsole.withSubtask(
        taskId,
        "add-bsp-fetched-android-sdks",
        BspPluginBundle.message("console.task.model.add.android.fetched.sdks"),
      ) {
        bspTracer.spanBuilder("add.bsp.fetched.android.sdks.ms").useWithScope {
          androidSdks?.forEach { addAndroidSdkIfNeeded(it, extension) }
        }
      }
    }
  }

  private suspend fun addAndroidSdkIfNeeded(
    androidSdk: AndroidSdk,
    androidSdkGetterExtension: AndroidSdkGetterExtension,
  ) {
    val sdk = writeAction { androidSdkGetterExtension.getAndroidSdk(androidSdk) } ?: return
    SdkUtils.addSdkIfNeeded(sdk)
  }
}

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
public fun calculateProjectDetailsWithCapabilities(
  project: Project,
  server: JoinedBuildServer,
  buildServerCapabilities: BazelBuildServerCapabilities,
  baseTargetInfos: BaseTargetInfos,
  projectRootDir: String,
  errorCallback: (Throwable) -> Unit,
  cancelOn: CompletableFuture<Void> = CompletableFuture(),
): ProjectDetails? {
  try {
    val dependencySourcesFuture =
      queryIf(
        buildServerCapabilities.dependencySourcesProvider == true,
        "buildTarget/dependencySources",
        cancelOn,
        errorCallback
      ) {
        server.buildTargetDependencySources(DependencySourcesParams(baseTargetInfos.allTargetIds))
      }

    val javaTargetIds = baseTargetInfos.infos.calculateJavaTargetIds()
    val scalaTargetIds = baseTargetInfos.infos.calculateScalaTargetIds()
    val libraries: WorkspaceLibrariesResult? =
      queryIf(buildServerCapabilities.workspaceLibrariesProvider, "workspace/libraries", cancelOn, errorCallback) {
        server.workspaceLibraries()
      }?.get()

    val directoriesFuture =
      queryIf(buildServerCapabilities.workspaceDirectoriesProvider, "workspace/directories", cancelOn, errorCallback) {
        server.workspaceDirectories()
      }

    val jvmBinaryJarsFuture = queryIf(
      BspFeatureFlags.isAndroidSupportEnabled &&
          buildServerCapabilities.jvmBinaryJarsProvider &&
          javaTargetIds.isNotEmpty(),
      "buildTarget/jvmBinaryJars",
      cancelOn, errorCallback
    ) {
      server.buildTargetJvmBinaryJars(JvmBinaryJarsParams(javaTargetIds))
    }

    // We use javacOptions only do build dependency tree based on classpath
    // If workspace/libraries endpoint is available (like in bazel-bsp)
    // we don't need javacOptions at all. For other servers (like SBT)
    // we still need to retrieve it
    // There's no capability for javacOptions
    val javacOptionsFuture = if (libraries == null)
      queryIf(javaTargetIds.isNotEmpty(), "buildTarget/javacOptions", cancelOn, errorCallback) {
        server.buildTargetJavacOptions(JavacOptionsParams(javaTargetIds))
      }
    else null

    // Same for Scala
    val scalacOptionsFuture = if (libraries == null)
      queryIf(
        scalaTargetIds.isNotEmpty() && BspFeatureFlags.isScalaSupportEnabled,
        "buildTarget/scalacOptions",
        cancelOn,
        errorCallback
      ) {
        server.buildTargetScalacOptions(ScalacOptionsParams(scalaTargetIds))
      }
    else null

    val pythonTargetsIds = baseTargetInfos.infos.calculatePythonTargetsIds()
    val pythonOptionsFuture =
      queryIf(
        pythonTargetsIds.isNotEmpty() && BspFeatureFlags.isPythonSupportEnabled,
        "buildTarget/pythonOptions",
        cancelOn,
        errorCallback
      ) {
        server.buildTargetPythonOptions(PythonOptionsParams(pythonTargetsIds))
      }

    val outputPathsFuture =
      queryIf(buildServerCapabilities.outputPathsProvider == true, "buildTarget/outputPaths", cancelOn, errorCallback) {
        server.buildTargetOutputPaths(OutputPathsParams(baseTargetInfos.allTargetIds))
      }

    return ProjectDetails(
      targetIds = baseTargetInfos.allTargetIds,
      targets = baseTargetInfos.infos.map { it.target }.toSet(),
      sources = baseTargetInfos.infos.flatMap { it.sources },
      resources = baseTargetInfos.infos.flatMap { it.resources },
      dependenciesSources = dependencySourcesFuture?.get()?.items ?: emptyList(),
      javacOptions = javacOptionsFuture?.get()?.items ?: emptyList(),
      scalacOptions = scalacOptionsFuture?.get()?.items ?: emptyList(),
      pythonOptions = pythonOptionsFuture?.get()?.items ?: emptyList(),
      outputPathUris = outputPathsFuture?.get()?.obtainDistinctUris() ?: emptyList(),
      libraries = libraries?.libraries,
      directories = directoriesFuture?.get()
        ?: WorkspaceDirectoriesResult(listOf(DirectoryItem(projectRootDir)), emptyList()),
      jvmBinaryJars = jvmBinaryJarsFuture?.get()?.items ?: emptyList(),
    )
  } catch (e: Exception) {
    // TODO the type xd
    if (e is ExecutionException && e.cause is CancellationException) {
      fileLogger().debug("calculateProjectDetailsWithCapabilities has been cancelled", e)
    } else {
      fileLogger().error("calculateProjectDetailsWithCapabilities has failed", e)
    }

    return null
  }
}

private fun List<BaseTargetInfo>.calculateJavaTargetIds(
): List<BuildTargetIdentifier> =
  filter { it.target.languageIds.includesJava() }.map { it.target.id }

private fun List<BaseTargetInfo>.calculateScalaTargetIds(): List<BuildTargetIdentifier> =
  filter { it.target.languageIds.includesScala() }.map { it.target.id }

private fun List<BaseTargetInfo>.calculatePythonTargetsIds(): List<BuildTargetIdentifier> =
  filter { it.target.languageIds.includesPython() }.map { it.target.id }

public fun <T> CompletableFuture<T>.catchSyncErrors(errorCallback: (Throwable) -> Unit): CompletableFuture<T> =
  this.whenComplete { _, exception ->
    exception?.let { errorCallback(it) }
  }

private fun OutputPathsResult.obtainDistinctUris(): List<String> =
  this.items
    .filterNotNull()
    .flatMap { it.outputPaths }
    .map { it.uri }
    .distinct()
