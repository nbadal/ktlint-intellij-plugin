package com.nbadal.ktlint

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
}
