package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TestFinish
import ch.epfl.scala.bsp4j.TestReport
import ch.epfl.scala.bsp4j.TestStart
import ch.epfl.scala.bsp4j.TestStatus
import ch.epfl.scala.bsp4j.TestTask
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.TaskId
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler

public class BspTestTaskListener(private val handler: BspProcessHandler<out Any>) : BspTaskListener {
  private val ansiEscapeDecoder = AnsiEscapeDecoder()
  private val gson = Gson()

  init {
    handler.addProcessListener(object : ProcessListener {
      override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {}
    })
  }

  override fun onTaskStart(
    taskId: TaskId,
    parentId: TaskId?,
    message: String,
    data: Any?,
  ) {
    when (data) {
      is TestTask -> {
        // OutputToGeneralTestEventsConverter.MyServiceMessageVisitor.visitServiceMessage ignores the first testingStarted event
        handler.notifyTextAvailable(ServiceMessageBuilder("testingStarted").toString(), ProcessOutputType.STDOUT)
        handler.notifyTextAvailable(ServiceMessageBuilder("testingStarted").toString(), ProcessOutputType.STDOUT)
      }

      is TestStart -> {
        val serviceMessage = if (message == TEST_TAG) ServiceMessageBuilder.testStarted(data.displayName)
          .addNodeId(taskId)
          .addAttribute("parentNodeId", parentId ?: "0").toString()
        else
          ServiceMessageBuilder.testSuiteStarted(data.displayName)
            .addAttribute("parentNodeId", "0")
            .addNodeId(taskId)
            .toString()
        handler.notifyTextAvailable(serviceMessage, ProcessOutputType.STDOUT)
      }
    }
  }

  override fun onTaskFinish(
    taskId: TaskId,
    message: String,
    status: StatusCode,
    data: Any?,
  ) {
    when (data) {
      is TestReport -> {
        handler.notifyTextAvailable(ServiceMessageBuilder("testingFinished").toString(), ProcessOutputType.STDOUT)
      }

      is TestFinish -> {
        val serviceMessage =
          if (message == TEST_TAG)
            processTestCaseFinish(taskId, message, data)
          else
            processTestSuiteFinish(taskId, data)

        handler.notifyTextAvailable(serviceMessage.toString(), ProcessOutputType.STDOUT)
      }
    }
  }

  override fun onOutputStream(taskId: TaskId?, text: String) {
    ansiEscapeDecoder.escapeText(text, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }

  override fun onErrorStream(taskId: TaskId?, text: String) {
    ansiEscapeDecoder.escapeText(text, ProcessOutputType.STDERR) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }

  // For compatibility with older BSP servers
  // TODO: Log messages in the correct place
  override fun onLogMessage(message: String) {
    val messageWithNewline = if (message.endsWith("\n")) message else "$message\n"
    ansiEscapeDecoder.escapeText(messageWithNewline, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }

  private fun ServiceMessageBuilder.addNodeId(nodeId: String): ServiceMessageBuilder =
    this.addAttribute("nodeId", nodeId)

  private fun ServiceMessageBuilder.addMessage(message: String?): ServiceMessageBuilder =
    message?.takeIf { it.isNotEmpty() }?.let { this.addAttribute("message", it) } ?: this

  private fun checkTestStatus(taskId: TaskId, data: TestFinish, details: JUnitStyleTestCaseData?, message: String) {
    if (message == SUITE_TAG) return

    val failureMessageBuilder = when (data.status!!) {
      TestStatus.FAILED -> {
        ServiceMessageBuilder.testFailed(data.displayName)
      }

      TestStatus.CANCELLED -> {
        ServiceMessageBuilder.testIgnored(data.displayName)
      }

      TestStatus.IGNORED -> {
        ServiceMessageBuilder.testIgnored(data.displayName)
      }

      TestStatus.SKIPPED -> {
        ServiceMessageBuilder.testIgnored(data.displayName)
      }

      else -> null
    }

    if (failureMessageBuilder != null) {
      failureMessageBuilder.addNodeId(taskId)
        .addMessage(details?.errorMessage)
        .addAttribute("type", details?.errorType ?: "")
        .toString()
      handler.notifyTextAvailable(failureMessageBuilder.toString(), ProcessOutputType.STDOUT)
      details?.errorContent?.let { handler.notifyTextAvailable(it, ProcessOutputType.STDERR) }
    }
  }

  private fun processTestCaseFinish(taskId: TaskId, message: String, data: TestFinish): ServiceMessageBuilder {
    val details = if (data.dataKind == JUnitStyleTestCaseData.DATA_KIND)
      gson.fromJson(data.data as JsonObject, JUnitStyleTestCaseData::class.java) else null

    checkTestStatus(taskId, data, details, message)

    return ServiceMessageBuilder.testFinished(data.displayName)
      .addNodeId(taskId)
      .addMessage(data.message)
  }

  private fun processTestSuiteFinish(taskId: TaskId, data: TestFinish): ServiceMessageBuilder {
    val details = if (data.dataKind == JUnitStyleTestSuiteData.DATA_KIND)
      gson.fromJson(data.data as JsonObject, JUnitStyleTestSuiteData::class.java) else null


    details?.systemOut?.let { handler.notifyTextAvailable(it, ProcessOutputType.STDOUT) }
    details?.systemErr?.let { handler.notifyTextAvailable(it, ProcessOutputType.STDERR) }
    return ServiceMessageBuilder.testSuiteFinished(data.displayName)
      .addNodeId(taskId)
  }

  companion object {
    private const val SUITE_TAG = "<S>"
    private const val TEST_TAG = "<T>"
  }
}
