package com.nbadal.ktlint

import com.intellij.openapi.project.Project

fun Project.config(): KtlintConfigStorage = getService(KtlintConfigStorage::class.java)
