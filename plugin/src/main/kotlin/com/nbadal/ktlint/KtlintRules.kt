package com.nbadal.ktlint

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

object KtlintRules {
    fun getAll(config: KtlintConfigStorage) = find(config.externalJarPaths, config.useExperimental)
        .fold(mutableListOf<String>()) { allRules, ruleSet -> allRules.apply { addAll(ruleSet.ids()) } }
        .toList()

    fun find(paths: List<String>, experimental: Boolean) = ServiceLoader
        .load(
            RuleSetProvider::class.java,
            URLClassLoader(
                externalRulesetArray(paths),
                RuleSetProvider::class.java.classLoader
            )
        )
        .associateBy {
            val key = it.get().id
            if (key == "standard") "\u0000$key" else key
        }
        .filterKeys { experimental || it != "experimental" }
        .toSortedMap()
        .map { it.value.get() }

    private fun RuleSet.ids() = rules.map { rule -> if (id == "standard") rule.id else "$id:${rule.id}" }

    private fun externalRulesetArray(paths: List<String>) = paths
        .map { it.replaceFirst(Regex("^~"), System.getProperty("user.home")) }
        .map { File(it) }
        .filter { it.exists() }
        .map { it.toURI().toURL() }
        .toTypedArray()
}