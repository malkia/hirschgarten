package org.jetbrains.plugins.bsp.ui.runconfig

import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.configurations.RuntimeConfigurationWarning
import com.intellij.execution.ui.CommandLinePanel
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.execution.ui.SettingsEditorFragmentType
import com.intellij.ide.macro.MacrosDialog
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.addEnvironmentFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.externalSystem.service.ui.util.SettingsFragmentInfo
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
import javax.swing.JTextField

interface HasTestFilter {
  var testFilter: String?
}

fun <C : HasTestFilter> SettingsEditorFragmentContainer<C>.addTestFilterFragment() =
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
    { state, component -> state.testFilter = component.text }
  )
