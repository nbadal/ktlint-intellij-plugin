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
        if (psiFile.project.isEnabled(KtlintFeature.POST_FORMAT_WITH_KTLINT) ||
            (psiFile.project.config().attachToIntellijFormat && psiFile.project.ktlintMode() == KtlintMode.MANUAL)
        ) {
            KtlintRuleEngineWrapper
                .instance
                .format(
                    psiFile,
                    ktlintFormatAutoCorrectHandler =
                        KtlintBlockAutocorrectHandler(
                            rangeToReformat.startOffset,
                            rangeToReformat.endOffset + 1,
                        ),
                    triggeredBy = "KtlintPostFormatProcessor",
                    forceFormat = true,
                )
        }
        return rangeToReformat
    }
}
