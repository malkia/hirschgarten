package org.jetbrains.plugins.bsp.flow.sync

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspSyncStatusService
import org.jetbrains.plugins.bsp.performance.testing.bspTracer
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresProvider
import org.jetbrains.plugins.bsp.server.client.importSubtaskId
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

private val log = logger<ProjectSyncTask>()

class ProjectSyncTask(private val project: Project) {
  private val cancelOnFuture = CompletableFuture<Void>()
  private var coroutineJob: Job? = null

  suspend fun sync(buildProject: Boolean) {
    bspTracer.spanBuilder("bsp.sync.project.ms").useWithScope {
      try {
        log.debug("Starting sync project task")
        project.syncConsole.startTask(
          taskId = projectSyncTaskId,
          title = BspPluginBundle.message("console.task.sync.title"),
          message = BspPluginBundle.message("console.task.sync.in.progress"),
          cancelAction = { cancelExecution() },
        )

        preSync()
        doSync(buildProject)

        project.syncConsole.finishTask(projectSyncTaskId, BspPluginBundle.message("console.task.sync.success"))
      } catch (e: CancellationException) {
        onCancel(e)
      } catch (e: Exception) {
        log.debug("BSP sync failed")
        project.syncConsole.finishTask(projectSyncTaskId, BspPluginBundle.message("console.task.sync.failed"), FailureResultImpl(e))
      } finally {
          BspSyncStatusService.getInstance(project).finishSync()
      }
    }
  }

  private fun preSync() {
    log.debug("Running pre sync tasks")
    BspSyncStatusService.getInstance(project).startSync()
    saveAllFiles()
  }

  private suspend fun doSync(buildProject: Boolean) {
    withContext(Dispatchers.Default) {
      coroutineJob = launch {
        withBackgroundProgress(project, "Syncing project...", true) {
          reportSequentialProgress {
            executeSyncHooks(it, buildProject)
          }
        }
      }
      coroutineJob?.join()
    }
  }

  private suspend fun executeSyncHooks(progressReporter: SequentialProgressReporter, buildProject: Boolean) {
    log.debug("Connecting to the server")
    runInterruptible { project.connection.connect(projectSyncTaskId) { cancelExecution() } }

    val diff = AllProjectStructuresProvider(project).newDiff()
    project.connection.runWithServerAsync { server, capabilities ->
      bspTracer.spanBuilder("collect.project.details.ms").use {
        project.syncConsole.startSubtask(
          projectSyncTaskId, importSubtaskId,
          BspPluginBundle.message("console.task.model.collect.in.progress"),
        )

        val baseTargetInfos = BaseProjectSync(project).execute(buildProject, server, capabilities, cancelOnFuture, { errorCallback(it) })
        project.defaultProjectSyncHooks.forEach {
          it.onSync(
            project = project,
            server = server,
            capabilities = capabilities,
            diff = diff,
            taskId = projectSyncTaskId,
            progressReporter = progressReporter,
            baseTargetInfos = baseTargetInfos,
            cancelOn = cancelOnFuture,
            errorCallback = { e -> errorCallback(e) },
          )
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
            errorCallback = { e -> errorCallback(e) },
          )
        }
      }
    }

    diff.applyAll()
    project.syncConsole.finishTask(projectSyncTaskId, "")
  }

  private fun onCancel(e: Exception) {
    cancelOnFuture.cancel(true)
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    bspSyncConsole.finishTask(projectSyncTaskId, e.message ?: "", FailureResultImpl(e))
  }

  private fun cancelExecution() {
    BspSyncStatusService.getInstance(project).cancel()
    cancelOnFuture.cancel(true)
  }

  private fun isCancellationException(e: Throwable): Boolean =
    e is CompletionException && e.cause is CancellationException

  private fun isTimeoutException(e: Throwable): Boolean =
    e is CompletionException && e.cause is TimeoutException

  private fun errorCallback(e: Throwable) = when {
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
      BspPluginBundle.message("console.task.exception.other"), FailureResultImpl(e),
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
