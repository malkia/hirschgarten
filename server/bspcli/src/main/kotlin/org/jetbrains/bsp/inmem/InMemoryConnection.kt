package org.jetbrains.bsp.inmem

import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.BuildServerCapabilities
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.jetbrains.bsp.bazel.server.BazelBspServer
import org.jetbrains.bsp.bazel.server.benchmark.TelemetryConfig
import org.jetbrains.bsp.bazel.server.bsp.BspIntegrationData
import org.jetbrains.bsp.bazel.server.bsp.info.BspInfo
import org.jetbrains.bsp.bazel.workspacecontext.DefaultWorkspaceContextProvider
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.utils.BazelBuildServerCapabilitiesTypeAdapter
import org.jetbrains.plugins.bsp.server.connection.TelemetryContextPropagatingLauncherBuilder
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class Connection(installationDirectory: Path, metricsFile: Path?, workspace: Path, client: BuildClient) {
    val serverOut = FixedThreadPipedOutputStream()
    val clientOut = FixedThreadPipedOutputStream()
    val serverExecutor = Executors.newFixedThreadPool(4, threadFactory("cli-server-pool-%d"))
    val telemetryConfig = TelemetryConfig(metricsFile = metricsFile)
    val serverLauncher = startServer(
        serverOut,
        clientOut.inputStream,
        serverExecutor,
        workspace,
        installationDirectory,
        telemetryConfig
    )
    val serverAliveFuture = serverLauncher.startListening()

    val clientExecutor = Executors.newFixedThreadPool(4, threadFactory("cli-client-pool-%d"))
    val clientLauncher = startClient(serverOut.inputStream, clientOut, clientExecutor, client)
    val clientAliveFuture = clientLauncher.startListening()

    fun stop() {
        clientExecutor.shutdown()
        serverExecutor.shutdown()

        clientOut.stop()
        serverOut.stop()

        clientAliveFuture.get()
        serverAliveFuture.get()
    }
}

/**
 * This class is required, because of limitations of java's PipedStreams.
 * Unfortunately, whenever PipedInputStream calls read ([code](https://github.com/openjdk/jdk/blob/e30e3564420c631f08ac3d613ab91c93227a00b3/src/java.base/share/classes/java/io/PipedInputStream.java#L314-L316)),
 * it checks whether the writing thread is alive. Unfortunately, in case of Bazel BSP server, there are a lot of writes
 * from different threads, that are often spawned only temporarily, from java's Executors.
 *
 * The idea how to solve it is to create a single thread, which lifetime is longer than both PipedOutputStream and
 * PipedInputStream, and it's the only thread that is allowed to write to PipedOutputStream
 */
class FixedThreadPipedOutputStream : OutputStream() {
    val inputStream = PipedInputStream()
    private val outputStream = PrintStream(PipedOutputStream(inputStream), true)
    private val queue = ArrayBlockingQueue<Int>(10000)
    private val _stop = AtomicBoolean(false)
    private val thread = Thread {
        while (!_stop.get()) {
            queue.poll(100, TimeUnit.MILLISECONDS)
                ?.let { outputStream.write(it) }
        }
    }.also { it.start() }

    fun stop() {
        outputStream.close()
        inputStream.close()
        _stop.set(true)
        thread.join()
    }

    override fun write(b: Int) {
        queue.put(b)
    }
}

private fun threadFactory(nameFormat: String): ThreadFactory =
    ThreadFactoryBuilder()
        .setNameFormat(nameFormat)
        .setUncaughtExceptionHandler { _, e ->
            e.printStackTrace()
            exitProcess(1)
        }
        .build()

private fun startClient(
    serverOut: PipedInputStream, clientIn: OutputStream, clientExecutor: ExecutorService?, buildClient: BuildClient
): Launcher<JoinedBuildServer> =
    TelemetryContextPropagatingLauncherBuilder<JoinedBuildServer>()
        .setInput(serverOut)
        .setOutput(clientIn)
        .setRemoteInterface(JoinedBuildServer::class.java)
        .setExecutorService(clientExecutor)
        .setLocalService(buildClient)
        .configureGson { builder ->
            builder.registerTypeAdapter(
                BuildServerCapabilities::class.java,
                BazelBuildServerCapabilitiesTypeAdapter(),
            )
        }
        .create()

private fun startServer(serverIn: OutputStream,
                        clientOut: PipedInputStream,
                        serverExecutor: ExecutorService,
                        workspace: Path,
                        directory: Path,
                        telemetryConfig: TelemetryConfig): Launcher<JoinedBuildClient> {
    val bspInfo = BspInfo(directory)
    val bspIntegrationData = BspIntegrationData(serverIn, clientOut, serverExecutor, null)
    val workspaceContextProvider = DefaultWorkspaceContextProvider(
        workspaceRoot = workspace,
        projectViewPath = directory.resolve("projectview.bazelproject")
    )
    val bspServer = BazelBspServer(bspInfo, workspaceContextProvider, workspace, telemetryConfig)
    return bspServer.buildServer(bspIntegrationData)
}