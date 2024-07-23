package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import java.util.UUID


public class GenericBspRunHandler : BspRunHandler {
  override val settings: GenericRunState = GenericRunState()

  override val name: String = "Generic BSP Run Handler"

  override fun getRunProfileState(
    executor: Executor,
    environment: ExecutionEnvironment,
  ): RunProfileState {
    val originId = UUID.randomUUID().toString()
    val configuration = environment.runProfile
    if (configuration !is BspRunConfiguration) {
      throw IllegalArgumentException("GenericBspRunHandler can only handle BspRunConfiguration")
    }
    return BspRunCommandLineState(environment, originId, settings)
  }
}

