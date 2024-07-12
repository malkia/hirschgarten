package org.jetbrains.bazel.extension

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.ui.widgets.BazelBspJumpToBuildFileAction
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildToolWindowTargetActionProviderExtension
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfoOld
import javax.swing.JComponent

class BazelTargetActionProviderExtension : BuildToolWindowTargetActionProviderExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override fun getTargetActions(
    component: JComponent,
    project: Project,
    buildTargetInfo: BuildTargetInfoOld,
  ): List<AnAction> =
    listOf(BazelBspJumpToBuildFileAction(component, project, buildTargetInfo))
}
