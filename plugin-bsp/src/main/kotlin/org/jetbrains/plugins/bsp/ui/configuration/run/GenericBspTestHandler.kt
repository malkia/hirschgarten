package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.service
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.target.TemporaryTargetUtils
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import java.util.UUID


public class GenericBspTestHandler : BspRunHandler {
  override val settings: GenericTestState = GenericTestState()

  override val name: String = "Generic BSP Test Handler"

  override fun getRunProfileState(
    executor: Executor,
    environment: ExecutionEnvironment,
  ): RunProfileState {
    val originId = UUID.randomUUID().toString()
    val configuration = environment.runProfile
    if (configuration !is BspRunConfiguration) {
      throw IllegalArgumentException("GenericBspTestHandler can only handle BspRunConfiguration")
    }
    return BspTestCommandLineState(environment, originId)
  }
}