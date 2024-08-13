package org.jetbrains.plugins.bsp.performance.testing

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter

internal class StopRecordingMaxMemoryCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "stopRecordingMaxMemory"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    MemoryProfiler.stopRecordingMaxMemory()
  }
}
