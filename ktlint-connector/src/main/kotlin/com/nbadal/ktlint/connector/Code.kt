package com.nbadal.ktlint.connector

import java.nio.file.Path

data class Code(
    val content: String,
    val filePath: Path?,
)
