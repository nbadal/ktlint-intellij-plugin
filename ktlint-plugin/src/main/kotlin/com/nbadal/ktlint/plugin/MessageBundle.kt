package com.nbadal.ktlint.plugin

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val BUNDLE = "messages.KtlintSettings"

internal object MessageBundle {
    private val INSTANCE = DynamicBundle(MessageBundle::class.java, BUNDLE)

    @Nls
    fun message(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any,
    ): String = INSTANCE.getMessage(key, *params)

    @Nls
    fun lazyMessage(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any,
    ): Supplier<String> = INSTANCE.getLazyMessage(key, *params)
}
