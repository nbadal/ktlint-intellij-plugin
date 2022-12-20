package com.nbadal.ktlint

import KtlintConfigForm
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class KtlintConfig(private val project: Project) : SearchableConfigurable {

    private val form = KtlintConfigForm(project, project.config())

    val formatOnSaveCheckbox by form::formatOnSave

    val enableKtlintCheckbox by form::enableKtlint

    override fun createComponent(): JComponent = form.createComponent()

    override fun isModified() = form.isModified

    override fun apply() {
        form.apply()
        // Re-inspect:
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

    override fun reset() = form.reset()

    override fun getDisplayName() = "ktlint"

    override fun getId() = "com.nbadal.ktlint.config"
}
