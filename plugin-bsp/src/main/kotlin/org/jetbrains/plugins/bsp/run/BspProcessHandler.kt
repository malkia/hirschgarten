package org.jetbrains.plugins.bsp.run

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import java.io.OutputStream
import java.util.concurrent.CompletableFuture

class BspProcessHandler<T>(private val requestFuture: CompletableFuture<T>) : ProcessHandler() {
  override fun startNotify() {
    super.startNotify()
    var thrownError: Throwable? = null
    requestFuture.handle { _, error ->
      if (error != null) {
        notifyTextAvailable(error.toString(), ProcessOutputType.STDERR)
        notifyProcessTerminated(1)
        thrownError = error
      } else {
        notifyProcessTerminated(0)
      }
    }
    // Handles the case when the future is already completed (because, for example, checkRunCapabilities failed)
    thrownError?.let { throw it }
  }

  override fun destroyProcessImpl() {
    requestFuture.cancel(true)
    super.notifyProcessTerminated(1)
  }

  override fun detachProcessImpl() {
    notifyProcessDetached()
  }

  override fun detachIsDefault(): Boolean = false

  override fun getProcessInput(): OutputStream? = null
}
