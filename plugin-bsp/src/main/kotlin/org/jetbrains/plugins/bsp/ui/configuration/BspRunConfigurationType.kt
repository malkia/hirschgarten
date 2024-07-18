package org.jetbrains.plugins.bsp.ui.configuration

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import javax.swing.Icon

public class BspRunConfigurationType : SimpleConfigurationType(
  id = ID,
  name = BspPluginBundle.message("runconfig.run.name"),
  description = BspPluginBundle.message("runconfig.run.description"),
  icon = NotNullLazyValue.createValue { BspPluginIcons.bsp },
) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration =
    BspRunConfiguration(project, this, name)

  public companion object {
    public const val ID: String = "BspRunConfiguration"
  }
}
