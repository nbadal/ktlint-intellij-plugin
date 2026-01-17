package com.nbadal.ktlint.plugin

import com.intellij.openapi.project.Project
import com.nbadal.ktlint.connector.KtlintVersion
import com.nbadal.ktlint.lib.KtlintRulesetVersion

fun Project.config(): KtlintProjectSettings = getService(KtlintProjectSettings::class.java)

fun Project.ktlintMode(): KtlintMode = config().ktlintMode

@Deprecated("refactor")
fun Project.ktlintRulesetVersion(): KtlintRulesetVersion = config().ktlintVersion?.toKtlintRulesetVersion() ?: KtlintRulesetVersion.DEFAULT

fun Project.ktlintVersion(): KtlintVersion = config().ktlintVersion ?: KtlintRulesetVersion.DEFAULT.toKtlintVersion()

fun Project.isEnabled(ktlintFeature: KtlintFeature) = config().ktlintMode.isEnabled(ktlintFeature)

@Deprecated("refactor")
fun KtlintRulesetVersion.toKtlintVersion() = KtlintVersion(this.label())

@Deprecated("refactor")
fun KtlintVersion.toKtlintRulesetVersion() = KtlintRulesetVersion.findByLabelOrDefault(value)
