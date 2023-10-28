package com.nbadal.ktlint

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor

class KtlintPostFormatProcessor : PostFormatProcessor {
    override fun processElement(
        source: PsiElement,
        settings: CodeStyleSettings,
    ) = source // Stub.

    override fun processText(
        psiFile: PsiFile,
        rangeToReformat: TextRange,
        settings: CodeStyleSettings,
    ): TextRange {
        ktlintFormat(psiFile, "KtlintPostFormatProcessor")
        return rangeToReformat
    }
}
