package com.nbadal.ktlint

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class KtlintDocumentSnapshotTest {
    @Test
    fun `test Given a document which is not changed after creating the snapshot then the snapshot is not stale`() {
        val document = mockk<Document> { every { modificationStamp } returns 1L }

        val actual = KtlintDocumentSnapshot(document, mockk<PsiFile>(), "val foo = 1", modificationStamp = 1L).isStale

        assertThat(actual).isFalse()
    }

    @Test
    fun `test Given a document which is changed after creating the snapshot then the snapshot is stale`() {
        val document = mockk<Document> { every { modificationStamp } returns 2L }

        val actual = KtlintDocumentSnapshot(document, mockk<PsiFile>(), "val foo = 1", modificationStamp = 1L).isStale

        assertThat(actual).isTrue()
    }
}
