package com.nbadal.ktlint.plugin

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class KtlintConfig(
    private val project: Project,
) : SearchableConfigurable {
    private val ktlintSettingsComponent = KtlintSettingsComponent(project, project.config())

    val distractFreeMode by ktlintSettingsComponent::distractFreeModeRadioButton

    val formatOnSaveCheckbox by ktlintSettingsComponent::formatOnSaveCheckbox

    override fun createComponent(): JComponent = ktlintSettingsComponent.createComponent()

    override fun isModified() = ktlintSettingsComponent.isModified

    override fun apply() {
        ktlintSettingsComponent.apply()
        // Re-inspect:
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

    override fun reset() = ktlintSettingsComponent.reset()

    override fun getDisplayName() = "KtLint"

    override fun getId() = "com.nbadal.ktlint.config"
}
