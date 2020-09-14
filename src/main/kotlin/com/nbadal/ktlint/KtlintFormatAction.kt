package com.nbadal.ktlint

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class KtlintFormatAction : BaseIntentionAction() {
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
