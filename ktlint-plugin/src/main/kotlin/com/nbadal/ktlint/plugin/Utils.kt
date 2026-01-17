package com.nbadal.ktlint.plugin

import com.intellij.openapi.project.Project
import com.nbadal.ktlint.connector.KtlintVersion

fun Project.config(): KtlintProjectSettings = getService(KtlintProjectSettings::class.java)

fun Project.ktlintMode(): KtlintMode = config().ktlintMode

fun Project.ktlintVersion(): KtlintVersion = config().ktlintVersion ?: KtlintVersion.DEFAULT

fun Project.isEnabled(ktlintFeature: KtlintFeature) = config().ktlintMode.isEnabled(ktlintFeature)
