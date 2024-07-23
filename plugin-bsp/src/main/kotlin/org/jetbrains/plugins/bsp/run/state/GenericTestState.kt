package org.jetbrains.plugins.bsp.run.state

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.externalSystem.service.execution.configuration.addEnvironmentFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.externalSystem.service.ui.util.SettingsFragmentInfo
import com.intellij.openapi.options.SettingsEditor
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.XCollection
import org.jetbrains.plugins.bsp.run.BspRunConfigurationState
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import javax.swing.JTextField

public class GenericTestState : BspRunConfigurationState<GenericTestState>() {

  @com.intellij.configurationStore.Property(description = "Test filter")
  @get:Attribute("testFilter")
  public var testFilter: String? by string()

  @com.intellij.configurationStore.Property(description = "Arguments")
  @get:XCollection
  public var arguments: MutableList<String> by list()

  @com.intellij.configurationStore.Property(description = "Working directory")
  @get:Attribute("workingDirectory")
  public var workingDirectory: String? by string()

  @com.intellij.configurationStore.Property(description = "Environment variables")
  @get:Attribute("env")
  public var env: EnvironmentVariablesData by property(EnvironmentVariablesData.DEFAULT) { it == EnvironmentVariablesData.DEFAULT }

  override fun getEditor(configuration: BspRunConfiguration): SettingsEditor<GenericTestState> {
    return GenericTestStateEditor(configuration)
  }
}

public class GenericTestStateEditor(private val config: BspRunConfiguration) : FragmentedSettingsEditor<GenericTestState>(config.handler.settings as GenericTestState) {

  override fun createFragments(): Collection<SettingsEditorFragment<GenericTestState, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createHeader("Test Configuration"))

      addSettingsEditorFragment(object : SettingsFragmentInfo {
        override val settingsActionHint: String = "Test Filter Action Hint"
        override val settingsGroup: String = "Test Filter Group"
        override val settingsHint: String = "Test Filter Hint"
        override val settingsId: String = "Test Filter ID"
        override val settingsName: String = "Test Filter Name"
        override val settingsPriority: Int = 0
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
      }, { JTextField() },
        { state, component -> component.text = state.testFilter ?: "" },
        { state, component -> state.testFilter = component.text} )

      addEnvironmentFragment(object : LabeledSettingsFragmentInfo {
        override val editorLabel: String = "Environment Variables"
        override val settingsActionHint: String = "Environment Variables Action Hint"
        override val settingsGroup: String = "Environment Variables Group"
        override val settingsHint: String = "Environment Variables Hint"
        override val settingsId: String = "Environment Variables ID"
        override val settingsName: String = "Environment Variables Name"
        override val settingsPriority: Int = 0
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
      },
        { env.envs },
        { env = env.with(it) },
        { env.isPassParentEnvs },
        { env = env.with(it) },
        false
      )
    }
}
