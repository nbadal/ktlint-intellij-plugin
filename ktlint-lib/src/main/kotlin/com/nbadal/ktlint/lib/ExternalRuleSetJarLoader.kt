package com.nbadal.ktlint.lib

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import kotlin.collections.orEmpty

private val logger = KtlintLibLogger()

class ExternalRuleSetJarLoader(
    val urlClassloaderFactory: (Array<URL>, ClassLoader) -> URLClassLoader,
) {
    private var externalJarPaths: List<String> = emptyList()

    private lateinit var externalRuleSetJarLoaderResult: ExternalRuleSetJarLoaderResult

    fun loadRuleProviders(externalJarPaths: List<String>): ExternalRuleSetJarLoaderResult {
        if (!::externalRuleSetJarLoaderResult.isInitialized || this.externalJarPaths != externalJarPaths) {
            this.externalJarPaths = externalJarPaths
            externalRuleSetJarLoaderResult =
                this.externalJarPaths
                    .distinct()
                    .map { loadRuleProvidersFromExternalJarPath(it) }
                    .let {
                        val ruleProviders = it.flatMap { (ruleProviders, _) -> ruleProviders }.toSet()
                        val errors = it.flatMap { (_, errors) -> errors }
                        ExternalRuleSetJarLoaderResult(ruleProviders, errors)
                    }
        }
        return externalRuleSetJarLoaderResult
    }

    private fun loadRuleProvidersFromExternalJarPath(path: String) =
        try {
            loadRuleSetProviderFromExternalJarPath(path)
                .getRuleProviders()
                .also { ktlintLibLogger.info { "Loaded ${it.size} rules from custom rule provider $path" } }
                .let { ExternalRuleSetJarLoaderResult(it, emptyList()) }
        } catch (throwable: Throwable) {
            ktlintLibLogger.error(throwable) { "Cannot load external ruleset jar '$path" }
            ExternalRuleSetJarLoaderResult(
                emptySet(),
                listOf(
                    "An error occurred while reading external ruleset file '$path'. No ktlint ruleset can be loaded from this file.",
                ),
            )
        }

    private fun loadRuleSetProviderFromExternalJarPath(path: String): RuleSetProviderV3 =
        loadProvidersFromJarUrl(path)
            ?.takeUnless { it.id == RuleSetId.STANDARD }
            ?: throw EmptyRuleSetJarException("Custom rule set '$path' does not contain a custom ktlint rule set provider")

    private fun loadProvidersFromJarUrl(path: String): RuleSetProviderV3? =
        with(RuleSetProviderV3::class.java) {
            // See: https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#classes-from-plugin-dependencies
            val thread = Thread.currentThread()
            val prevLoader = thread.getContextClassLoader()
            try {
                val loader = KtlintConnector::class.java.classLoader
                thread.contextClassLoader = loader
                val url = File(path).toUrlArray()
                ServiceLoader
                    .load(this, urlClassloaderFactory(url, loader)) // RelocatingClassLoader(url, loader)
                    .singleOrNull()
                    ?.takeUnless { it.id == RuleSetId.STANDARD }
                    ?: throw EmptyRuleSetJarException("Custom rule set '$path' does not contain a custom ktlint rule set provider")
            } catch (e: ServiceConfigurationError) {
                logger.warn(e) { "Could not load rule providers from '$path'" }
                throw EmptyRuleSetJarException("Jar '$path' does not contain a ktlint rule set provider")
            } finally {
                // Restore original classloader
                thread.contextClassLoader = prevLoader
            }
        }

    private fun File.toUrlArray() = arrayOf(toURI().toURL())
}

private class EmptyRuleSetJarException(
    message: String,
) : RuntimeException(message)

data class ExternalRuleSetJarLoaderResult(
    var ruleProviders: Set<RuleProvider>,
    var errors: List<String>,
)
