package org.jetbrains.plugins.bsp.flow.sync

import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.bspBuildToolId
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.plugins.bsp.server.tasks.CollectProjectDetailsTask
import java.util.concurrent.CompletableFuture

class BigProjectSyncHook: ProjectSyncHook {
  override val buildToolId: BuildToolId = bspBuildToolId

  override suspend fun onSync(
    project: Project,
    server: JoinedBuildServer,
    capabilities: BazelBuildServerCapabilities,
    diff: AllProjectStructuresDiff,
    taskId: String,
    progressReporter: SequentialProgressReporter,
    baseTargetInfos: BaseTargetInfos,
    cancelOn: CompletableFuture<Void>,
    errorCallback: (Throwable) -> Unit,
  ) {
    println("AAA")
    val task = CollectProjectDetailsTask(project, taskId, diff.workspaceModelDiff.mutableEntityStorage)
    task.execute(server, capabilities, progressReporter, baseTargetInfos, cancelOn, errorCallback)
    diff.workspaceModelDiff.addPostApplyAction { task.postprocessingSubtask(progressReporter); println("BBB") }
  }
}
