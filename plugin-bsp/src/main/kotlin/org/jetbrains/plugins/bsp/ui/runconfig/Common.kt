package org.jetbrains.plugins.bsp.ui.runconfig

import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.configurations.RuntimeConfigurationWarning
import com.intellij.execution.ui.CommandLinePanel
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.ide.macro.MacrosDialog
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.addEnvironmentFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.TextComponentEmptyText
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.ThrowableRunnable
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Paths

interface HasEnv {
  var env: EnvironmentVariablesData
}

fun <C : HasEnv> SettingsEditorFragmentContainer<C>.addEnvironmentFragment() =
  addEnvironmentFragment(
    object : LabeledSettingsFragmentInfo {
      override val editorLabel: String = ExecutionBundle.message("environment.variables.component.title")
      override val settingsId: String = "external.system.environment.variables.fragment"
      override val settingsName: String = ExecutionBundle.message("environment.variables.fragment.name")
      override val settingsGroup: String = ExecutionBundle.message("group.operating.system")
      override val settingsHint: String = ExecutionBundle.message("environment.variables.fragment.hint")
      override val settingsActionHint: String = ExecutionBundle.message("set.custom.environment.variables.for.the.process")
    },
    { this.env.envs },
    { this.env = env.with(it) },
    { this.env.isPassParentEnvs },
    { this.env = env.with(it) },
    hideWhenEmpty = false
  )

interface HasWorkingDirectory {
  var workingDirectory: String?
}

fun <T: HasWorkingDirectory> workingDirectoryFragment(configuration: RunConfiguration): SettingsEditorFragment<T, LabeledComponent<TextFieldWithBrowseButton>> {

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
  val workingDirectorySettings: SettingsEditorFragment<T, LabeledComponent<TextFieldWithBrowseButton>> =
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


interface HasProgramArguments {
  var programArguments: MutableList<String>
}

fun <T: HasProgramArguments> programArgumentsFragment(): SettingsEditorFragment<T, RawCommandLineEditor> {
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
  val parameters: SettingsEditorFragment<T, RawCommandLineEditor> =
    SettingsEditorFragment(
      "commandLineParameters",
      ExecutionBundle.message("run.configuration.program.parameters.name"),
      null as String?,
      programArguments,
      100,
      { settings, component ->
        component.text = settings.programArguments.joinToString(" ")
      },
      { settings, component ->
        settings.programArguments = component.text.split("\\s+".toRegex()).toMutableList()
      },
      { true }
    )
  parameters.isRemovable = false
  parameters.setEditorGetter { editor: RawCommandLineEditor -> editor.editorField }
  parameters.setHint(ExecutionBundle.message("run.configuration.program.parameters.hint"))

  return parameters
}
