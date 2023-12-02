package com.nbadal.ktlint

import com.nbadal.ktlint.actions.FormatAction
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import java.net.URL
import java.net.URLClassLoader
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

// See: LoadRuleProviders.kt
internal fun List<URL>.loadRuleProviders(): Set<RuleProvider> =
    RuleSetProviderV3::class.java
        .loadFromJarFiles(this, providerId = { it.id.value })
        .flatMap { it.getRuleProviders() }
        .toSet()

// See: KtlintServiceLoader.kt
private fun <T> Class<T>.loadFromJarFiles(
    urls: List<URL>,
    providerId: (T) -> String,
): Set<T> {
    val providersFromKtlintJars = this.loadProvidersFromJars(null)
    println("Loaded ${providersFromKtlintJars.size} providers from ktlint jars")
    val providerIdsFromKtlintJars = providersFromKtlintJars.map { providerId(it) }
    val providersFromCustomJars =
        urls
            .distinct()
            .flatMap { url ->
                loadProvidersFromJars(url)
                    .also { providers -> println("Loaded ${providers.size} providers from $url") }
                    .filterNot { providerIdsFromKtlintJars.contains(providerId(it)) }
                    .filterNotNull()
            }.toSet()
    return providersFromKtlintJars
        .plus(providersFromCustomJars)
        .filterNotNull()
        .toSet()
}

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
