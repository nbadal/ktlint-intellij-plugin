package com.nbadal.ktlint.lib

import java.nio.file.Path

data class Code(
    val content: String,
    val filePath: Path?,
)
