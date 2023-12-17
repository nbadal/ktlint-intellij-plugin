package com.nbadal.ktlint

import com.intellij.openapi.project.Project
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.DISTRACT_FREE
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.MANUAL
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.NOT_INITIALIZED

fun Project.config(): KtlintConfigStorage = getService(KtlintConfigStorage::class.java)

fun Project.ktlintMode(): KtlintConfigStorage.KtlintMode = config().ktlintMode

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
