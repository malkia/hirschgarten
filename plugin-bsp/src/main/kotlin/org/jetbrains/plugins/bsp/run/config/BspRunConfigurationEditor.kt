package org.jetbrains.plugins.bsp.run.config

import com.intellij.compiler.options.CompileStepBeforeRun
import com.intellij.execution.ui.BeforeRunFragment
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.CommonTags
import com.intellij.execution.ui.RunConfigurationFragmentedEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.addBeforeRunFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addLabeledSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.components.JBTextField
import org.jetbrains.plugins.bsp.run.BspRunConfigurationState

/**
 * The base editor for a BSP run configuration.
 * Takes care of targets, the common settings and sets up the handler-specific settings editor.
 */
class BspRunConfigurationEditor(val runConfiguration: BspRunConfiguration) :
  RunConfigurationFragmentedEditor<BspRunConfiguration>(
    runConfiguration, BspRunConfigurationExtensionManager.getInstance()
  ) {

  override fun createRunFragments(): List<SettingsEditorFragment<BspRunConfiguration, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createRunHeader())
      addBeforeRunFragment(CompileStepBeforeRun.ID)
      addAll(BeforeRunFragment.createGroup())
      add(CommonTags.parallelRun())
      addBspTargetFragment()
      addStateEditorFragment()
    }

  private fun SettingsEditorFragmentContainer<BspRunConfiguration>.addStateEditorFragment() {
    val handler = runConfiguration.handler ?: return
    // TODO: this way of getting the editor causes a disposer exception
    val stateEditor: SettingsEditor<BspRunConfigurationState<*>> = handler.settings.getEditor(runConfiguration) as SettingsEditor<BspRunConfigurationState<*>>
    this.addLabeledSettingsEditorFragment(object : LabeledSettingsFragmentInfo { // TODO: Use bundle
      override val editorLabel: String = "Handler settings"
      override val settingsId: String = "bsp.state.editor"
      override val settingsName: String = "Handler settings"
      override val settingsGroup: String = "BSP"
      override val settingsHint: String = "Handler settings hint"
      override val settingsActionHint: String = "Handler settings action hint"
    }, { stateEditor.component }, { _, _ ->
      stateEditor.resetFrom(handler.settings)
    }, { _, _ -> stateEditor.applyTo(handler.settings)
    })
  }

  private fun SettingsEditorFragmentContainer<BspRunConfiguration>.addBspTargetFragment() {
    this.addLabeledSettingsEditorFragment(object : LabeledSettingsFragmentInfo { // TODO: Use bundle
      override val editorLabel: String = "Build target"
      override val settingsId: String = "bsp.target.fragment"
      override val settingsName: String = "Build target"
      override val settingsGroup: String = "BSP"
      override val settingsHint: String = "Build target"
      override val settingsActionHint: String = "Build target"
    }, { JBTextField().apply { isEditable = false } }, { s, c ->
      c.text = s.targets.joinToString(", ")
    }, { _, _ -> {}
    }, { true })
  }
}
