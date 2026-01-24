package com.nbadal.ktlint.plugin

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class KtlintFileEditorManagerListener : FileEditorManagerListener {
    override fun fileOpened(
        source: FileEditorManager,
        file: VirtualFile,
    ) {
        // Remove the ktlint annotator user data which was previously stored for the document. It does not work to remove this data on
        // closing the file. Removing the data enforces the ktlint annotator to run always when a new editor is opened for a file which
        // was opened (and closed) before.
        source.selectedTextEditor?.document?.removeKtlintAnnotatorUserData()

        super.fileOpened(source, file)
    }

    // The selectionChanged event is raised when clicking a different editor tab. Unfortunately when switch between application windows
    // (e.g. when multiple projects are opened in different application windows), this event is not sent. As of that this is not a
    // reliable way to update the project in the ProjectWrapper.
    // https://platform.jetbrains.com/t/fileeditormanagerlistener-does-not-trigger-selectionchanged-if-window-is-opened-on-second-screen/1199/5
    // override fun selectionChanged(event: FileEditorManagerEvent) {
    //
    // }
}
