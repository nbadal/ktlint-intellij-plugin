package com.nbadal.ktlint.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintConfigStorage
import com.nbadal.ktlint.doLint

class KtlintFormatAction : BaseIntentionAction(), HighPriorityAction {
    override fun getFamilyName() = "ktlint"

    override fun getText() = "Format file with ktlint"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = file != null

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val config = KtlintConfigStorage.instance(project)
        file?.let {
            doLint(it, config, true)
        }
    }
}
