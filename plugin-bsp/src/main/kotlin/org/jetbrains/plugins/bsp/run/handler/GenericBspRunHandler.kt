package org.jetbrains.plugins.bsp.run.handler

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.commandLine.BspRunCommandLineState
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.GenericRunState
import java.util.UUID

public class GenericBspRunHandler : BspRunHandler {
  override val state: GenericRunState = GenericRunState()

  override val name: String = "Generic BSP Run Handler"

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val originId = UUID.randomUUID().toString()
    val configuration = environment.runProfile as BspRunConfiguration
    return BspRunCommandLineState(environment, originId, state)
  }
}
