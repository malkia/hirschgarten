package org.jetbrains.plugins.bsp.run.config

import com.intellij.execution.Executor
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element
import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.BspRunHandlerProvider

class BspRunConfiguration(
  private val project: Project,
  name: String,
  targets: List<String>,
) : LocatableConfigurationBase<RunProfileState>(project, BspRunConfigurationType(), name),
  RunConfigurationWithSuppressedDefaultDebugAction,
  SMRunnerConsolePropertiesProvider,
  DumbAware {
  private val logger: Logger = logger<BspRunConfiguration>()

  /** The BSP-specific parts of the last serialized state of this run configuration. */
  private var bspElementState = Element(BSP_STATE_TAG)

  var targets: List<String> = targets
    private set // private because we need to set the targets directly when running readExternal

  fun updateTargets(newTargets: List<String>) {
    targets = newTargets
    updateHandlerIfDifferentProvider(BspRunHandlerProvider.getRunHandlerProvider(project, newTargets))
  }

  private var handlerProvider: BspRunHandlerProvider? = null

  var handler: BspRunHandler? = null

  private fun updateHandlerIfDifferentProvider(newProvider: BspRunHandlerProvider) {
    if (newProvider == handlerProvider) return // TODO
    try {
      handler?.settings?.writeExternal(bspElementState)
    } catch (e: WriteExternalException) {
      logger.error("Failed to write BSP state", e)
    }
    handlerProvider = newProvider
    handler = newProvider.createRunHandler(this)

    try {
      handler?.settings?.readExternal(bspElementState)
    } catch (e: Exception) {
      logger.error("Failed to read BSP state", e)
    }
  }

  override fun getConfigurationEditor(): SettingsEditor<BspRunConfiguration> = BspRunConfigurationEditor(this)

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? =
    handler?.getRunProfileState(executor, environment)

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

      try {
        handler?.settings?.readExternal(bspElement)
      } catch (e: Exception) {
        logger.error("Failed to read BSP state", e)
      }
    } else {
      logger.warn("Failed to find run handler provider with ID $providerId")
      val newProvider = BspRunHandlerProvider.getRunHandlerProvider(project, targets)
      updateHandlerIfDifferentProvider(newProvider)
    }

    bspElementState = bspElement
  }

  // TODO: ideally we'd use an existing serialization mechanism like https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html
  //  but it's hard to figure out how to implement it in our case, so for now let's use the franken-implementation
  //  taken from Google's plugin (which probably predates modern IJ state serialization)
  override fun writeExternal(element: Element) {
    super.writeExternal(element)

    val provider = handlerProvider
    val handler = handler

    if (provider == null || handler == null) {
      logger.warn("No handler provider or handler found in run configuration")
      return
    }

    bspElementState.removeChildren(TARGET_TAG)

    for (target in targets) {
      val targetElement = Element(TARGET_TAG)
      targetElement.text = target
      bspElementState.addContent(targetElement)
    }

    bspElementState.setAttribute(HANDLER_PROVIDER_ATTR, provider.id)

    handler.settings.writeExternal(bspElementState)

    element.setContent(bspElementState.clone())
  }

  override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties =
    SMTRunnerConsoleProperties(this, "BSP", executor)

  companion object {
    private const val TARGET_TAG = "bsp-target"
    private const val BSP_STATE_TAG = "bsp-state"
    private const val HANDLER_PROVIDER_ATTR = "handler-provider-id"
  }
}
