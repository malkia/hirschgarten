package org.jetbrains.plugins.bsp.jvm

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RemoteDebugData
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesAndroid
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.isJvmTarget
import org.jetbrains.plugins.bsp.services.OriginId
import org.jetbrains.plugins.bsp.run.BspProcessHandler
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.BspCommandLineStateBase
import org.jetbrains.plugins.bsp.run.BspRunCommandLineState
import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.BspRunHandlerProvider
import org.jetbrains.plugins.bsp.run.BspTaskListener
import org.jetbrains.plugins.bsp.run.task.BspRunTaskListener
import org.jetbrains.plugins.bsp.run.state.GenericRunState
import java.util.UUID
import java.util.concurrent.CompletableFuture

public class JvmBspRunHandler(private val configuration: BspRunConfiguration) : BspRunHandler {
  override val name: String = "Jvm BSP Run Handler"

  override val settings = GenericRunState()

  override fun getRunProfileState(
    executor: Executor,
    environment: ExecutionEnvironment,
  ): RunProfileState {
    return when {
      executor is DefaultDebugExecutor -> {
        JvmDebugHandlerState(environment, UUID.randomUUID().toString())
      }

      else -> {
        BspRunCommandLineState(environment, UUID.randomUUID().toString(), settings)
      }
    }
  }

  public object JvmBspRunHandlerProvider : BspRunHandlerProvider {
    override val id: String = "JvmBspRunHandlerProvider"

    override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler =
      JvmBspRunHandler(configuration)

    override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
      targetInfos.all {
        it.languageIds.isJvmTarget() ||
            it.languageIds.includesAndroid() && it.capabilities.canTest
      }

    override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean {
      return targetInfos.all { it.capabilities.canDebug }
    }

  }
}

public class JvmDebugHandlerState(
  environment: ExecutionEnvironment,
  originId: OriginId,
) : BspCommandLineStateBase(environment, originId) {
  public val remoteConnection: RemoteConnection =
    RemoteConnection(true, "localhost", "0", true)

  public val portForDebug: Int?
    get() = remoteConnection.debuggerAddress?.toInt()

  override fun createAndAddTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener =
    BspRunTaskListener(handler)

  override fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities): CompletableFuture<*> {
    if (!capabilities.runWithDebugProvider) {
      throw ExecutionException("BSP server does not support running")
    }

    val configuration = environment.runProfile as BspRunConfiguration
    val targetId = BuildTargetIdentifier(configuration.targets.single())
    val runParams = RunParams(targetId)
    runParams.originId = originId
    val remoteDebugData = RemoteDebugData("jdwp", portForDebug!!)
    val runWithDebugParams = RunWithDebugParams(originId, runParams, remoteDebugData)

    return server.buildTargetRunWithDebug(runWithDebugParams)
  }
}
