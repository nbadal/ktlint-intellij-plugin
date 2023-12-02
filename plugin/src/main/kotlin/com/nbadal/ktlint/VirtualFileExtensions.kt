package com.nbadal.ktlint

import com.intellij.openapi.vfs.VirtualFile

fun VirtualFile?.isKotlinFile() =
    this != null &&
        extension in setOf("kt", "kts") &&
        path != "/fragment.kt"
