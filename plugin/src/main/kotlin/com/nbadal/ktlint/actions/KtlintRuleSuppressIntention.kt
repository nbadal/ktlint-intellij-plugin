package com.nbadal.ktlint.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.nbadal.ktlint.ktlintFormat
import com.pinterest.ktlint.rule.engine.api.LintError
import org.jetbrains.kotlin.lexer.KtTokens.EOL_COMMENT
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf

class KtlintRuleSuppressIntention(private val lintError: LintError) : BaseIntentionAction(), HighPriorityAction {
    override fun getFamilyName() = "KtLint"

    override fun getText() = "Suppress '${lintError.ruleId.value}'"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ): Boolean {
        // Skip if there's an existing EOL comment that isn't a ktlint-disable one
        val errorEolComment = psiFile.errorEolComment()
        if (errorEolComment != null && !errorEolComment.text.isKtlintDisableDirective()) {
            return false
        }

        // Skip if we can't resolve an EOL whitespace to target
        return psiFile.errorEol() != null
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ) {
        psiFile.apply {
            // At this moment it is not possible to add the 'Suppress' annotation directly into the PsiFile as it does
            // not contain the Kotlin representation of the file. As of that the KtTokens which are needed to determine
            // the correct location of the annotation can not be used.
            // As workaround, the line is suppressed with the deprecated "ktlint-disable" directive. This directive does
            // not become visible (unless an error occurs) as ktlintFormat is executed afterward. Ktlint rule
            // `ktlint-suppression' replaces the disable directive with a 'Suppress' annotation at the correct location.
            errorEol()?.let { eol ->
                val factory = KtPsiFactory(project)
                errorEolComment()
                    ?.let { existingComment ->
                        // Add item to existing comment, if it's a disable comment.
                        existingComment
                            .appendToExistingKtlintDisableDirectiveCommentOrNull()
                            ?.let(factory::createComment)
                            ?.let(existingComment::replace)
                            ?: {
                                // If we can't add our ID, do nothing.
                            }
                    }
                    ?: factory.createComment("// ktlint-disable ${lintError.ruleId.value}")
                        .let {
                            // Create new comment.
                            eol.parent.addBefore(factory.createWhiteSpace(" "), eol)
                            eol.parent.addBefore(it, eol)
                        }
            }
            ktlintFormat(psiFile, "KtlintRuleSuppressIntention")
        }
    }

    /** @return the element specified by the error */
    private fun PsiFile.findElementAtLintErrorOffset(): PsiElement? =
        viewProvider
            .document
            ?.let { doc ->
                if (lintError.line >= doc.lineCount) return null
                return findElementAt(doc.getLineStartOffset(lintError.line - 1) + lintError.col - 1)
            }

    /** @return the EOL whitespace after this error, if found */
    private fun PsiFile.errorEol(): PsiWhiteSpace? =
        findElementAtLintErrorOffset()
            ?.let { err ->
                err.nextLeaf { it is PsiWhiteSpace && it.textContains('\n') } as PsiWhiteSpace?
            }

    /** @return the EOL comment on the line with this error, if present */
    private fun PsiFile.errorEolComment(): PsiComment? =
        errorEol()?.let { eol ->
            val prev = eol.prevSibling
            return if (prev is PsiComment && prev.tokenType == EOL_COMMENT) prev else null
        }

    private fun PsiComment.appendToExistingKtlintDisableDirectiveCommentOrNull(): String? =
        this
            .text
            ?.takeIf { it.isKtlintDisableDirective() }
            ?.takeIf { !it.contains(lintError.ruleId.value) }
            ?.let { "// ${this.text} ${lintError.ruleId.value}" }

    private fun String.isKtlintDisableDirective() =
        this
            .removePrefix("//")
            .trim()
            .split(" ")
            .takeIf { it.isNotEmpty() }
            ?.let { it[0] == "ktlint-disable" }
            ?: false
}
