package com.nbadal.ktlint

import com.nbadal.ktlint.actions.FormatAction
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetV2Provider
import com.pinterest.ktlint.rule.engine.core.api.RuleInstanceProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import java.io.File
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

private val logger = KtlintLogger()

/**
 * Loads the rule providers for a given external ruleset JAR. Rules from the [RuleSetId.STANDARD] will be excluded. The rules provided via
 * ktlint will be loaded separately. So technically it is possible that the custom ruleset provides the standard rules of a different ktlint
 * version than the ktlint version the user has selected in the Ktlint plugin preferences.
 */
internal fun loadRuleProvidersFromExternalJarPath(path: String): Set<RuleInstanceProvider> =
    try {
        loadAllRuleSetProvidersFromJarPath(path)
            // Ignore the STANDARD ruleset when it is also provided by the external ruleset jar
            .filterNot { it.ruleId.ruleSetId == RuleSetId.STANDARD }
            .also { logger.info { "Loaded ${it.size} rules from external ruleset jar file '$path'" } }
            .ifEmpty { throw EmptyRuleSetJarException("Custom ruleset jar file '$path' does not contain any custom ktlint rule") }
            .toSet()
    } catch (throwable: Throwable) {
        logger.error(throwable) { "Cannot load external ruleset jar '$path" }
        emptySet()
    }

class EmptyRuleSetJarException(
    message: String,
) : RuntimeException(message)

private fun loadAllRuleSetProvidersFromJarPath(path: String): Set<RuleInstanceProvider> {
    val ruleV2InstanceProviders =
        RuleSetV2Provider::class.java
            .loadAll(path)
            .flatMap {
                // Theoretically the external ruleset jar could contain multiple custom rulesets. So load all of them.
                it.getRuleProviders()
            }.toSet()
    if (ruleV2InstanceProviders.isNotEmpty()) {
        return ruleV2InstanceProviders
    }

    return RuleSetProviderV3::class.java
        .loadAll(path)
        .flatMap {
            // Theoretically the external ruleset jar could contain multiple custom rulesets. So load all of them.
            it.getRuleProviders()
        }.toSet()
}

private fun <T> Class<T>.loadAll(path: String): Set<T> {
    // See: https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#classes-from-plugin-dependencies
    val thread = Thread.currentThread()
    val prevLoader = thread.getContextClassLoader()
    try {
        val loader = FormatAction::class.java.classLoader
        thread.contextClassLoader = loader
        return try {
            ServiceLoader
                .load(this, RelocatingClassLoader(File(path).toUrlArray(), loader))
                .toSet()
        } catch (_: ServiceConfigurationError) {
            emptySet()
        }
    } finally {
        // Restore original classloader
        thread.contextClassLoader = prevLoader
    }
}

private fun File.toUrlArray() = arrayOf(toURI().toURL())
