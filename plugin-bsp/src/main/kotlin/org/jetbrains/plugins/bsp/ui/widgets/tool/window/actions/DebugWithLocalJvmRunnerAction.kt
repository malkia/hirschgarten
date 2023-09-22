package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.tasks.JvmRunEnvironmentTask

internal class DebugWithLocalJvmRunnerAction :
  LocalJvmRunnerAction(BspPluginBundle.message("widget.debug.target.with.runner.popup.message")) {
  override fun getEnvironment(project: Project): JvmEnvironmentItem? =
    target?.let { target ->
      JvmRunEnvironmentTask(project).connectAndExecute(target.toBsp4JTargetIdentifier())?.items?.first()
    }

  override fun getExecutor(): Executor = DefaultDebugExecutor.getDebugExecutorInstance()
}
