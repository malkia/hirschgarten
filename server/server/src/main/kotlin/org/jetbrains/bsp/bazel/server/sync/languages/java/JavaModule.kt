package org.jetbrains.bsp.bazel.server.sync.languages.java

import java.net.URI
import org.jetbrains.bsp.bazel.server.model.LanguageData

data class Jdk(val version: String, val javaHome: URI?)

data class JavaModule(
    val jdk: Jdk,
    val runtimeJdk: Jdk?,
    val javacOpts: List<String>,
    val jvmOps: List<String>,
    val mainOutput: URI,
    val binaryOutputs: List<URI>,
    val mainClass: String?,
    val args: List<String>,
) : LanguageData
