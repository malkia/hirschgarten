package org.jetbrains.plugins.bsp.intellij

import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.options.SettingsEditor
import org.jetbrains.plugins.bsp.run.BspRunConfigurationState
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.runconfig.HasIntellijSdkName
import org.jetbrains.plugins.bsp.ui.runconfig.HasJavaVmOptions
import org.jetbrains.plugins.bsp.ui.runconfig.HasProgramArguments
import org.jetbrains.plugins.bsp.ui.runconfig.programArgumentsFragment

class IntellijPluginRunHandlerState : BspRunConfigurationState<IntellijPluginRunHandlerState>(), HasJavaVmOptions,
  HasProgramArguments, HasIntellijSdkName {

  override var javaVmOptions: String? by string()

  override var programArguments: String? by string()

  override var intellijSdkName: String? by string()

  override fun getEditor(configuration: BspRunConfiguration): SettingsEditor<IntellijPluginRunHandlerState> =
    IntellijPluginRunHandlerStateEditor(configuration)

}

class IntellijPluginRunHandlerStateEditor(private val config: BspRunConfiguration) :
  FragmentedSettingsEditor<IntellijPluginRunHandlerState>(config.handler?.settings as IntellijPluginRunHandlerState) {
  override fun createFragments(): Collection<SettingsEditorFragment<IntellijPluginRunHandlerState, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(programArgumentsFragment())
    }

}
