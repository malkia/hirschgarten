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

  init {
    handler.addProcessListener(object : ProcessListener {
      override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
      }
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
        handler.notifyTextAvailable("\n##teamcity[testingStarted]\n", ProcessOutputType.STDOUT)
      }

      is TestStart -> {
        val serviceMessage = if (message == "<T>") ServiceMessageBuilder.testStarted(data.displayName)
          .addAttribute("nodeId", taskId)
          .addAttribute("parentNodeId", parentId ?: "0").toString()
        else
          ServiceMessageBuilder.testSuiteStarted(data.displayName)
            .addAttribute("parentNodeId", "0")
            .addAttribute("nodeId", taskId)
            .toString()
        handler.notifyTextAvailable(serviceMessage, ProcessOutputType.STDOUT)
        handler.notifyTextAvailable("\n${serviceMessage.substringAfterLast("#teamcity")}\n", ProcessOutputType.STDOUT)
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
        handler.notifyTextAvailable("\n##teamcity[testingFinished]\n", ProcessOutputType.STDOUT)
      }

      is TestFinish -> {
        val failureMessageBuilder =
          when (data.status!!) {
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
          }?.addAttribute("nodeId", taskId)

        if (failureMessageBuilder != null && message == "<T>") {
          failureMessageBuilder.addAttribute("message", data.message ?: "No message")
          handler.notifyTextAvailable(failureMessageBuilder.toString(), ProcessOutputType.STDOUT)
        }

        val serviceMessage = if (message == "<T>") ServiceMessageBuilder.testFinished(data.displayName)
          .addAttribute("nodeId", taskId)
          .addAttribute("message", data.message ?: "No message")
          .toString()
        else
          ServiceMessageBuilder.testSuiteFinished(data.displayName)
            .addAttribute("nodeId", taskId)
            .addAttribute("message", data.message ?: "No message")
            .toString()
        handler.notifyTextAvailable(serviceMessage, ProcessOutputType.STDOUT)
        handler.notifyTextAvailable("\n${serviceMessage.substringAfterLast("#teamcity")}\n", ProcessOutputType.STDOUT)
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
}
