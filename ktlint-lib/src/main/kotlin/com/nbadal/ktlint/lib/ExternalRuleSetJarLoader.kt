package com.nbadal.ktlint.lib

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

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
            loadAllRuleSetProvidersFromJarPath(path)
                // Ignore the STANDARD ruleset when it is also provided by the external ruleset jar
                .filterNot { it.id == RuleSetId.STANDARD }
                .flatMap { ruleSetProviderV3 ->
                    // Theoretically the external ruleset jar could contain multiple custom rulesets. So load all of them.
                    ruleSetProviderV3
                        .getRuleProviders()
                        .also {
                            ktlintLibLogger.info {
                                "Loaded ${it.size} rules from ruleset with id '${ruleSetProviderV3.id.value}' from external ruleset jar file '$path'"
                            }
                        }
                }.ifEmpty { throw EmptyRuleSetJarException("Custom ruleset jar file '$path' does not contain any custom ktlint rule") }
                .let { ExternalRuleSetJarLoaderResult(it.toSet(), emptyList()) }
        } catch (throwable: Throwable) {
            ktlintLibLogger.error(throwable) { "Cannot load external ruleset jar '$path" }
            ExternalRuleSetJarLoaderResult(
                emptySet(),
                listOf(
                    "An error occurred while reading external ruleset file '$path'. No ktlint ruleset can be loaded from this file.",
                ),
            )
        }

    private fun loadAllRuleSetProvidersFromJarPath(path: String): Set<RuleSetProviderV3> =
        with(RuleSetProviderV3::class.java) {
            // See: https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#classes-from-plugin-dependencies
            val thread = Thread.currentThread()
            val prevLoader = thread.getContextClassLoader()
            try {
                val loader = KtlintConnector::class.java.classLoader
                thread.contextClassLoader = loader
                val url = File(path).toUrlArray()
                // The urlClassLoaderFactory delegates the construction of a RelocatingClassLoader to the context of the "ktlint-plugin"
                // module, as this class should use the Intellij IDEA version of [org.jetbrains.org.objectweb.asm.commons.ClassRemapper],
                // and not the version provided via KtLint.
                ServiceLoader.load(this, urlClassloaderFactory(url, loader)).toSet()
            } catch (e: ServiceConfigurationError) {
                ktlintLibLogger.warn(e) { "Could not load rule providers from '$path'" }
                emptySet()
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
