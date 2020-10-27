package com.nbadal.ktlint

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.pinterest.ktlint.core.LintError
import org.jetbrains.kotlin.lexer.KtTokens.EOL_COMMENT
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf

class KtlintLineDisableAction(private val error: LintError) : BaseIntentionAction() {
    override fun getFamilyName() = "ktlint"

    override fun getText() = "Disable '${error.ruleId}' for line"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        // Skip for unhandled errors (see notes on list)
        if (unhandledErrors.contains(error.ruleId)) return false

        // Skip if there's an existing EOL comment that isn't a ktlint-disable one
        val eolComment = file.errorEolComment()
        if (eolComment != null && eolComment.textWithError() == null) return false

        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        val factory = KtPsiFactory(project)

        file.apply {
            errorEol()?.let { eol ->
                errorEolComment()?.let { existingComment ->
                    // Add item to existing comment, if it's a disable comment.
                    existingComment.textWithError()?.let(factory::createComment)?.let(existingComment::replace) ?: {
                        // If we can't add our ID, do nothing.
                    }
                } ?: factory.createComment("// ktlint-disable ${error.ruleId}").let {
                    // Create new comment.
                    eol.parent.addBefore(factory.createWhiteSpace(" "), eol)
                    eol.parent.addBefore(it, eol)
                }
            }
        }
    }

    /** List of rules to skip + reasons. Probably incomplete */
    private val unhandledErrors = listOf(
        "indent", // Doesn't seem to be respected, even when on the same line.
        "no-trailing-spaces", // because the space is _within_ a PsiWhitespace, we can't determine its line yet.
        "no-consecutive-blank-lines", // doesn't have a 'line' associated with it.
        "final-newline", // No way to disable using comment, since it's the last character
    )

    /** @return the element specified by the error */
    private fun PsiFile.errorElement(): PsiElement? =
        viewProvider.document?.getLineStartOffset(error.line - 1)?.let { lineOffset ->
            findElementAt(lineOffset + error.col - 1)
        }

    /** @return the EOL whitespace after this error, if found */
    private fun PsiFile.errorEol(): PsiWhiteSpace? =
        errorElement()?.let { err ->
            err.nextLeaf { it is PsiWhiteSpace && it.textContains('\n') } as PsiWhiteSpace?
        }

    /** @return the EOL comment on the line with this error, if present */
    private fun PsiFile.errorEolComment(): PsiComment? =
        errorEol()?.let { eol ->
            val prev = eol.prevSibling
            return if (prev is PsiComment && prev.tokenType == EOL_COMMENT) prev else null
        }

    /** @return the text for a new ktlint-disable comment, if it already is one */
    private fun PsiComment.textWithError(): String? {
        val items = this.text.removePrefix("//").trim().split(" ")
        if (items.isEmpty() || items[0] != "ktlint-disable") return null
        return "// ${items.joinToString(" ")} ${error.ruleId}"
    }
}
