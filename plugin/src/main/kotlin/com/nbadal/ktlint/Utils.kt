package com.nbadal.ktlint

import com.intellij.openapi.project.Project
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.DISABLED
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.ENABLED
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.NOT_INITIALIZED

fun Project.config(): KtlintConfigStorage = getService(KtlintConfigStorage::class.java)

/**
 * Checks if ktlint is not yet initalized.
 */
fun Project.ktlintNotInitialized(): Boolean = getService(KtlintConfigStorage::class.java).ktlintMode == NOT_INITIALIZED

/**
 * Checks if ktlint is explicitly enabled for the project. If so, both lint and format should run.
 */
fun Project.ktlintEnabled(): Boolean = getService(KtlintConfigStorage::class.java).ktlintMode == ENABLED

/**
 * Checks if ktlint is explicitly disabled for the project. If so, lint and format may never run.
 */
fun Project.ktlintDisabled(): Boolean = getService(KtlintConfigStorage::class.java).ktlintMode == DISABLED
