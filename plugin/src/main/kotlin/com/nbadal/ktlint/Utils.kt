package com.nbadal.ktlint

import com.intellij.openapi.project.Project
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.DISTRACT_FREE
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.MANUAL
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.NOT_INITIALIZED

fun Project.config(): KtlintConfigStorage = getService(KtlintConfigStorage::class.java)

/**
 * Checks if ktlint is not yet initalized.
 */
fun Project.ktlintNotInitialized(): Boolean = getService(KtlintConfigStorage::class.java).ktlintMode == NOT_INITIALIZED

/**
 * Checks if ktlint is explicitly enabled for the project. If so, both lint and format should run.
 */
fun Project.ktlintEnabled(): Boolean = getService(KtlintConfigStorage::class.java).ktlintMode == DISTRACT_FREE

fun Project.ktlintMode(): KtlintConfigStorage.KtlintMode = getService(KtlintConfigStorage::class.java).ktlintMode

/**
 * Checks if ktlint is explicitly disabled for the project. If so, lint and format may never run.
 */
fun Project.ktlintDisabled(): Boolean = getService(KtlintConfigStorage::class.java).ktlintMode == MANUAL

fun Project.isEnabled(ktlintFeature: KtlintFeature) =
    when (config().ktlintMode) {
        DISTRACT_FREE -> {
            KtlintFeatureProfile.DISTRACT_FREE.isEnabled(ktlintFeature)
        }

        MANUAL -> {
            KtlintFeatureProfile.MANUAL.isEnabled(ktlintFeature)
        }

        NOT_INITIALIZED -> {
            KtlintFeatureProfile.NOT_YET_CONFIGURED.isEnabled(ktlintFeature)
        }

        KtlintConfigStorage.KtlintMode.DISABLED -> {
            KtlintFeatureProfile.DISABLED.isEnabled(ktlintFeature)
        }
    }
