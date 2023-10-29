package com.nbadal.ktlint

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.ui.LightColors

class KtlintEditorNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>() {
    private val key: Key<EditorNotificationPanel> = Key.create("KtlintEditorNotificationProvider")

    override fun getKey(): Key<EditorNotificationPanel> = key

    override fun createNotificationPanel(
        file: VirtualFile,
        fileEditor: FileEditor,
        project: Project,
    ): EditorNotificationPanel? =
        if (project.ktlintNotInitialized()) {
            val panel = EditorNotificationPanel(fileEditor, LightColors.YELLOW)
            panel.text = "Ktlint is not yet configured for this project"
            panel.createActionLabel("Enable") {
                project.config().ktlintMode = KtlintConfigStorage.KtlintMode.ENABLED
                DaemonCodeAnalyzer.getInstance(project).restart()
                panel.isVisible = false
            }
            panel.createActionLabel("Disable") {
                project.config().ktlintMode = KtlintConfigStorage.KtlintMode.DISABLED
                DaemonCodeAnalyzer.getInstance(project).restart()
                panel.isVisible = false
            }
            panel.createActionLabel("Settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, KtlintConfig::class.java)
                panel.isVisible = false
            }
            panel
        } else {
            null
        }
}
