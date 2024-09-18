package com.nbadal.ktlint

import com.intellij.openapi.project.Project
import com.pinterest.ktlint.ruleset.standard.KtlintRulesetVersion

fun Project.config(): KtlintProjectSettings = getService(KtlintProjectSettings::class.java)

fun Project.ktlintMode(): KtlintMode = config().ktlintMode

fun Project.ktlintRulesetVersion(): KtlintRulesetVersion = config().ktlintRulesetVersion

fun Project.isEnabled(ktlintFeature: KtlintFeature) = config().ktlintMode.isEnabled(ktlintFeature)
