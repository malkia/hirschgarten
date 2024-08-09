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
import org.jetbrains.plugins.bsp.extension.points.withBuildToolId
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import java.util.concurrent.CompletableFuture

interface ProjectSyncHook : WithBuildToolId {
  fun isEnabled(project: Project): Boolean = true

  suspend fun onSync(
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

  companion object {
    internal val ep =
      ExtensionPointName.create<ProjectSyncHook>("org.jetbrains.bsp.projectSyncHook")
  }
}

interface DefaultProjectSyncHooksDisabler : WithBuildToolId {
  fun disabledProjectSyncHooks(project: Project): List<Class<ProjectSyncHook>>

  companion object {
    internal val ep =
      ExtensionPointName.create<DefaultProjectSyncHooksDisabler>("org.jetbrains.bsp.defaultProjectSyncHooksDisabler")
  }
}

internal val Project.disabledDefaultProjectSyncHooks: List<Class<ProjectSyncHook>>
  get() = DefaultProjectSyncHooksDisabler.ep
    .withBuildToolId(buildToolId)
    ?.disabledProjectSyncHooks(this)
    .orEmpty()

internal val Project.defaultProjectSyncHooks: List<ProjectSyncHook>
  get() {
    val disabled = disabledDefaultProjectSyncHooks

    return ProjectSyncHook.ep
      .allWithBuildToolId(bspBuildToolId)
      .filterNot { it::class.java in disabled }
  }

internal val Project.additionalProjectSyncHooks: List<ProjectSyncHook>
  get() = if (buildToolId != bspBuildToolId) ProjectSyncHook.ep.allWithBuildToolId(buildToolId) else emptyList()
