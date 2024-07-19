package org.jetbrains.plugins.bsp.flow.sync

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.WithBuildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolId
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff

/**
 * Will be called on each (re)sync after all default calls to the server.
 * This hook should be used to collect additional info from the server needed for other extension points.
 */
public interface ProjectSyncHook : WithBuildToolId {
  public fun onSync(
    project: Project,
    server: JoinedBuildServer,
    capabilities: BuildServerCapabilities,
    diff: AllProjectStructuresDiff,
  )

  public companion object {
    internal val ep =
      ExtensionPointName.create<ProjectSyncHook>("org.jetbrains.bsp.projectSyncHook")
  }
}

internal val Project.projectSyncHook: ProjectSyncHook?
  get() = ProjectSyncHook.ep.withBuildToolId(buildToolId)
