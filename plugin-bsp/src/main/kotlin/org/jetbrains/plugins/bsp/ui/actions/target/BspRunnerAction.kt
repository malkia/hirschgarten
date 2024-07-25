package org.jetbrains.plugins.bsp.ui.actions.target

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.config.BspRunConfigurationType
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.getBuildTargetName
import javax.swing.Icon

abstract class BspRunnerAction(
  targetInfo: BuildTargetInfo,
  text: () -> String,
  icon: Icon? = null,
  private val isDebugAction: Boolean = false,
) : BaseRunnerAction(targetInfo, text, icon, isDebugAction) {
  abstract fun getConfigurationType(project: Project): ConfigurationType

  override suspend fun getRunnerSettings(
    project: Project,
    buildTargetInfo: BuildTargetInfo,
  ): RunnerAndConfigurationSettings? {
    val factory = getConfigurationType(project).configurationFactories.first()
    val name = calculateConfigurationName(buildTargetInfo)
    val runConfig = BspRunConfiguration(project, name, listOf(buildTargetInfo.id.uri))
    val settings =
      RunManager.getInstance(project).createConfiguration(runConfig, factory)
    return settings
  }

  private fun calculateConfigurationName(targetInfo: BuildTargetInfo): String {
    val targetDisplayName = targetInfo.getBuildTargetName()
    val actionNameKey = when {
      isDebugAction -> "target.debug.config.name"
      this is TestTargetAction -> "target.test.config.name"
      else -> "target.run.config.name"
    }
    return BspPluginBundle.message(actionNameKey, targetDisplayName)
  }
}
