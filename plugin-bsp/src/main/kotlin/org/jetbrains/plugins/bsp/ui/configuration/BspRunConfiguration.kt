package org.jetbrains.plugins.bsp.ui.configuration

import com.intellij.execution.Executor
import com.intellij.openapi.diagnostic.logger
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationEditor
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunHandler
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunHandlerProvider

public class BspRunConfiguration(
  private val project: Project,
  configurationFactory: BspRunConfigurationType,
  name: String,
) : LocatableConfigurationBase<RunProfileState>(project, configurationFactory, name),
  RunConfigurationWithSuppressedDefaultDebugAction,
  SMRunnerConsolePropertiesProvider,
  DumbAware {

  private val logger: Logger = logger<BspRunConfiguration>()

  /** The BSP-specific parts of the last serialized state of this run configuration. */
  private var bspElementState = Element(BSP_STATE_TAG)

  public var targets: List<String> = emptyList()
    set(value) {
      field = value
      updateHandlerIfDifferentProvider(BspRunHandlerProvider.getRunHandlerProvider(project, value))
    }

  private var handlerProvider: BspRunHandlerProvider = BspRunHandlerProvider.getRunHandlerProvider(project, targets)

  public var handler: BspRunHandler = handlerProvider.createRunHandler(this)

  private fun updateHandlerIfDifferentProvider(newProvider: BspRunHandlerProvider) {
    if (newProvider == handlerProvider) return // TODO
    try {
      handler.settings.writeExternal(bspElementState)
    } catch (e: WriteExternalException) {
      logger.error("Failed to write BSP state", e)
    }
    handlerProvider = newProvider
    handler = handlerProvider.createRunHandler(this)

    try {
      handler.settings.readExternal(bspElementState)
    } catch (e: Exception) {
      logger.error("Failed to read BSP state", e)
    }
  }

  override fun getConfigurationEditor(): SettingsEditor<BspRunConfiguration> {
    return BspRunConfigurationEditor(this)
  }

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    return handler.getRunProfileState(executor, environment)
  }

  override fun readExternal(element: Element) {
    super.readExternal(element)

    val bspElement = element.getChild(BSP_STATE_TAG) ?: return

    val targets = mutableListOf<String>()
    for (targetElement in bspElement.getChildren(TARGET_TAG)) {
      targets.add(targetElement.text)
    }

    this.targets = targets

    // It should be possible to load the configuration before the project is synchronized,
    // so we can't access targets' data here. Instead, we have to use the stored provider ID.
    // TODO: is that true?
    val providerId = bspElement.getAttributeValue(HANDLER_PROVIDER_ATTR)
    if (providerId == null) {
      logger.warn("No handler provider ID found in run configuration")
      return
    }
    val provider = BspRunHandlerProvider.findRunHandlerProvider(providerId)
    if (provider != null) {
      updateHandlerIfDifferentProvider(provider)
      // TODO the above already reads
      handler.settings.readExternal(bspElement)
    } else {
      logger.warn("Failed to find run handler provider with ID $providerId")
      val newProvider = BspRunHandlerProvider.getRunHandlerProvider(project, targets)
      updateHandlerIfDifferentProvider(newProvider)
    }

    bspElementState = bspElement
  }

  override fun writeExternal(element: Element) {
    super.writeExternal(element)

    bspElementState.removeChildren(TARGET_TAG)

    for (target in targets) {
      val targetElement = Element(TARGET_TAG)
      targetElement.text = target
      bspElementState.addContent(targetElement)
    }

    bspElementState.setAttribute(HANDLER_PROVIDER_ATTR, handlerProvider.id)

    handler.settings.writeExternal(bspElementState)

    element.addContent(bspElementState.clone())
  }

  override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties {
    return SMTRunnerConsoleProperties(this, "BSP", executor)
  }

  public companion object {
    private const val TARGET_TAG = "bsp-target"
    private const val BSP_STATE_TAG = "bsp-state"
    private const val HANDLER_PROVIDER_ATTR = "handler-provider-id"
  }
}

