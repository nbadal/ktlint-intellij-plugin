package com.nbadal.ktlint

import com.intellij.openapi.project.Project

fun Project.config(): KtlintConfigStorage = getService(KtlintConfigStorage::class.java)

fun Project.ktlintMode(): KtlintMode = config().ktlintMode

fun Project.isEnabled(ktlintFeature: KtlintFeature) = config().ktlintMode.isEnabled(ktlintFeature)
