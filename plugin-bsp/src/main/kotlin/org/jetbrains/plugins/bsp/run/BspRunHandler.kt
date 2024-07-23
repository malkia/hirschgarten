package org.jetbrains.plugins.bsp.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment

/**
 * Supports the run configuration flow for BSP run configurations.
 * The lifetime of a handler is shorter than the lifetime of a run configuration and fully owned by the run configuration.
 *
 * <p>Provides language-specific configuration state, validation, presentation, and runner.
 */
public interface BspRunHandler {
  public val settings: BspRunConfigurationState<*>

  /**
   * The name of the run handler (shown in the UI).
   */
  public val name: String

  public fun getRunProfileState(
    executor: Executor,
    environment: ExecutionEnvironment,
  ): RunProfileState

}
