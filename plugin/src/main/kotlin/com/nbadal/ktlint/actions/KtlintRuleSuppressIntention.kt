package com.nbadal.ktlint.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.nextLeaf
import com.intellij.psi.util.prevLeaf
import com.nbadal.ktlint.ktlintFormat
import com.pinterest.ktlint.rule.engine.api.LintError

class KtlintRuleSuppressIntention(private val lintError: LintError) : BaseIntentionAction(), HighPriorityAction {
    override fun getFamilyName() = "KtLint"

    override fun getText() = "Suppress '${lintError.ruleId.value}'"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ): Boolean {
        // Skip if there's an existing EOL comment that isn't a ktlint-disable one
        psiFile
            .newlineWhitespaceAfterErrorOffset()
            ?.precedingPsiCommentOrNull()
            ?.let { psiComment ->
                if (!psiComment.text.isKtlintDisableDirective()) {
                    return false
                }
            }

        // Skip if we can't resolve an EOL whitespace to target
        return psiFile.newlineWhitespaceAfterErrorOffset() != null
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ) {
        // At this moment it is not possible to add the 'Suppress' annotation directly into the PsiFile as it does
        // not contain the Kotlin representation of the file. As of that the KtTokens which are needed to determine
        // the correct location of the annotation can not be used.
        // As workaround, the line is suppressed with the deprecated "ktlint-disable" directive. This directive does
        // not become visible (unless an error occurs) as ktlintFormat is executed afterward. Ktlint rule
        // `ktlint-suppression' replaces the disable directive with a 'Suppress' annotation at the correct location.
        psiFile.apply {
            newlineWhitespaceAfterErrorOffset()
                ?.let { whitespaceAfterErrorOffset ->
                    whitespaceAfterErrorOffset
                        .precedingPsiCommentOrNull()
                        ?.takeIf { it.text.isKtlintDisableDirective() }
                        ?.takeUnless { it.text.contains(lintError.ruleId.value) }
                        ?.let { existingKtlintDisableDirective ->
                            // Add rule to existing ktlint-disable directive
                            psiFile
                                .createPsiComment("${existingKtlintDisableDirective.text} ${lintError.ruleId.value}")
                                .let(existingKtlintDisableDirective::replace)
                        }
                        ?: psiFile
                            .createPsiComment("// ktlint-disable ${lintError.ruleId.value}")
                            .let {
                                whitespaceAfterErrorOffset.parent.addBefore(it, whitespaceAfterErrorOffset)
                            }
                    println("After adding ktlint-disable-directive:\n${psiFile.text}")
                }
            ktlintFormat(psiFile, "KtlintRuleSuppressIntention")
        }
    }

    private fun PsiFile.createPsiComment(comment: String) =
        PsiFileFactory
            .getInstance(project)
            .createFileFromText(language, comment)
            .firstChild
            .nextSibling
            .nextSibling
            .firstChild
            .firstChild
            .let { it as PsiComment }

    /** @return the element specified by the error */
    private fun PsiFile.findElementAtLintErrorOffset(): PsiElement? =
        viewProvider
            .document
            ?.let { doc ->
                if (lintError.line >= doc.lineCount) return null
                return findElementAt(doc.getLineStartOffset(lintError.line - 1) + lintError.col - 1)
            }

    private fun PsiFile.newlineWhitespaceAfterErrorOffset(): PsiWhiteSpace? =
        findElementAtLintErrorOffset()
            ?.nextLeaf { it is PsiWhiteSpace && it.textContains('\n') } as PsiWhiteSpace?

    private fun PsiWhiteSpace.precedingPsiCommentOrNull(): PsiComment? = prevLeaf() as? PsiComment?

    private fun String.isKtlintDisableDirective() =
        this
            .removePrefix("//")
            .trim()
            .split(" ")
            .takeIf { it.isNotEmpty() }
            ?.let { it[0] == "ktlint-disable" }
            ?: false
}
