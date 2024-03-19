package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.StatusCode
import ch.epfl.scala.bsp4j.TestFinish
import ch.epfl.scala.bsp4j.TestReport
import ch.epfl.scala.bsp4j.TestStart
import ch.epfl.scala.bsp4j.TestTask
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.TaskId
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler

public class BspTestTaskListener(private val handler: BspProcessHandler<out Any>) : BspTaskListener {
  private val ansiEscapeDecoder = AnsiEscapeDecoder()
  override fun onTaskStart(taskId: TaskId, parentId: TaskId?, message: String, data: Any?) {
    when (data) {
      is TestStart -> {
        val testSuiteStarted = "\n" + ServiceMessageBuilder.testSuiteStarted(data.displayName).toString() + "\n"
        handler.notifyTextAvailable(testSuiteStarted, ProcessOutputType.STDOUT)
      }

      is TestTask -> {
        val testStarted = "\n" + ServiceMessageBuilder.testStarted(data.target.uri).toString() + "\n"
        handler.notifyTextAvailable(testStarted, ProcessOutputType.STDOUT)
      }
    }
  }

  override fun onTaskFinish(taskId: TaskId, message: String, status: StatusCode, data: Any?) {
    when (data) {
      is TestFinish -> {
        val testSuiteFinished = "\n" + ServiceMessageBuilder.testSuiteFinished(data.displayName).toString() + "\n"
        handler.notifyTextAvailable(testSuiteFinished, ProcessOutputType.STDOUT)
      }

      is TestReport -> {
        val testFinished = "\n" + ServiceMessageBuilder.testFinished(data.target.uri).toString() + "\n"
        handler.notifyTextAvailable(testFinished, ProcessOutputType.STDOUT)
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