package com.nbadal.ktlint

import com.nbadal.ktlint.actions.FormatAction
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import java.net.URL
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

private val logger = KtlintLogger()

/**
 * Loads the rule providers from the given list of JAR [URL]s. Rules from the [RuleSetId.STANDARD] will be excluded. The rules provided via
 * ktlint will be loaded separately. So technically it is possible that the custom ruleset provides the standard rules of a different ktlint
 * version than the ktlint version the user has selected in the Ktlint plugin preferences.
 */
internal fun List<URL>.loadCustomRuleProviders(): Set<RuleProvider> =
    RuleSetProviderV3::class.java
        .loadCustomRuleProvidersFromJarFiles(this)
        .flatMap { it.getRuleProviders() }
        .toSet()

private fun Class<RuleSetProviderV3>.loadCustomRuleProvidersFromJarFiles(urls: List<URL>): Set<RuleSetProviderV3> {
    val providersFromCustomJars =
        urls
            .distinct()
            .flatMap { url ->
                loadProvidersFromJars(url)
                    .filterNot { it.id == RuleSetId.STANDARD }
                    .also { providers -> logger.debug { "Loaded ${providers.size} custom ruleset providers from $url" } }
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
            ServiceLoader.load(this, RelocatingClassLoader(url.toArray(), loader)).toSet()
        } catch (e: ServiceConfigurationError) {
            emptySet()
        }
    } finally {
        // Restore original classloader
        thread.contextClassLoader = prevLoader
    }
}

private fun URL?.toArray() = this?.let { arrayOf(this) }.orEmpty()
