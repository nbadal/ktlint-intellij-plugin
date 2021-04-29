package com.nbadal.ktlint

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor

class KtlintPostFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings) = source // Stub.

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        val config = source.project.config()

        if (shouldLint(source, config)) {
            doLint(source, config, true)
        }

        return rangeToReformat
    }

    private fun shouldLint(source: PsiFile, config: KtlintConfigStorage) = when {
        // Skip if disabled
        (!config.enableKtlint || !config.lintAfterReformat) -> false
        // Skip if not in project
        (source.virtualFile == null || !ProjectFileIndex.getInstance(source.project).isInContent(source.virtualFile)) -> false
        // Skip if it isn't a kotlin file
        (source.fileType.name != "Kotlin") -> false

        else -> true
    }
}
