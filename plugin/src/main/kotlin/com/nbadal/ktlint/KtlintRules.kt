package com.nbadal.ktlint

import com.pinterest.ktlint.core.RuleSetProviderV2
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

object KtlintRules {
    private fun find(paths: List<String>, experimental: Boolean) = ServiceLoader
        .load(
            RuleSetProviderV2::class.java,
            URLClassLoader(
                externalRulesetArray(paths),
                RuleSetProviderV2::class.java.classLoader,
            ),
        )
        .filterNot { it.id == "experimental" && !experimental }
        .toSet()

    fun findRules(path: List<String>, experimental: Boolean) = find(path, experimental).map { it.id }

    fun findRuleProviders(path: List<String>, experimental: Boolean) =
        find(path, experimental).flatMap { it.getRuleProviders() }.toSet()

    private fun externalRulesetArray(paths: List<String>) = paths
        .map { it.replaceFirst(Regex("^~"), System.getProperty("user.home")) }
        .map { File(it) }
        .filter { it.exists() }
        .map { it.toURI().toURL() }
        .toTypedArray()
}
