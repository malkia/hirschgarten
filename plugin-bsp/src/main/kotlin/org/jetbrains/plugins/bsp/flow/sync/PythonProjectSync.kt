package org.jetbrains.plugins.bsp.flow.sync

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsParams
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.bspBuildToolId
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import java.util.concurrent.CompletableFuture

class PythonProjectSync : ProjectSyncHook {
  override val buildToolId: BuildToolId = bspBuildToolId

  override fun isEnabled(project: Project): Boolean = BspFeatureFlags.isPythonSupportEnabled

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
    val pythonTargetIds = baseTargetInfos.calculatePythonTargetIds()
    val pythonOptions = server.queryPythonOptions(pythonTargetIds, cancelOn, errorCallback)

//    update workspace model
//    diff.workspaceModelDiff.mutableEntityStorage.addEntity()
  }

  private fun BaseTargetInfos.calculatePythonTargetIds(): List<BuildTargetIdentifier> =
    infos.filter { it.target.languageIds.contains("python") }.map { it.target.id }

  private fun JoinedBuildServer.queryPythonOptions(
    pythonTargetIds: List<BuildTargetIdentifier>,
    cancelOn: CompletableFuture<Void>,
    errorCallback: (Throwable) -> Unit
  ): List<PythonOptionsItem> =
    queryIf(
      pythonTargetIds.isNotEmpty(),
      "buildTarget/pythonOptions",
      cancelOn,
      errorCallback
    ) {
      buildTargetPythonOptions(PythonOptionsParams(pythonTargetIds))
    }?.get()?.items.orEmpty()
}
