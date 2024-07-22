package org.jetbrains.plugins.bsp.ui.configuration.run

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
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration

/**
 * The base editor for a BSP run configuration.
 * Takes care of targets, the common settings and sets up the handler-specific settings editor.
 */
public class BspRunConfigurationEditor(public val runConfiguration: BspRunConfiguration) :
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

//  public fun programArguments(): SettingsEditorFragment<BspRunConfigurationBase, RawCommandLineEditor> {
//    val programArguments = RawCommandLineEditor()
//    CommandLinePanel.setMinimumWidth(programArguments, 400)
//    val message = ExecutionBundle.message("run.configuration.program.parameters.placeholder")
//    programArguments.editorField.emptyText.setText(message)
//    programArguments.editorField.accessibleContext.accessibleName = message
//    TextComponentEmptyText.setupPlaceholderVisibility(programArguments.editorField)
//    CommonParameterFragments.setMonospaced(programArguments.textField)
//
//    val parameters: SettingsEditorFragment<BspRunConfigurationBase, RawCommandLineEditor> =
//      SettingsEditorFragment<BspRunConfigurationBase, RawCommandLineEditor>(
//        "commandLineParameters",
//        ExecutionBundle.message("run.configuration.program.parameters.name"),
//        null,
//        programArguments,
//        100,
//        { settings: BspRunConfigurationBase, component: RawCommandLineEditor ->
//          component.text = settings.getProgramParameters()
//        },
//        { component: RawCommandLineEditor ->
//          settings.setProgramParameters(
//            component.text
//          )
//        },
//        { true }
//      )
//    parameters.isRemovable = false
//    parameters.setEditorGetter { editor: RawCommandLineEditor -> editor.editorField }
//    parameters.setHint(ExecutionBundle.message("run.configuration.program.parameters.hint"))
//
//    return parameters
//  }

//  private fun SettingsEditorFragmentContainer<BspRunConfigurationBase>.addBspEnvironmentFragment() {
//    this.addEnvironmentFragment(
//      object : LabeledSettingsFragmentInfo {
//        override val editorLabel: String = ExecutionBundle.message("environment.variables.component.title")
//        override val settingsId: String = "external.system.environment.variables.fragment" // TODO: does it matter?
//        override val settingsName: String = ExecutionBundle.message("environment.variables.fragment.name")
//        override val settingsGroup: String = ExecutionBundle.message("group.operating.system")
//        override val settingsHint: String = ExecutionBundle.message("environment.variables.fragment.hint")
//        override val settingsActionHint: String =
//          ExecutionBundle.message("set.custom.environment.variables.for.the.process")
//      },
//      { runConfiguration.env.envs },
//      { runConfiguration.env.with(it) },
//      { runConfiguration.env.isPassParentEnvs },
//      { runConfiguration.env.with(it) },
//      false
//    )
//  }

  private fun SettingsEditorFragmentContainer<BspRunConfiguration>.addStateEditorFragment() {
    val stateEditor: SettingsEditor<BspRunConfigurationState<*>> = runConfiguration.handler.settings.getEditor(runConfiguration) as SettingsEditor<BspRunConfigurationState<*>>
    this.addLabeledSettingsEditorFragment(object : LabeledSettingsFragmentInfo { // TODO: Use bundle
      override val editorLabel: String = "Handler settings"
      override val settingsId: String = "bsp.state.editor"
      override val settingsName: String = "Handler settings"
      override val settingsGroup: String = "BSP"
      override val settingsHint: String = "Handler settings hint"
      override val settingsActionHint: String = "Handler settings action hint"
    }, { stateEditor.component }, { s, c ->
      stateEditor.resetFrom(runConfiguration.handler.settings)
    }, { s, c -> stateEditor.applyTo(runConfiguration.handler.settings)
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
    }, { BspTargetComponent() }, { s, c ->
      c.text = s.targets.joinToString(", ")
    }, { _, _ -> {}
    }, { true })
  }
}

public class BspTargetComponent : JBTextField() {
  init {
    this.isEditable = false
  }
}
