package com.nbadal.ktlint

import com.intellij.testFramework.utils.vfs.getDocument
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import testhelper.KtlintRuleEngineTestCase

class KtlintActionOnSaveTest : KtlintRuleEngineTestCase() {
    private val actionOnSave = KtlintActionOnSave()

    fun `test Given that format on save is enabled then format all open files when editorconfig is saved`() {
        every { project.isEnabled(KtlintFeature.FORMAT_WITH_KTLINT_ON_SAVE) } returns true
        every { configMock.formatOnSave } returns true
        val editorConfigFile = createFile(".editorconfig", "root = true")
        val javaFile = createFile("Foo.java", "// Some java code")
        val kotlinFile = createKotlinFile("Foo.kt")
        val documents = openFilesAsUnsavedDocuments(editorConfigFile, javaFile, kotlinFile)

        actionOnSave.processDocuments(project, documents)

        assertThat(editorConfigFile.getDocument().isSaved()).isTrue
        assertThat(javaFile.getDocument().isSaved()).isFalse
        assertThat(kotlinFile.getDocument().isFormattedWithKtlint()).isTrue
        assertThat(kotlinFile.getDocument().isSaved()).isTrue
    }

    fun `test Given that format on save is enabled then format only the modified kotlin file`() {
        every { project.isEnabled(KtlintFeature.FORMAT_WITH_KTLINT_ON_SAVE) } returns true
        every { configMock.formatOnSave } returns true
        val javaFile = createFile("Foo.java", "// Some java code")
        val kotlinFile = createKotlinFile("Foo.kt")
        val documents = openFilesAsUnsavedDocuments(javaFile, kotlinFile)

        actionOnSave.processDocuments(project, documents)

        assertThat(javaFile.getDocument().isSaved()).isFalse
        assertThat(kotlinFile.getDocument().isFormattedWithKtlint()).isTrue
        assertThat(kotlinFile.getDocument().isSaved()).isTrue
    }

    fun `test Given that format on save is disabled then do not format the file`() {
        every { project.isEnabled(KtlintFeature.FORMAT_WITH_KTLINT_ON_SAVE) } returns true
        every { configMock.formatOnSave } returns false
        val kotlinFile = createKotlinFile("Foo.kt")
        val documents = openFilesAsUnsavedDocuments(kotlinFile)

        actionOnSave.processDocuments(project, documents)

        assertThat(kotlinFile.getDocument().isSaved()).isFalse
    }

    fun `test Given that the format on save feature is disabled then do not format the file`() {
        every { project.isEnabled(KtlintFeature.FORMAT_WITH_KTLINT_ON_SAVE) } returns false
        every { configMock.formatOnSave } returns true
        val kotlinFile = createKotlinFile("Foo.kt")
        val documents = openFilesAsUnsavedDocuments(kotlinFile)

        actionOnSave.processDocuments(project, documents)

        assertThat(kotlinFile.getDocument().isSaved()).isFalse
    }
}
