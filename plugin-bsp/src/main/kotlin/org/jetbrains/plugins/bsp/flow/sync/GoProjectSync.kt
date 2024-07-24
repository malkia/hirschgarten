package org.jetbrains.plugins.bsp.flow.sync

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.workspace.storage.WorkspaceEntity
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.bspBuildToolId
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.workspacemodel.entities.BspEntitySource
import java.util.concurrent.CompletableFuture

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
    val goTargetIds = baseTargetInfos.calculateGoTargetIds()

    goTargetIds.forEach {
      val goModuleEntities = prepareAllGoEntities(it)
//      goModuleEntities.goDependenciesWorkspaceEntity.forEach {goDependency ->
//        diff.workspaceModelDiff.mutableEntityStorage.addEntity(goDependency)
//      }
//      goModuleEntities.goLibrariesWorkspaceEntity.forEach {goLibrary ->
//        diff.workspaceModelDiff.mutableEntityStorage.addEntity(goLibrary)
      }
    }


//    update workspace model
//    diff.workspaceModelDiff.mutableEntityStorage.addEntity()
//  }

  private fun BaseTargetInfos.calculateGoTargetIds(): List<BuildTargetIdentifier> =
    infos.filter { it.target.languageIds.contains("go") }.map { it.target.id }

  private data class GoTargetEntities(
    val goModuleWorkspaceEntity: WorkspaceEntity,
    val goDependenciesWorkspaceEntity: List<WorkspaceEntity>,
    val goLibrariesWorkspaceEntity: List<WorkspaceEntity>,
  )

  private fun prepareAllGoEntities(goTargetId: BuildTargetIdentifier): GoTargetEntities? {
//    val vgoModule = VgoStandaloneModuleEntity(
//      moduleId = goModule.symbolicId,
//      entitySource = BspEntitySource,
//      importPath = entityToAdd.importPath,
////      root = if (entityToAdd.importPath == "github.com/rickypai/golang-boilerplate/protobufs/helloworld")
////        entityToAdd.root.resolve("io.grpc.examples.helloworld").toVirtualFileUrl(virtualFileUrlManager)
////      else entityToAdd.root.toVirtualFileUrl(virtualFileUrlManager)
//      root = entityToAdd.root.toVirtualFileUrl(virtualFileUrlManager),
//    )
//
//    val vgoModuleDependencies = entityToAdd.goDependencies.map {
//      VgoDependencyEntity(
//        importPath = it.importPath,
//        entitySource = BspEntitySource,
//        isMainModule = false,
//        internal = true,
//      ) {
//        this.module = vgoModule
////        this.root = if (it.importPath == "github.com/rickypai/golang-boilerplate/protobufs/helloworld")
////          it.root.resolve("io.grpc.examples.helloworld").toVirtualFileUrl(virtualFileUrlManager)
//        this.root = it.root.toVirtualFileUrl(virtualFileUrlManager)
//      }
//    }
//
//    val vgoModuleLibraries = entityToAdd.goLibraries.map {
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
//
//    return GoTargetEntities(
//      goModuleWorkspaceEntity = vgoModule,
//      goDependenciesWorkspaceEntity = vgoModuleDependencies,
//      goLibrariesWorkspaceEntity = vgoModuleLibraries,
//    )
    return null
  }

}
