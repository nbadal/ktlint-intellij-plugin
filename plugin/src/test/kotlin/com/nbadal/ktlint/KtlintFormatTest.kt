package com.nbadal.ktlint

import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class KtlintFormatTest {
    private val config = KtlintConfigStorage()

    @BeforeEach
    internal fun setUp() {
//        mockkObject(KtlintRules)
//        every { KtlintRules.findRuleProviders(any(), any()) } returns StandardRuleSetProvider().getRuleProviders()
//
//        mockkConstructor(KtLintRuleEngine::class)
//        every { anyConstructed<KtLintRuleEngine>().trimMemory() } answers { /* stub */ }
    }

    @Test
    internal fun `parse exception should skip linting`() {
//        every { anyConstructed<KtLintRuleEngine>().lint(any(), any(), any()) } throws KtLintParseException(0, 0, "test")
//
//        val result = doLint(mockFile(), config, false)
//
//        assertEquals(emptyList<LintError>(), result.uncorrectedErrors)
//        assertEquals(emptyList<LintError>(), result.correctedErrors)
//        assertEquals(emptyList<LintError>(), result.ignoredErrors)
    }

    private fun mockFile() =
        mockk<PsiFile>(relaxed = true).apply {
            every { viewProvider } returns
                mockk<FileViewProvider>().apply {
                    every { document } returns null
                }
        }
}
