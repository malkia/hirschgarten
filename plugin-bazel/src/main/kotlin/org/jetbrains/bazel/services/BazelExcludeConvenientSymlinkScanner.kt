package org.jetbrains.bazel.services

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntity
import com.intellij.util.indexing.roots.IndexableFileScanner
import com.intellij.util.indexing.roots.kind.ContentOrigin
import com.intellij.util.indexing.roots.kind.ProjectFileOrDirOrigin
import org.apache.commons.io.FileUtils.isSymlink
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspEntitySource

class BazelExcludeConvenientSymlinkScanner: IndexableFileScanner {
  override fun startSession(project: Project): IndexableFileScanner.ScanSession {
    return IndexableFileScanner.ScanSession {
      if (it is ContentOrigin || it is ProjectFileOrDirOrigin) {
        IndexableFileScanner.IndexableFileVisitor { fileOrDir -> excludeUrl(fileOrDir, project) }
      } else null
    }
  }

  private fun excludeUrl(fileOrDir: VirtualFile, project: Project) {
    if (fileOrDir.shouldExclude(project)) {
      val workspaceModel = WorkspaceModel.getInstance(project)
      runWriteAction {
        workspaceModel.updateProjectModel("Exclude file: ${fileOrDir.path}") { storage ->
          storage.addEntity(
            ExcludeUrlEntity(
              url = fileOrDir.toVirtualFileUrl(workspaceModel.getVirtualFileUrlManager()),
              entitySource = BspEntitySource
            )
          )
        }
      }
    }
  }

  private fun VirtualFile.shouldExclude(project: Project) = when {
    !project.isBspProject -> false
    isFile || !isSymlink(this.toNioPath().toFile()) -> false
    name.startsWith("bazel-") && project.rootDir == parent -> true
    else -> false
  }
}
