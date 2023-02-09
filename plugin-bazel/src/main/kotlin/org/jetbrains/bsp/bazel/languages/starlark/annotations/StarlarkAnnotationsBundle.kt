package org.jetbrains.bsp.bazel.languages.starlark.annotations

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.StarlarkAnnotationsBundle"

object StarlarkAnnotationsBundle : DynamicBundle(BUNDLE) {
    @Nls
    fun message(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any
    ): String = messageOrDefault(key, "", *params)
}
