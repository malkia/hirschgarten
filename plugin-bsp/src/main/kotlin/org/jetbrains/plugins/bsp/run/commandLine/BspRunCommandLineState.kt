package org.jetbrains.plugins.bsp.run.commandLine

import ch.epfl.scala.bsp4j.RunParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.run.BspCommandLineStateBase
import org.jetbrains.plugins.bsp.run.BspProcessHandler
import org.jetbrains.plugins.bsp.run.BspTaskListener
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.GenericRunState
import org.jetbrains.plugins.bsp.run.task.BspRunTaskListener
import org.jetbrains.plugins.bsp.services.OriginId
import java.util.concurrent.CompletableFuture

internal class BspRunCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  private val runState: GenericRunState,
) : BspCommandLineStateBase(environment, originId) {
  private val configuration = environment.runProfile as BspRunConfiguration

  override fun createAndAddTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener = BspRunTaskListener(handler)

  override fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities): CompletableFuture<*> {
    if (configuration.targets.singleOrNull() == null || capabilities.runProvider == null) {
      throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }

    val targetId = configuration.targets.single()
    val runParams = RunParams(targetId)
    runParams.originId = originId
    runParams.arguments = transformProgramArguments(runState.programArguments)
    runParams.environmentVariables = runState.env.envs
    runParams.workingDirectory = runState.workingDirectory
    return server.buildTargetRun(runParams)
  }
}
