package org.jetbrains.plugins.bsp.flow.sync

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.WithBuildToolId
import org.jetbrains.plugins.bsp.extension.points.allWithBuildToolId
import org.jetbrains.plugins.bsp.extension.points.bspBuildToolId
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import java.util.concurrent.CompletableFuture

public interface ProjectSyncHook : WithBuildToolId {
  fun isEnabled(project: Project): Boolean = true

  public suspend fun onSync(
    project: Project,
    server: JoinedBuildServer,
    capabilities: BazelBuildServerCapabilities,
    diff: AllProjectStructuresDiff,
    taskId: String,
    progressReporter: SequentialProgressReporter,
    baseTargetInfos: BaseTargetInfos,
    cancelOn: CompletableFuture<Void>,
    errorCallback: (Throwable) -> Unit,
  )

  public companion object {
    internal val ep =
      ExtensionPointName.create<ProjectSyncHook>("org.jetbrains.bsp.projectSyncHook")
  }
}

internal val defaultProjectSyncHooks: List<ProjectSyncHook>
  get() = ProjectSyncHook.ep.allWithBuildToolId(bspBuildToolId)

internal val Project.additionalProjectSyncHooks: List<ProjectSyncHook>
  get() = ProjectSyncHook.ep.allWithBuildToolId(buildToolId)
