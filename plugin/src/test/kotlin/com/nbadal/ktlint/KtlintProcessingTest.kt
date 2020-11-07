package com.nbadal.ktlint

import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.ParseException
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class KtlintProcessingTest {

    val config = KtlintConfigStorage()

    @BeforeEach
    internal fun setUp() {
        mockkObject(KtLint)
    }

    @Test
    internal fun `parse exception should skip linting`() {
        every { KtLint.lint(any()) } throws ParseException(0, 0, "test")

        val result = doLint(mockFile(), config, false)

        assertEquals(emptyList<LintError>(), result.uncorrectedErrors)
        assertEquals(emptyList<LintError>(), result.correctedErrors)
    }

    private fun mockFile() = mockk<PsiFile>(relaxed = true).apply {
        every { viewProvider } returns mockk<FileViewProvider>().apply {
            every { document } returns null
        }
    }
}
