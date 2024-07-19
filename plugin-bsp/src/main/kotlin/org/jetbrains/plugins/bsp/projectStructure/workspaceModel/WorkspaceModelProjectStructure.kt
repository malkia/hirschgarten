package org.jetbrains.plugins.bsp.projectStructure.workspaceModel

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.impl.WorkspaceModelInternal
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.workspace.jps.JpsFileDependentEntitySource
import com.intellij.platform.workspace.jps.JpsFileEntitySource
import com.intellij.platform.workspace.jps.JpsGlobalFileEntitySource
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.plugins.bsp.performance.testing.bspTracer
import org.jetbrains.plugins.bsp.projectStructure.ProjectStructureDiff
import org.jetbrains.plugins.bsp.projectStructure.ProjectStructureProvider
import org.jetbrains.workspacemodel.entities.BspDummyEntitySource
import org.jetbrains.workspacemodel.entities.BspEntitySource

class WorkspaceModelProjectStructureDiff(val diff: MutableEntityStorage) : ProjectStructureDiff {
  override suspend fun apply(project: Project) {
    val workspaceModel = WorkspaceModel.getInstance(project) as WorkspaceModelInternal
    val snapshot = workspaceModel.getBuilderSnapshot()
    bspTracer.spanBuilder("replacebysource.in.apply.on.workspace.model.ms").use {
      snapshot.builder.replaceBySource({ it.isBspRelevant() }, diff)
    }
    val storageReplacement = snapshot.getStorageReplacement()
    writeAction {
      val workspaceModelUpdated = bspTracer.spanBuilder("replaceprojectmodel.in.apply.on.workspace.model.ms").use {
        workspaceModel.replaceProjectModel(storageReplacement)
      }
      if (!workspaceModelUpdated) {
        error("Project model is not updated successfully. Try `reload` action to recalculate the project model.")
      }
    }
  }

  private fun EntitySource.isBspRelevant() =
    when (this) {
      // avoid touching global sources
      is JpsGlobalFileEntitySource -> false

      is JpsFileEntitySource,
      is JpsFileDependentEntitySource,
      is BspEntitySource,
      is BspDummyEntitySource,
        -> true

      else -> false
    }
}

class WorkspaceModelProjectStructureProvider : ProjectStructureProvider<WorkspaceModelProjectStructureDiff, Unit> {
  override fun newDiff(project: Project): WorkspaceModelProjectStructureDiff =
    WorkspaceModelProjectStructureDiff(MutableEntityStorage.create())

  override fun current(project: Project) {
    TODO("Not yet implemented")
  }
}
