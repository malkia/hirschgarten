package org.jetbrains.plugins.bsp.server.client

import ch.epfl.scala.bsp4j.BuildServer
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.CleanCacheParams
import ch.epfl.scala.bsp4j.CleanCacheResult
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileReport
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.CompileTask
import ch.epfl.scala.bsp4j.DebugSessionAddress
import ch.epfl.scala.bsp4j.DebugSessionParams
import ch.epfl.scala.bsp4j.DependencyModulesParams
import ch.epfl.scala.bsp4j.DependencyModulesResult
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.InitializeBuildResult
import ch.epfl.scala.bsp4j.InverseSourcesParams
import ch.epfl.scala.bsp4j.InverseSourcesResult
import ch.epfl.scala.bsp4j.OutputPathsParams
import ch.epfl.scala.bsp4j.OutputPathsResult
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.RunParams
import ch.epfl.scala.bsp4j.RunResult
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TaskDataKind
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskId
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartParams
import ch.epfl.scala.bsp4j.TestParams
import ch.epfl.scala.bsp4j.TestResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.google.gson.Gson
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

private class MockServer : BuildServer {

  val compileTasks = mutableMapOf<OriginId, CompletableFuture<CompileResult>>()

  override fun buildTargetCompile(params: CompileParams): CompletableFuture<CompileResult> {
    val originId = OriginId(params.originId)
    val compileTask = CompletableFuture<CompileResult>()
    compileTasks[originId] = compileTask
    return compileTask
  }

  fun finishCompileTask(originId: OriginId, result: CompileResult) {
    compileTasks[originId]?.complete(result)
  }

  fun cancelCompileTask(originId: OriginId) {
    compileTasks[originId]?.cancel(true)
  }

  fun abortCompileTask(originId: OriginId, throwable: Throwable) {
    compileTasks[originId]?.completeExceptionally(throwable)
  }

  override fun buildTargetTest(params: TestParams?): CompletableFuture<TestResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetRun(params: RunParams?): CompletableFuture<RunResult> {
    TODO("Not yet implemented")
  }

  // Not needed for tasks

  override fun buildInitialize(params: InitializeBuildParams?): CompletableFuture<InitializeBuildResult> {
    TODO("Not yet implemented")
  }

  override fun onBuildInitialized() {
    TODO("Not yet implemented")
  }

  override fun buildShutdown(): CompletableFuture<Any> {
    TODO("Not yet implemented")
  }

  override fun onBuildExit() {
    TODO("Not yet implemented")
  }

  override fun workspaceBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult> {
    TODO("Not yet implemented")
  }

  override fun workspaceReload(): CompletableFuture<Any> {
    TODO("Not yet implemented")
  }

  override fun buildTargetSources(params: SourcesParams?): CompletableFuture<SourcesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetInverseSources(params: InverseSourcesParams?): CompletableFuture<InverseSourcesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetDependencySources(params: DependencySourcesParams?): CompletableFuture<DependencySourcesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetResources(params: ResourcesParams?): CompletableFuture<ResourcesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetOutputPaths(params: OutputPathsParams?): CompletableFuture<OutputPathsResult> {
    TODO("Not yet implemented")
  }

  override fun debugSessionStart(params: DebugSessionParams?): CompletableFuture<DebugSessionAddress> {
    TODO("Not yet implemented")
  }

  override fun buildTargetCleanCache(params: CleanCacheParams?): CompletableFuture<CleanCacheResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetDependencyModules(params: DependencyModulesParams?): CompletableFuture<DependencyModulesResult> {
    TODO("Not yet implemented")
  }
}

class MockCompileTaskObserver : CompileTaskObserver {
  val taskStartedNotifications = mutableListOf<ClientTaskStartedParams<ClientCompileStartedData>>()
  val taskProgressNotifications = mutableListOf<ClientTaskProgressParams<Nothing>>()
  val taskFinishedNotifications = mutableListOf<ClientTaskFinishedParams<ClientCompileFinishedData>>()
  val topLevelCompileTaskFinishedNotifications = mutableListOf<ClientCompileResult>()
  val topLevelTaskFailedNotifications = mutableListOf<Throwable>()

  override fun onTaskStarted(params: ClientTaskStartedParams<ClientCompileStartedData>) {
    taskStartedNotifications.add(params)
  }

  override fun onTaskProgress(params: ClientTaskProgressParams<Nothing>) {
    taskProgressNotifications.add(params)
  }

  override fun onTaskFinished(params: ClientTaskFinishedParams<ClientCompileFinishedData>) {
    taskFinishedNotifications.add(params)
  }

  override fun onTopLevelTaskFinished(params: ClientCompileResult) {
    topLevelCompileTaskFinishedNotifications.add(params)
  }

  override fun onTopLevelTaskFailed(throwable: Throwable) {
    topLevelTaskFailedNotifications.add(throwable)
  }
}

class TaskClientTest {
  private val server = MockServer()
  private val client = TaskClient(server)
  private val listener = client.BspClientListener()
  private val gson = Gson()

  @Test
  fun `compile task correctly starts and finishes`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)
    val result = CompileResult(StatusCode.OK)
    server.finishCompileTask(compileTask.originId, result)
    observer.topLevelCompileTaskFinishedNotifications shouldBe listOf(
      ClientCompileResult(result.statusCode)
    )
    observer.topLevelTaskFailedNotifications shouldBe emptyList()
    observer.taskStartedNotifications shouldBe emptyList()
    observer.taskProgressNotifications shouldBe emptyList()
    observer.taskFinishedNotifications shouldBe emptyList()
  }

  @Test
  fun `compile task correctly reports subtasks`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)
    val subtaskId = TaskId("subtask1")
    subtaskId.parents = listOf(compileTask.originId.id)
    val subtaskStart = TaskStartParams(subtaskId)
    listener.onBuildTaskStart(subtaskStart)
    val nowLong = Instant.now().toEpochMilli()
    val now = Instant.ofEpochMilli(nowLong)
    val subtaskProgress = TaskProgressParams(subtaskId)
    subtaskProgress.eventTime = nowLong
    subtaskProgress.message = "subtask1 progress"
    listener.onBuildTaskProgress(subtaskProgress)
    val subtaskFinish = TaskFinishParams(subtaskId, StatusCode.OK)
    subtaskFinish.eventTime = nowLong
    listener.onBuildTaskFinish(subtaskFinish)

    val compileSubtaskId = TaskId("subtask2")
    compileSubtaskId.parents = listOf(compileTask.originId.id)
    val compileSubtaskStart = TaskStartParams(compileSubtaskId)
    compileSubtaskStart.dataKind = TaskDataKind.COMPILE_TASK
    val compileSubtaskTarget = BuildTargetIdentifier("target")
    val compileSubtaskData = CompileTask(compileSubtaskTarget)
    compileSubtaskStart.data = gson.toJsonTree(compileSubtaskData)
    listener.onBuildTaskStart(compileSubtaskStart)
    val compileSubtaskFinish = TaskFinishParams(compileSubtaskId, StatusCode.OK)
    compileSubtaskFinish.dataKind = TaskDataKind.COMPILE_REPORT
    val compileSubtaskReport = CompileReport(compileSubtaskTarget, 1, 2)
    compileSubtaskFinish.data = gson.toJsonTree(compileSubtaskReport)
    listener.onBuildTaskFinish(compileSubtaskFinish)

    val result = CompileResult(StatusCode.OK)
    server.finishCompileTask(compileTask.originId, result)

    observer.taskStartedNotifications shouldBe listOf(
      ClientTaskStartedParams(ClientTaskId(subtaskId.id), ClientTaskId(compileTask.originId.id), null, null, null),
      ClientTaskStartedParams(
        ClientTaskId(compileSubtaskId.id), ClientTaskId(compileTask.originId.id),
        null, null, ClientCompileStartedData(compileSubtaskData)
      )
    )

    observer.taskProgressNotifications shouldBe listOf(
      ClientTaskProgressParams(ClientTaskId(subtaskId.id), ClientTaskId(compileTask.originId.id), now, "subtask1 progress", null, null, null, null)
    )

    observer.taskFinishedNotifications shouldBe listOf(
      ClientTaskFinishedParams(ClientTaskId(subtaskId.id), ClientTaskId(compileTask.originId.id), now, null, StatusCode.OK, null),
      ClientTaskFinishedParams(
        ClientTaskId(compileSubtaskId.id), ClientTaskId(compileTask.originId.id),
        null, null, StatusCode.OK, ClientCompileFinishedData(compileSubtaskReport),
      )
    )
  }

  @Test
  fun `compile task correctly reports nested subtasks`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)
    val subtaskId = TaskId("subtask1")
    subtaskId.parents = listOf(compileTask.originId.id)
    val subtaskStart = TaskStartParams(subtaskId)
    listener.onBuildTaskStart(subtaskStart)

    val subSubtask1Id = TaskId("subsubtask1")
    subSubtask1Id.parents = listOf(subtaskId.id)
    val subSubTask1Start = TaskStartParams(subSubtask1Id)
    listener.onBuildTaskStart(subSubTask1Start)

    val subSubtask2Id = TaskId("subsubtask2")
    subSubtask2Id.parents = listOf(subtaskId.id)
    val subSubTask2Start = TaskStartParams(subSubtask2Id)
    listener.onBuildTaskStart(subSubTask2Start)

    listener.onBuildTaskFinish(TaskFinishParams(subSubtask1Id, StatusCode.OK))
    listener.onBuildTaskFinish(TaskFinishParams(subSubtask2Id, StatusCode.OK))
    listener.onBuildTaskFinish(TaskFinishParams(subtaskId, StatusCode.OK))

    val result = CompileResult(StatusCode.OK)
    server.finishCompileTask(compileTask.originId, result)

    observer.taskStartedNotifications shouldBe listOf(
      ClientTaskStartedParams(ClientTaskId(subtaskId.id), ClientTaskId(compileTask.originId.id), null, null, null),
      ClientTaskStartedParams(ClientTaskId(subSubtask1Id.id), ClientTaskId(subtaskId.id), null, null, null),
      ClientTaskStartedParams(ClientTaskId(subSubtask2Id.id), ClientTaskId(subtaskId.id), null, null, null),
    )

    observer.taskFinishedNotifications shouldBe listOf(
      ClientTaskFinishedParams(ClientTaskId(subSubtask1Id.id), ClientTaskId(subtaskId.id), null, null, StatusCode.OK, null),
      ClientTaskFinishedParams(ClientTaskId(subSubtask2Id.id), ClientTaskId(subtaskId.id), null, null, StatusCode.OK, null),
      ClientTaskFinishedParams(ClientTaskId(subtaskId.id), ClientTaskId(compileTask.originId.id), null, null, StatusCode.OK, null),
    )
  }

  @Test
  fun `listener throws exception on parentless subtask`() {
    val subtaskId = TaskId("subtask1")
    val subtaskStart = TaskStartParams(subtaskId)

    shouldThrowExactly<NoParent> { listener.onBuildTaskStart(subtaskStart) }
  }

  @Test
  fun `listener throws exception on subtask with unknown parent`() {
    val subtaskId = TaskId("subtask1")
    subtaskId.parents = listOf("unknown")
    val subtaskStart = TaskStartParams(subtaskId)

    val throwable = shouldThrowExactly<OriginNotFound> { listener.onBuildTaskStart(subtaskStart) }
    throwable.originId shouldBe OriginId("unknown")
    throwable.taskId shouldBe ClientTaskId("subtask1")
  }

  @Test
  fun `listener throws exception on subtask with a parent with no children`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)

    val parent = TaskId("parent")
    parent.parents = listOf(compileTask.originId.id)
    val subtaskStart = TaskStartParams(parent)

    listener.onBuildTaskStart(subtaskStart)

    val other = TaskId("other")
    other.parents = listOf(compileTask.originId.id)
    val otherStart = TaskStartParams(other)

    listener.onBuildTaskStart(otherStart)


    val child = TaskId("child")
    child.parents = listOf(parent.id)
    val childStarted = TaskStartParams(child)
    listener.onBuildTaskStart(childStarted)

    child.parents = listOf(other.id)
    val childProgress = TaskProgressParams(child)

    val throwable = shouldThrowExactly<IncorrectSubtaskParent> { listener.onBuildTaskProgress(childProgress) }

    server.finishCompileTask(compileTask.originId, CompileResult(StatusCode.OK))

    throwable.originId shouldBe OriginId(compileTask.originId.id)
    throwable.parentTaskId shouldBe ClientTaskId(other.id)
    throwable.taskId shouldBe ClientTaskId(child.id)
  }

  @Test
  fun `listener throws exception when trying to start already started task`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)

    val subtask = TaskId("subtask")
    subtask.parents = listOf(compileTask.originId.id)
    val subtaskStart = TaskStartParams(subtask)

    listener.onBuildTaskStart(subtaskStart)

    val throwable = shouldThrowExactly<SubtaskAlreadyStarted> { listener.onBuildTaskStart(subtaskStart) }

    server.finishCompileTask(compileTask.originId, CompileResult(StatusCode.OK))

    throwable.originId shouldBe OriginId(compileTask.originId.id)
    throwable.taskId shouldBe ClientTaskId(subtask.id)
  }

  @Test
  fun `listener throws exception when trying to finish already finished task`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)

    val subtask = TaskId("subtask")
    subtask.parents = listOf(compileTask.originId.id)
    val subtaskStart = TaskStartParams(subtask)

    listener.onBuildTaskStart(subtaskStart)

    val subtaskFinish = TaskFinishParams(subtask, StatusCode.OK)
    listener.onBuildTaskFinish(subtaskFinish)

    val throwable = shouldThrowExactly<SubtaskNotFound> { listener.onBuildTaskFinish(subtaskFinish) }

    server.finishCompileTask(compileTask.originId, CompileResult(StatusCode.OK))

    throwable.originId shouldBe OriginId(compileTask.originId.id)
    throwable.taskId shouldBe ClientTaskId(subtask.id)
  }

  @Test
  fun `listener throws exception when trying to deserialize unknown data kind`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)

    val subtask = TaskId("subtask")
    subtask.parents = listOf(compileTask.originId.id)
    val subtaskStart = TaskStartParams(subtask)
    subtaskStart.dataKind = "I like trains"

    val throwable = shouldThrowExactly<UnsupportedDataKind> { listener.onBuildTaskStart(subtaskStart) }

    server.finishCompileTask(compileTask.originId, CompileResult(StatusCode.OK))

    throwable.taskId shouldBe ClientTaskId(subtask.id)
    throwable.kind shouldBe "I like trains"
  }

  @Test
  fun `listener throws exception when trying to deserialize incorrect data`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)

    val subtask = TaskId("subtask")
    subtask.parents = listOf(compileTask.originId.id)
    val subtaskStart = TaskStartParams(subtask)
    subtaskStart.dataKind = TaskDataKind.COMPILE_TASK
    subtaskStart.data = "I like trains"

    val throwable = shouldThrowExactly<DeserializationError> { listener.onBuildTaskStart(subtaskStart) }

    server.finishCompileTask(compileTask.originId, CompileResult(StatusCode.OK))

    throwable.taskId shouldBe ClientTaskId(subtask.id)
  }

  // NOTE: Mock server handles cancellation differently from real server
  // see https://github.com/eclipse/lsp4j/blob/main/documentation/jsonrpc.md#cancelling-requests
  @Test
  fun `client-side cancellation is supported`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)
    compileTask.cancel()
    observer.topLevelTaskFailedNotifications[0] shouldBe CancellationException()
  }

  @Test
  fun `server-side cancellation is supported`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)
    server.cancelCompileTask(compileTask.originId)
    observer.topLevelTaskFailedNotifications[0] shouldBe CancellationException()
  }

  @Test
  fun `exceptions thrown during top level task execution are correctly handled`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)
    server.abortCompileTask(compileTask.originId, RuntimeException("Exception thrown on server"))
    observer.topLevelTaskFailedNotifications[0] shouldBe RuntimeException("Exception thrown on server")
  }

  @Test
  fun `finishing a subtask removes its children`() {
    val observer = MockCompileTaskObserver()
    val params = ClientCompileTaskParams(listOf(), listOf())
    val compileTask = client.startCompileTask(params, observer)

    val subtask = TaskId("subtask")
    subtask.parents = listOf(compileTask.originId.id)
    val subtaskStart = TaskStartParams(subtask)

    listener.onBuildTaskStart(subtaskStart)

    val child = TaskId("child")
    child.parents = listOf(subtask.id)
    val childStarted = TaskStartParams(child)
    // start the child
    listener.onBuildTaskStart(childStarted)

    // change child's parent to compileTask
    child.parents = listOf(compileTask.originId.id)
    // try to start the child again
    val subtaskAlreadyStarted = shouldThrowExactly<SubtaskAlreadyStarted> { listener.onBuildTaskStart(childStarted) }

    val subtaskFinish = TaskFinishParams(subtask, StatusCode.OK)
    // finish the parent
    listener.onBuildTaskFinish(subtaskFinish)

    // start the child again (if this succeeds, the child was removed)
    listener.onBuildTaskStart(childStarted)

    server.finishCompileTask(compileTask.originId, CompileResult(StatusCode.OK))

    subtaskAlreadyStarted.originId shouldBe OriginId(compileTask.originId.id)
    subtaskAlreadyStarted.taskId shouldBe ClientTaskId(child.id)
  }

}
