package org.jetbrains.plugins.bsp.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.services.BspTaskEventsService
import org.jetbrains.plugins.bsp.services.OriginId
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.GenericRunState
import org.jetbrains.plugins.bsp.run.task.BspRunTaskListener
import java.util.concurrent.CompletableFuture

public abstract class BspCommandLineStateBase(
  environment: ExecutionEnvironment,
  protected val originId: OriginId,
) : CommandLineState(environment) {

  protected abstract fun createAndAddTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener

  /** Run the actual BSP command or throw an exception if the server does not support running the configuration */
  protected abstract fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities): CompletableFuture<*>

  final override fun startProcess(): BspProcessHandler<out Any> {
    val configuration = environment.runProfile as BspRunConfiguration

    // We have to start runFuture later, because we need to register the listener first
    // Otherwise, we might miss some events
    val computationStarter = CompletableFuture<Unit>()
    val runFuture = computationStarter.thenCompose {
      // The "useless" type below is actually needed because of a bug in Kotlin compiler
      val completableFuture: CompletableFuture<*> =
        configuration.project.connection.runWithServer { server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities ->
          startBsp(server, capabilities)
        }
      completableFuture
    }

    val handler = BspProcessHandler(runFuture)
    val runListener = createAndAddTaskListener(handler)

    with(BspTaskEventsService.getInstance(configuration.project)) {
      saveListener(originId, runListener)
      runFuture.handle { _, _ ->
        removeListener(originId)
      }
    }

    computationStarter.complete(Unit)
    handler.startNotify()

    return handler
  }
}

internal class BspRunCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  val runState: GenericRunState,
) : BspCommandLineStateBase(environment, originId) {
  private val configuration = environment.runProfile as BspRunConfiguration

  override fun createAndAddTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener =
    BspRunTaskListener(handler)

  override fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities): CompletableFuture<*> {
    if (configuration.targets.singleOrNull() == null || capabilities.runProvider == null) {
      throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }

    val targetId = BuildTargetIdentifier(configuration.targets.single())
    val runParams = RunParams(targetId)
    runParams.originId = originId
    runParams.arguments = runState.programArguments
    runParams.environmentVariables = runState.env.envs
    runParams.workingDirectory = runState.workingDirectory
    return server.buildTargetRun(runParams)
  }
}
