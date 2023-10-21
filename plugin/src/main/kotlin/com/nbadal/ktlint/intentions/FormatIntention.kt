package com.nbadal.ktlint.intentions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.config
import com.nbadal.ktlint.doLint

class FormatIntention : BaseIntentionAction(), HighPriorityAction {
    override fun getFamilyName() = "ktlint"

    override fun getText() = "Format file with ktlint"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = file != null &&
        project.config().enableKtlint

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        file?.let {
            doLint(it, project.config(), true)
        }
    }
}
