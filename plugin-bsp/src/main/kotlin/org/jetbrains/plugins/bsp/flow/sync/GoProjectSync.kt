package org.jetbrains.plugins.bsp.flow.sync

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourcesItem
import com.goide.vgo.project.workspaceModel.entities.VgoDependencyEntity
import com.goide.vgo.project.workspaceModel.entities.VgoStandaloneModuleEntity
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.jps.entities.*
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModule
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.bspBuildToolId
import org.jetbrains.plugins.bsp.extension.points.goSdkExtension
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ResourceRoot
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.RawUriToDirectoryPathTransformer
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.plugins.bsp.ui.console.syncConsole
import org.jetbrains.plugins.bsp.ui.console.withSubtask
import org.jetbrains.workspacemodel.entities.BspEntitySource
import java.net.URI
import java.util.concurrent.CompletableFuture
import kotlin.io.path.toPath

class GoProjectSync : ProjectSyncHook {
  override val buildToolId: BuildToolId = bspBuildToolId

  override fun isEnabled(project: Project): Boolean = BspFeatureFlags.isGoSupportEnabled

  override suspend fun onSync(
    project: Project,
    server: JoinedBuildServer,
    capabilities: BazelBuildServerCapabilities,
    diff: AllProjectStructuresDiff,
    taskId: String,
    progressReporter: SequentialProgressReporter,
    baseTargetInfos: BaseTargetInfos,
    cancelOn: CompletableFuture<Void>,
    errorCallback: (Throwable) -> Unit
  ) {
    val goTargets = baseTargetInfos.calculateGoTargets()
    val goTargetsMap = goTargets.associateBy({ it.target.id }, { it })
    val virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()

//    println(goTargets)

    goTargets.forEach {
      addSourcesAndResourcesFromTarget(
        builder = diff.workspaceModelDiff.mutableEntityStorage,
        target = it,
        virtualFileUrlManager = virtualFileUrlManager,
      )
      val goModuleEntities = prepareAllGoEntities(it, virtualFileUrlManager, goTargetsMap, server, capabilities,
        cancelOn, errorCallback)
      diff.workspaceModelDiff.mutableEntityStorage.addEntity(goModuleEntities.goModuleWorkspaceEntity)
      goModuleEntities.goDependenciesWorkspaceEntity?.forEach { goDependency ->
        diff.workspaceModelDiff.mutableEntityStorage.addEntity(goDependency)
      }
      goModuleEntities.goLibrariesWorkspaceEntity?.forEach {goLibrary ->
        diff.workspaceModelDiff.mutableEntityStorage.addEntity(goLibrary)
      }
    }

    diff.workspaceModelDiff.addPostApplyAction {
      if (BspFeatureFlags.isGoSupportEnabled) {
        calculateAndAddGoSdks(goTargets, project, taskId)
        goSdkExtension()?.restoreGoModulesRegistry(project)
        enableGoSupportInTargets(project, diff, taskId)
      }
    }
  }

  private fun addSourcesAndResourcesFromTarget(
    builder: MutableEntityStorage,
    target: BaseTargetInfo,
    virtualFileUrlManager: VirtualFileUrlManager,
  ) {
    val moduleEntity = ModuleEntity(
      name = target.target.displayName,
      dependencies = target.target.dependencies.map {
        ModuleDependency(
          module = ModuleId(it.uri),
          exported = true,
          scope = DependencyScope.COMPILE,
          productionOnTest = true,
        )
      },
      entitySource = BspEntitySource,
    ) {
      this.type = ModuleTypeId("WEB_MODULE")
    }
    target.sources.forEach { addSourcesItem(builder, moduleEntity, it, virtualFileUrlManager) }
    target.resources.forEach { addResourcesItem(builder, moduleEntity, it, virtualFileUrlManager) }
  }

  private fun addSourcesItem(
    builder: MutableEntityStorage,
    moduleEntity: ModuleEntity.Builder,
    sourcesItem: SourcesItem,
    virtualFileUrlManager: VirtualFileUrlManager
  ) {
    sourcesItem.sources.forEach { source ->
      val contentRootEntity = ContentRootEntity(
        url = URI.create(source.uri).toPath().toVirtualFileUrl(virtualFileUrlManager),
        excludedPatterns = ArrayList(),
        entitySource = moduleEntity.entitySource,
      ) {
        this.excludedUrls = listOf()
        this.module = moduleEntity
      }
      builder.addEntity(
        SourceRootEntity(
          url = URI.create(source.uri).toPath().toVirtualFileUrl(virtualFileUrlManager),
          rootTypeId = SourceRootTypeId("go-source"),
          entitySource = BspEntitySource,
        ) {
          this.contentRoot = contentRootEntity
        },
      )
    }
  }

  private fun addResourcesItem(
    builder: MutableEntityStorage,
    moduleEntity: ModuleEntity.Builder,
    resourcesItem: ResourcesItem,
    virtualFileUrlManager: VirtualFileUrlManager
  ) {
    resourcesItem.resources
      .map(this::toGoResourceRoot)
      .forEach { resource ->
        val contentRootEntity = ContentRootEntity(
          url = resource.resourcePath.toVirtualFileUrl(virtualFileUrlManager),
          excludedPatterns = listOf(),
          entitySource = moduleEntity.entitySource,
        ) {
          this.excludedUrls = listOf()
          this.module = moduleEntity
        }
        builder.addEntity(
          SourceRootEntity(
            url = resource.resourcePath.toVirtualFileUrl(virtualFileUrlManager),
            rootTypeId = resource.rootType,
            entitySource = BspEntitySource,
          ) {
            this.contentRoot = contentRootEntity
          },
        )
    }
  }

  private fun toGoResourceRoot(resourcePath: String) =
    ResourceRoot(
      resourcePath = RawUriToDirectoryPathTransformer.transform(resourcePath),
      rootType = SourceRootTypeId("go-resource"),
    )

  private fun BaseTargetInfos.calculateGoTargets(): List<BaseTargetInfo> =
    infos.filter { it.target.languageIds.contains("go") }

  private data class GoTargetEntities(
    val goModuleWorkspaceEntity: VgoStandaloneModuleEntity.Builder,
    val goDependenciesWorkspaceEntity: List<VgoDependencyEntity.Builder>?,
    val goLibrariesWorkspaceEntity: List<VgoDependencyEntity.Builder>?,
  )

  private fun prepareAllGoEntities(
    inputEntity: BaseTargetInfo,
    virtualFileUrlManager: VirtualFileUrlManager,
    goTargetsMap: Map<BuildTargetIdentifier, BaseTargetInfo>,
    server: JoinedBuildServer,
    capabilities: BazelBuildServerCapabilities,
    cancelOn: CompletableFuture<Void>,
    errorCallback: (Throwable) -> Unit,
  ): GoTargetEntities {
    val goBuildInfo = extractGoBuildTarget(inputEntity.target)

    val vgoModule = VgoStandaloneModuleEntity(
      moduleId = ModuleId(inputEntity.target.displayName),
      entitySource = BspEntitySource,
      importPath = goBuildInfo?.importPath ?: "",
      root = URI.create(inputEntity.target.baseDirectory).toPath().toVirtualFileUrl(virtualFileUrlManager),
    )

    val vgoModuleDependencies = inputEntity.target.dependencies.mapNotNull {
      val goDependencyBuildInfo = goTargetsMap[it]?.let { depId -> extractGoBuildTarget(depId.target) }
      goDependencyBuildInfo?.let { goDepBuildInfo ->
        VgoDependencyEntity(
          importPath = goDepBuildInfo.importPath,
          entitySource = BspEntitySource,
          isMainModule = false,
          internal = true,
        ) {
          this.module = vgoModule
          this.root = URI.create(inputEntity.target.baseDirectory).toPath().toVirtualFileUrl(virtualFileUrlManager)
        }
      }
    }

    val vgoModuleLibraries = queryGoLibraries(server, capabilities, cancelOn, errorCallback)?.libraries?.map {
      VgoDependencyEntity(
        importPath = it.goImportPath ?: "",
        entitySource = BspEntitySource,
        isMainModule = false,
        internal = false,
      ) {
        this.module = vgoModule
        this.root = it.goRoot?.toPath()?.toVirtualFileUrl(virtualFileUrlManager)
      }
    }

    return GoTargetEntities(
      goModuleWorkspaceEntity = vgoModule,
      goDependenciesWorkspaceEntity = vgoModuleDependencies,
      goLibrariesWorkspaceEntity = vgoModuleLibraries,
    )
  }

  private fun queryGoLibraries(
    server: JoinedBuildServer,
    capabilities: BazelBuildServerCapabilities,
    cancelOn: CompletableFuture<Void>,
    errorCallback: (Throwable) -> Unit
  ): WorkspaceLibrariesResult? =
      queryIf(
        capabilities.workspaceLibrariesProvider,
        "workspace/libraries",
        cancelOn,
        errorCallback
      ) {
        server.workspaceLibraries()
      }?.get()

  private suspend fun calculateAndAddGoSdks(
    goTargets: List<BaseTargetInfo>,
    project: Project,
    taskId: String,
  ) {
    goSdkExtension()?.let { extension ->
      reportSequentialProgress { reporter ->
        reporter.indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.go.sdk.infos")) {
          extension.calculateAllGoSdkInfos(goTargets.map { it.target }.toSet())
        }
      }
      project.syncConsole.withSubtask(
        taskId,
        "add-bsp-fetched-go-sdks",
        BspPluginBundle.message("console.task.model.add.go.fetched.sdks")
      ) {
        extension.addGoSdks(project)
      }
    }
  }

  private suspend fun enableGoSupportInTargets(
    project: Project,
    diff: AllProjectStructuresDiff,
    taskId: String,
  ) =
    goSdkExtension()?.let { extension ->
      project.syncConsole.withSubtask(
        taskId,
        "enable-go-support-in-targets",
        BspPluginBundle.message("console.task.model.add.go.support.in.targets"),
      ) {
        val workspaceModel = WorkspaceModel.getInstance(project) // TODO: try with diff instead
        workspaceModel.currentSnapshot.entities(ModuleEntity::class.java).forEach { moduleEntity ->
          moduleEntity.findModule(workspaceModel.currentSnapshot)?.let { module ->
            writeAction {
              extension.enableGoSupportForModule(module)
            }
          }
        }
      }
    }
}
