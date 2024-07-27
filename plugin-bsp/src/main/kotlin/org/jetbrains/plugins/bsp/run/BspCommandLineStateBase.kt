package org.jetbrains.plugins.bsp.run

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.services.BspTaskEventsService
import org.jetbrains.plugins.bsp.services.OriginId
import java.util.concurrent.CompletableFuture

abstract class BspCommandLineStateBase(environment: ExecutionEnvironment, protected val originId: OriginId) :
  CommandLineState(environment) {
  protected abstract fun createAndAddTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener

  /** Run the actual BSP command or throw an exception if the server does not support running the configuration */
  protected abstract fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities): CompletableFuture<*>

  final override fun startProcess(): BspProcessHandler<out Any> {
    val configuration = environment.runProfile as BspRunConfiguration

    // We have to start runFuture later, because we need to register the listener first
    // Otherwise, we might miss some events
    val computationStarter = CompletableFuture<Unit>()
    val runFuture =
      computationStarter.thenCompose {
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
