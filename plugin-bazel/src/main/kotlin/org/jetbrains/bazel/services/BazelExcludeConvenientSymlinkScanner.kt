package org.jetbrains.bazel.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.util.indexing.roots.IndexableFileScanner
import com.intellij.util.indexing.roots.kind.ContentOrigin
import com.intellij.util.indexing.roots.kind.ProjectFileOrDirOrigin
import org.apache.commons.io.FileUtils.isSymlink
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.rootDir

class BazelExcludeConvenientSymlinkScanner: IndexableFileScanner {
  override fun startSession(project: Project): IndexableFileScanner.ScanSession {
    return IndexableFileScanner.ScanSession {
      if (it is ContentOrigin || it is ProjectFileOrDirOrigin) {
        IndexableFileScanner.IndexableFileVisitor { fileOrDir -> dummyVisitor(fileOrDir, project) }
      } else null
    }
  }

  private fun dummyVisitor(fileOrDir: VirtualFile, project: Project): Boolean? =
    fileOrDir.shouldExclude(project).takeIf { it }

  private fun VirtualFile.shouldExclude(project: Project) = when {
    !project.isBspProject -> false
    isFile || !isSymlink(this.toNioPath().toFile()) -> false
    name.startsWith("bazel-") && project.rootDir == parent -> true
    else -> false
  }
}
