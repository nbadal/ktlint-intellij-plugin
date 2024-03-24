package com.nbadal.ktlint

import com.nbadal.ktlint.actions.FormatAction
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import java.net.URL
import java.net.URLClassLoader
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

private val logger = KtlintLogger("com.nbdal.ktlint.KtlintLoadRuleProviders")

// See: LoadRuleProviders.kt
internal fun List<URL>.loadRuleProviders(): Set<RuleProvider> =
    RuleSetProviderV3::class.java
        .loadFromJarFiles(this)
        .flatMap { it.getRuleProviders() }
        .toSet()

// See: KtlintServiceLoader.kt
private fun <T> Class<T>.loadFromJarFiles(urls: List<URL>): Set<T> {
    val providersFromCustomJars =
        urls
            .distinct()
            .flatMap { url ->
                loadProvidersFromJars(url)
                    .also { providers -> logger.debug { "Loaded ${providers.size} custom ruleset providers from $url" } }
                    .filterNotNull()
                    .ifEmpty { throw EmptyRuleSetJarException("Custom rule set '$url' does not contain a custom ktlint rule set provider") }
            }.toSet()
    return providersFromCustomJars.toSet()
}

class EmptyRuleSetJarException(
    message: String,
) : RuntimeException(message)

private fun <T> Class<T>.loadProvidersFromJars(url: URL?): Set<T> {
    // See: https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#classes-from-plugin-dependencies
    val thread = Thread.currentThread()
    val prevLoader = thread.getContextClassLoader()
    try {
        val loader = FormatAction::class.java.classLoader
        thread.contextClassLoader = loader
        return try {
            ServiceLoader.load(this, URLClassLoader(url.toArray(), loader)).toSet()
        } catch (e: ServiceConfigurationError) {
            emptySet()
        }
    } finally {
        // Restore original classloader
        thread.contextClassLoader = prevLoader
    }
}

private fun URL?.toArray() = this?.let { arrayOf(this) }.orEmpty()
