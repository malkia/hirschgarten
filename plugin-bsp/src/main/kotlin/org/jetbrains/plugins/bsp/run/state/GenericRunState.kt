package org.jetbrains.plugins.bsp.run.state

import com.intellij.execution.ExecutionBundle
import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.configurations.RuntimeConfigurationWarning
import com.intellij.execution.ui.CommandLinePanel
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.FragmentedSettingsEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.ide.macro.MacrosDialog
import com.intellij.openapi.externalSystem.service.execution.configuration.addEnvironmentFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.TextComponentEmptyText
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ThrowableRunnable
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.XCollection
import org.jetbrains.plugins.bsp.run.BspRunConfigurationState
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Paths


public class GenericRunState : BspRunConfigurationState<GenericRunState>() {

  @com.intellij.configurationStore.Property(description = "Arguments")
  @get:XCollection
  public var arguments: MutableList<String> by list()

  @com.intellij.configurationStore.Property(description = "Working directory")
  @get:Attribute("workingDirectory")
  public var workingDirectory: String? by string()

  // TODO: handle passing system environment variables
  @com.intellij.configurationStore.Property(description = "Environment variables")
  @get:Attribute("env")
  public var env: EnvironmentVariablesData by property(EnvironmentVariablesData.DEFAULT) { it == EnvironmentVariablesData.DEFAULT }

  override fun getEditor(configuration: BspRunConfiguration): SettingsEditor<GenericRunState> {
    return GenericRunStateEditor(configuration)
  }
}

fun workingDirectoryFragment(configuration: BspRunConfiguration
): SettingsEditorFragment<GenericRunState, LabeledComponent<TextFieldWithBrowseButton>> {

  val textField = ExtendableTextField(10)
  MacrosDialog.addMacroSupport(
    textField, MacrosDialog.Filters.DIRECTORY_PATH
  ) { false }
  val workingDirectoryField = TextFieldWithBrowseButton(textField)
  workingDirectoryField.addBrowseFolderListener(
    ExecutionBundle.message(
      "select.working.directory.message",
    ),
    null,
    configuration.project,
    FileChooserDescriptorFactory.createSingleFolderDescriptor(),
    TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
  )
  val field = LabeledComponent.create(
    workingDirectoryField,
    ExecutionBundle.message("run.configuration.working.directory.label"),
    "West"
  )
  val workingDirectorySettings: SettingsEditorFragment<GenericRunState, LabeledComponent<TextFieldWithBrowseButton>> =
    SettingsEditorFragment(
      "workingDirectory",
      ExecutionBundle.message("run.configuration.working.directory.name"),
      null as String?,
      field,
      { settings, component ->
        (component.component as TextFieldWithBrowseButton).setText(settings.workingDirectory)
      },
      { settings, component ->
        settings.workingDirectory = component.component.text
      },
      {  true }
    )
  workingDirectorySettings.isRemovable = false
  workingDirectorySettings.setValidation { settings ->
    val workingDir = settings.workingDirectory
    val runnable =
      ThrowableRunnable<RuntimeConfigurationWarning> {
        val exists = try {
          Files.exists(Paths.get(workingDir))
        } catch (e: InvalidPathException) {
          false
        }
        if (!exists) {
          throw RuntimeConfigurationWarning(
            ExecutionBundle.message(
              "dialog.message.working.directory.doesn.t.exist",
              workingDir
            )
          )
        }
      }
    val validationInfo =
      RuntimeConfigurationException.validate(
        textField,
        runnable
      )
    listOf(validationInfo)
  }
  return workingDirectorySettings
}

fun programArgumentsFragment(): SettingsEditorFragment<GenericRunState, RawCommandLineEditor> {
  val programArguments = RawCommandLineEditor()
  CommandLinePanel.setMinimumWidth(programArguments, 400)
  val message = ExecutionBundle.message("run.configuration.program.parameters.placeholder")
  programArguments.editorField.emptyText.setText(message)
  programArguments.editorField.accessibleContext.accessibleName = message
  TextComponentEmptyText.setupPlaceholderVisibility(programArguments.editorField)
  CommonParameterFragments.setMonospaced(programArguments.textField)
  MacrosDialog.addMacroSupport(
    programArguments.editorField, MacrosDialog.Filters.ALL
  ) { false }
  val parameters: SettingsEditorFragment<GenericRunState, RawCommandLineEditor> =
    SettingsEditorFragment(
      "commandLineParameters",
      ExecutionBundle.message("run.configuration.program.parameters.name"),
      null as String?,
      programArguments,
      100,
      { settings, component ->
        component.text = settings.arguments.joinToString(" ")
      },
      { settings, component ->
        settings.arguments = component.text.split("\\s+".toRegex()).toMutableList()
      },
      { true }
    )
  parameters.isRemovable = false
  parameters.setEditorGetter { editor: RawCommandLineEditor -> editor.editorField }
  parameters.setHint(ExecutionBundle.message("run.configuration.program.parameters.hint"))

  return parameters
}

fun <T : JavaRunConfigurationBase> vmOptions(): SettingsEditorFragment<T, RawCommandLineEditor> {
  val group = ExecutionBundle.message("group.java.options")
  val vmOptions = RawCommandLineEditor()
  CommandLinePanel.setMinimumWidth(vmOptions, 400)
  CommonParameterFragments.setMonospaced(vmOptions.textField)
  val message = ExecutionBundle.message("run.configuration.java.vm.parameters.empty.text")
  vmOptions.editorField.accessibleContext.accessibleName = message
  vmOptions.editorField.emptyText.setText(message)
  MacrosDialog.addMacroSupport(vmOptions.editorField, MacrosDialog.Filters.ALL) { false }
  TextComponentEmptyText.setupPlaceholderVisibility(vmOptions.editorField)
  val vmParameters: SettingsEditorFragment<T, RawCommandLineEditor> = SettingsEditorFragment("vmParameters",
    ExecutionBundle.message("run.configuration.java.vm.parameters.name"),
    group,
    vmOptions,
    15,
    { configuration, c ->
      c.text = configuration.vmParameters
    },
    { configuration, c ->
      configuration.vmParameters = if (c.isVisible) c.text else null
    },
    { configuration -> StringUtil.isNotEmpty(configuration.vmParameters) })
  vmParameters.setHint(ExecutionBundle.message("run.configuration.java.vm.parameters.hint"))
  vmParameters.actionHint =
    ExecutionBundle.message("specify.vm.options.for.running.the.application")
  vmParameters.setEditorGetter { editor: RawCommandLineEditor -> editor.editorField }

  return vmParameters
}

public class GenericRunStateEditor(private val config: BspRunConfiguration) : FragmentedSettingsEditor<GenericRunState>(config.handler.settings as GenericRunState) {
  override fun createFragments(): Collection<SettingsEditorFragment<GenericRunState, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createHeader("Test Configuration"))

      add(programArgumentsFragment())
      add(workingDirectoryFragment(config))

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
