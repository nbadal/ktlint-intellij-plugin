package com.nbadal.ktlint.plugin

import com.intellij.openapi.vfs.VirtualFile

internal fun VirtualFile?.isKotlinFile() =
    this != null &&
        extension in setOf("kt", "kts") &&
        path != "/fragment.kt"
