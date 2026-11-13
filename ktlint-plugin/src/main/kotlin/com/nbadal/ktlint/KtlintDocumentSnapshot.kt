package com.nbadal.ktlint

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

/**
 * The snapshot of a document at a given moment. Ktlint is executed in a background thread on the snapshot instead of on the actual document
 * as that would block the EDT. The actual document may be changed while ktlint is running on the snapshot.
 */
internal class KtlintDocumentSnapshot(
    // Note: the document from which the snapshot is taken, may have been changed by the time that Ktlint has finished in the background.
    val document: Document,
    val psiFile: PsiFile,
    val content: String,
    val modificationStamp: Long,
) {
    val isStale get() = document.modificationStamp != modificationStamp

    companion object {
        /**
         * Must be called with read access.
         */
        fun from(psiFile: PsiFile): KtlintDocumentSnapshot? =
            psiFile
                .takeIf { it.isValid }
                ?.let {
                    // The psiFile may contain unsaved changes. Get the content via the PsiDocumentManager instead of from "psiFile.text"
                    // directly. In case the content of an active editor window is changed via a global find and replace, the document text
                    // is updated but the Psi (and PsiFile) have not yet been changed.
                    PsiDocumentManager.getInstance(it.project).getDocument(it)
                }?.let { document -> KtlintDocumentSnapshot(document, psiFile, document.text, document.modificationStamp) }
    }
}
