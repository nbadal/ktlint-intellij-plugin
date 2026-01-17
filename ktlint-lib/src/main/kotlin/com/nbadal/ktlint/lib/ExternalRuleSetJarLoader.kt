package com.nbadal.ktlint.lib

import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import java.io.File

class ExternalRuleSetJarLoader {
    private var externalJarPaths: List<String> = emptyList()

    private lateinit var externalRuleSetJarLoaderResult: ExternalRuleSetJarLoaderResult

    fun loadRuleProviders(externalJarPaths: List<String>): ExternalRuleSetJarLoaderResult {
        if (!::externalRuleSetJarLoaderResult.isInitialized || this.externalJarPaths != externalJarPaths) {
            this.externalJarPaths = externalJarPaths
            externalRuleSetJarLoaderResult =
                this.externalJarPaths
                    .map { externalJarRuleProviders(it) }
                    .let {
                        val ruleProviders = it.flatMap { (ruleProviders, _) -> ruleProviders }.toSet()
                        val errors = it.flatMap { (_, errors) -> errors }
                        ExternalRuleSetJarLoaderResult(ruleProviders, errors)
                    }
        }
        return externalRuleSetJarLoaderResult
    }

    private fun externalJarRuleProviders(path: String) =
        try {
            listOf(File(path).toURI().toURL())
                .loadCustomRuleProviders()
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
}

data class ExternalRuleSetJarLoaderResult(
    var ruleProviders: Set<RuleProvider>,
    var errors: List<String>,
)
