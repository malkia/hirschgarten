package org.jetbrains.plugins.bsp.run.state

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.options.SettingsEditor
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.XCollection
import org.jetbrains.plugins.bsp.run.BspRunConfigurationState
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.runconfig.HasEnv
import org.jetbrains.plugins.bsp.ui.runconfig.HasProgramArguments
import org.jetbrains.plugins.bsp.ui.runconfig.HasWorkingDirectory
import org.jetbrains.plugins.bsp.ui.runconfig.addEnvironmentFragment
import org.jetbrains.plugins.bsp.ui.runconfig.programArgumentsFragment
import org.jetbrains.plugins.bsp.ui.runconfig.workingDirectoryFragment


public class GenericRunState : BspRunConfigurationState<GenericRunState>(), HasEnv, HasProgramArguments,
  HasWorkingDirectory {

  @com.intellij.configurationStore.Property(description = "Arguments")
  @get:XCollection
  public override var programArguments: MutableList<String> by list()

  @com.intellij.configurationStore.Property(description = "Working directory")
  @get:Attribute("workingDirectory")
  public override var workingDirectory: String? by string()

  // TODO: handle passing system environment variables
  @com.intellij.configurationStore.Property(description = "Environment variables")
  @get:Attribute("env")
  public override var env: EnvironmentVariablesData by property(EnvironmentVariablesData.DEFAULT) { it == EnvironmentVariablesData.DEFAULT }

  override fun getEditor(configuration: BspRunConfiguration): SettingsEditor<GenericRunState> {
    return GenericRunStateEditor(configuration)
  }
}

public class GenericRunStateEditor(private val config: BspRunConfiguration) :
  FragmentedSettingsEditor<GenericRunState>(config.handler.settings as GenericRunState) {
  override fun createFragments(): Collection<SettingsEditorFragment<GenericRunState, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createHeader("Test Configuration"))

      add(programArgumentsFragment())
      add(workingDirectoryFragment(config))
      addEnvironmentFragment()
    }

}
