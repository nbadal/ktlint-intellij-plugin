package com.nbadal.ktlint

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

fun Project.config(): KtlintConfigStorage = getService(KtlintConfigStorage::class.java)

fun VirtualFile.isKotlinFile(): Boolean = extension in setOf("kt", "kts")
