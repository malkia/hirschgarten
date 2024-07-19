package org.jetbrains.plugins.bsp.flow.sync

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.coroutines.forEachConcurrent
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.performance.testing.bspTracer
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresProvider
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.server.connection.reactToExceptionIn
import org.jetbrains.plugins.bsp.server.tasks.catchSyncErrors
import org.jetbrains.plugins.bsp.server.tasks.saveAllFiles
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.syncConsole
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

private const val projectSyncTaskId = "project-sync"

class ProjectSyncTask(private val project: Project) {
  private val cancelOnFuture = CompletableFuture<Void>()
  private var coroutineJob: Job? = null

  suspend fun sync(buildProject: Boolean) {
    saveAllFiles()
    withContext(Dispatchers.Default) {
      coroutineJob = launch {
        try {
          withBackgroundProgress(project, "Syncing project...", true) {
            reportSequentialProgress {
              doSync(it, buildProject)
            }
          }
        } catch (e: CancellationException) {
          onCancel(e)
        }
      }
      coroutineJob?.join()
    }
  }

  private suspend fun doSync(progressReporter: SequentialProgressReporter, buildProject: Boolean) {
    val diff = AllProjectStructuresProvider(project).newDiff()

    project.connection.runWithServerAsync { server, capabilities ->
      bspTracer.spanBuilder("collect.project.details.ms").use {
        val baseTargetInfos = BaseProjectSync(project).execute(buildProject, server, capabilities, cancelOnFuture, { errorCallback(it) })
        defaultProjectSyncHooks.forEach {
          it.onSync(
            project = project,
            server = server,
            capabilities = capabilities,
            diff = diff,
            taskId = projectSyncTaskId,
            progressReporter = progressReporter,
            baseTargetInfos = baseTargetInfos,
            cancelOn = cancelOnFuture,
            errorCallback = { errorCallback(it) })
        }
        project.additionalProjectSyncHooks.forEach {
          it.onSync(
            project = project,
            server = server,
            capabilities = capabilities,
            diff = diff,
            taskId = projectSyncTaskId,
            progressReporter = progressReporter,
            baseTargetInfos = baseTargetInfos,
            cancelOn = cancelOnFuture,
            errorCallback = { errorCallback(it) })
        }
      }
    }

    diff.applyAll()
  }

  private fun onCancel(e: Exception) {
    cancelOnFuture.cancel(true)
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    bspSyncConsole.finishTask(projectSyncTaskId, e.message ?: "", FailureResultImpl(e))
  }

  fun isCancellationException(e: Throwable): Boolean =
    e is CompletionException && e.cause is CancellationException

  fun isTimeoutException(e: Throwable): Boolean =
    e is CompletionException && e.cause is TimeoutException

  fun errorCallback(e: Throwable) = when {
    isCancellationException(e) ->
      project.syncConsole.finishTask(
        taskId = projectSyncTaskId,
        message = BspPluginBundle.message("console.task.exception.cancellation"),
        result = FailureResultImpl(BspPluginBundle.message("console.task.exception.cancellation.message")),
      )

    isTimeoutException(e) ->
      project.syncConsole.finishTask(
        taskId = projectSyncTaskId,
        message = BspPluginBundle.message("console.task.exception.timed.out"),
        result = FailureResultImpl(BspPluginBundle.message("console.task.exception.timeout.message")),
      )

    else -> project.syncConsole.finishTask(
      projectSyncTaskId,
      BspPluginBundle.message("console.task.exception.other"), FailureResultImpl(e)
    )
  }
}

fun <Result> queryIf(
  check: Boolean,
  queryName: String,
  cancelOn: CompletableFuture<Void>,
  errorCallback: (Throwable) -> Unit,
  doQuery: () -> CompletableFuture<Result>,
): CompletableFuture<Result>? = if (check) query(queryName, cancelOn, errorCallback, doQuery) else null

fun <Result> query(
  queryName: String,
  cancelOn: CompletableFuture<Void>,
  errorCallback: (Throwable) -> Unit,
  doQuery: () -> CompletableFuture<Result>,
): CompletableFuture<Result> =
  doQuery()
    .reactToExceptionIn(cancelOn)
    .catchSyncErrors(errorCallback)
    .exceptionally {
      fileLogger().warn("Query '$queryName' has failed", it)
      null
    }