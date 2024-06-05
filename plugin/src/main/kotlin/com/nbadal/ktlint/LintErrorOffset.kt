package com.nbadal.ktlint

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.pinterest.ktlint.rule.engine.api.LintError

internal fun PsiFile.findElementAt(lintError: LintError): PsiElement? =
    with(viewProvider.document) {
        getLineStartOffset(lintError)
            ?.let { findElementAt(it) }
    }

internal fun Document.getLineStartOffset(lintError: LintError) =
    takeIf { lintError.line <= it.lineCount }
        ?.let { getLineStartOffset(lintError.line - 1) + lintError.col - 1 }

internal fun PsiFile.getLineStartOffset(lintError: LintError) = viewProvider.document.getLineStartOffset(lintError)
