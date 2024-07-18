package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.externalSystem.service.execution.configuration.addEnvironmentFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.externalSystem.service.ui.util.SettingsFragmentInfo
import com.intellij.openapi.options.SettingsEditor
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import javax.swing.JCheckBox
import javax.swing.JSpinner
import javax.swing.JTextField

public class TestState : BspRunConfigurationState() {

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

  override fun getEditor(configuration: BspRunConfiguration): SettingsEditor<BspRunConfiguration> {
    return TestStateEditor(configuration)
  }
}

public class TestStateEditor(private val config: BspRunConfiguration) : BspStateFragmentedSettingsEditor<TestState>(config) {

  override fun createFragments(): Collection<SettingsEditorFragment<BspRunConfiguration, *>> =
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
        { _, component -> component.text = handlerState.testFilter ?: "" },
        { _, component -> handlerState.testFilter = component.text} )

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
        { handlerState.env.envs },
        { handlerState.env = handlerState.env.with(it) },
        { handlerState.env.isPassParentEnvs },
        { handlerState.env = handlerState.env.with(it) },
        false
      )
    }
}


public class SomeState : BspRunConfigurationState() {

  @com.intellij.configurationStore.Property(description = "Show console when a message is printed to standard error stream")
  @get:Attribute("someName")
  public var someName: String? by string()

  @com.intellij.configurationStore.Property(description = "Show console when a message is printed to standard error stream")
  @get:Attribute("counter")
  public var counter: Int by property(0)

  @Tag("output_file")
  public class OutputFileOptions : BaseState() {
    @get:Attribute("path")
    public var fileOutputPath: String? by string()

    @get:Attribute("is_save")
    public var isSaveOutput: Boolean by property(false)
  }

  @get:Property
  public var outputFileOptions: OutputFileOptions by property(OutputFileOptions())

  override fun getEditor(configuration: BspRunConfiguration): SettingsEditor<BspRunConfiguration> {
    return SomeEditor(configuration)
  }
}

public abstract class BspStateFragmentedSettingsEditor<State: BspRunConfigurationState>(private val config: BspRunConfiguration) : FragmentedSettingsEditor<BspRunConfiguration>(config) {
  protected val handlerState: State = config.handler.settings as State
}

public class SomeEditor(private val config: BspRunConfiguration) : BspStateFragmentedSettingsEditor<SomeState>(config) {

  override fun createFragments(): Collection<SettingsEditorFragment<BspRunConfiguration, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createHeader("WITAJ W EDYTORZE KONFIGURACJI"))
      addSettingsEditorFragment(object : SettingsFragmentInfo {

        override val settingsActionHint: String = "Some Action Hint"
        override val settingsGroup: String = "Some Group"
        override val settingsHint: String = "Some Hint"
        override val settingsId: String = "Some ID"
        override val settingsName: String = "Some Name"
        override val settingsPriority: Int = 0
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
      },
        { JTextField() },
        { _, component -> component.text = handlerState.someName ?: "" },
        { _, component -> handlerState.someName = component.text })

      addSettingsEditorFragment(object : LabeledSettingsFragmentInfo {
        override val editorLabel: String = "Counter"
        override val settingsActionHint: String = "Counter Action Hint"
        override val settingsGroup: String = "Counter Group"
        override val settingsHint: String = "Counter Hint"
        override val settingsId: String = "Counter ID"
        override val settingsName: String = "Counter Name"
        override val settingsPriority: Int = 0
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
      },
        { JSpinner() },
        { _, component -> component.value = handlerState.counter },
        { _, component -> handlerState.counter = component.value as Int })

      // output file
      addSettingsEditorFragment(object : SettingsFragmentInfo {
        override val settingsActionHint: String = "Output File Action Hint"
        override val settingsGroup: String = "Output File Group"
        override val settingsHint: String = "Output File Hint"
        override val settingsId: String = "Output File ID"
        override val settingsName: String = "Output File Name"
        override val settingsPriority: Int = 0
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
      },
        { JTextField() },
        { settings, component -> component.text = handlerState.outputFileOptions.fileOutputPath ?: "" },
        { settings, component -> handlerState.outputFileOptions.fileOutputPath = component.text })

      // save output
      addSettingsEditorFragment(object : SettingsFragmentInfo {
        override val settingsActionHint: String = "Save Output Action Hint"
        override val settingsGroup: String = "Save Output Group"
        override val settingsHint: String = "Save Output Hint"
        override val settingsId: String = "Save Output ID"
        override val settingsName: String = "Save Output Name"
        override val settingsPriority: Int = 0
        override val settingsType: SettingsEditorFragmentType = SettingsEditorFragmentType.EDITOR
      },
        { JCheckBox() },
        { settings, component -> component.isSelected = handlerState.outputFileOptions.isSaveOutput },
        { settings, component -> handlerState.outputFileOptions.isSaveOutput = component.isSelected })
    }
}
