package org.jetbrains.plugins.bsp.flow.sync

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.goide.vgo.project.workspaceModel.entities.VgoDependencyEntity
import com.goide.vgo.project.workspaceModel.entities.VgoStandaloneModuleEntity
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModule
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.bspBuildToolId
import org.jetbrains.plugins.bsp.extension.points.goSdkExtension
import org.jetbrains.plugins.bsp.extension.points.goSdkExtensionExists
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff
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
    val goTargetsMap = goTargets.associateBy({it.target.id}, {it})
    val virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
//    diff.workspaceModelDiff.mutableEntityStorage.getVirtualFileUrlIndex()

//    println(goTargets)

    goTargets.forEach {
      val goModuleEntities = prepareAllGoEntities(it, virtualFileUrlManager, goTargetsMap)
      diff.workspaceModelDiff.mutableEntityStorage.addEntity(goModuleEntities.goModuleWorkspaceEntity)
      goModuleEntities.goDependenciesWorkspaceEntity.forEach { goDependency ->
        diff.workspaceModelDiff.mutableEntityStorage.addEntity(goDependency)
      }
//      goModuleEntities.goLibrariesWorkspaceEntity.forEach {goLibrary ->
//        diff.workspaceModelDiff.mutableEntityStorage.addEntity(goLibrary)
//      }
    }

//    diff.workspaceModelDiff.addPostApplyAction {
//      if (BspFeatureFlags.isGoSupportEnabled) {
//        calculateAndAddGoSdks(goTargets, project)
//        goSdkExtension()?.restoreGoModulesRegistry(project)
//        enableGoSupportInTargets(project, diff)
//      }
//    }
  }

  private fun BaseTargetInfos.calculateGoTargets(): List<BaseTargetInfo> =
    infos.filter { it.target.languageIds.contains("go") }

  private data class GoTargetEntities(
    val goModuleWorkspaceEntity: VgoStandaloneModuleEntity.Builder,
    val goDependenciesWorkspaceEntity: List<VgoDependencyEntity.Builder>,
//    val goLibrariesWorkspaceEntity: List<WorkspaceEntity>,
  )

  private fun prepareAllGoEntities(
    inputEntity: BaseTargetInfo,
    virtualFileUrlManager: VirtualFileUrlManager,
    goTargetsMap: Map<BuildTargetIdentifier, BaseTargetInfo>
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

//    val vgoModuleLibraries = inputEntity.target // TODO query libraries
//      prepareGoLibrariesEntities(vgoModule, inputEntity, virtualFileUrlManager)
//    entityToAdd.goLibraries.map {
//      VgoDependencyEntity(
//        importPath = it.goImportPath ?: "",
//        entitySource = BspEntitySource,
//        isMainModule = false,
//        internal = false,
//      ) {
//        this.module = vgoModule
//        this.root = it.goRoot?.toPath()?.toVirtualFileUrl(virtualFileUrlManager)
//      }
//    }

    return GoTargetEntities(
      goModuleWorkspaceEntity = vgoModule,
      goDependenciesWorkspaceEntity = vgoModuleDependencies,
//      goLibrariesWorkspaceEntity = vgoModuleLibraries,
    )
  }

  private suspend fun calculateAndAddGoSdks(goTargets: List<BaseTargetInfo>, project: Project) {
    if (goSdkExtensionExists()) {
      reportSequentialProgress { reporter ->
        reporter.indeterminateStep(text = BspPluginBundle.message("progress.bar.calculate.go.sdk.infos")) {
          goSdkExtension()?.calculateAllGoSdkInfos(goTargets.map { it.target }.toSet())
        }
      }
      goSdkExtension()?.addGoSdks(project)
    }
  }

  private fun enableGoSupportInTargets(project: Project, diff: AllProjectStructuresDiff) =
    goSdkExtension()?.let { extension ->
      diff.workspaceModelDiff.mutableEntityStorage.entities(ModuleEntity::class.java).forEach { moduleEntity ->
        moduleEntity.findModule(WorkspaceModel.getInstance(project).currentSnapshot)?.let { module ->
          extension.enableGoSupportForModule(module)
        }
      }
    }
}
