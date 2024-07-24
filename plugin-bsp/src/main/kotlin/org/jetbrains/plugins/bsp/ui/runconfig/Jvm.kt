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

interface HasJavaVmOptions {
  var javaVmOptions: String?
}

fun <T : HasJavaVmOptions> vmOptions(): SettingsEditorFragment<T, RawCommandLineEditor> {
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
      c.text = configuration.javaVmOptions
    },
    { configuration, c ->
      configuration.javaVmOptions = if (c.isVisible) c.text else null
    },
    { configuration -> StringUtil.isNotEmpty(configuration.javaVmOptions) })
  vmParameters.setHint(ExecutionBundle.message("run.configuration.java.vm.parameters.hint"))
  vmParameters.actionHint =
    ExecutionBundle.message("specify.vm.options.for.running.the.application")
  vmParameters.setEditorGetter { editor: RawCommandLineEditor -> editor.editorField }

  return vmParameters
}

interface HasIntellijSdkName {
  var intellijSdkName: String?
}