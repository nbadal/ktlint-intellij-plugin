package com.nbadal.ktlint.plugin

import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.utils.vfs.getDocument
import com.intellij.testFramework.utils.vfs.getPsiFile
import org.assertj.core.api.Assertions.assertThat
import testhelper.KtlintRuleEngineTestCase

class KtlintPluginParseTest : KtlintRuleEngineTestCase() {
    fun `test Can parse an editorconfig file and open it in an editor`() {
        val editorConfigFile = createFile(".editorconfig", "root = true")
        openFilesAsUnsavedDocuments(editorConfigFile)

        assertThat(editorConfigFile).isNotSameAs(myFixture.file)
        assertThat(editorConfigFile.getDocument()).isSameAs(myFixture.editor.document)
        assertThat(myFixture.languageId()).isEqualTo("EditorConfig")
    }

    fun `test Can parse a kotlin file and open it in an editor`() {
        val kotlinFile = createKotlinFile("Foo.kt")
        openFilesAsUnsavedDocuments(kotlinFile)

        assertThat(kotlinFile).isNotSameAs(myFixture.file)
        assertThat(kotlinFile.getDocument()).isSameAs(myFixture.editor.document)
        assertThat(myFixture.languageId()).isEqualTo("kotlin")
    }

    fun `test Can parse a Java file and open it in an editor`() {
        val javaFile = createFile("Foo.java", "// Some java code")
        openFilesAsUnsavedDocuments(javaFile)

        assertThat(javaFile).isNotSameAs(myFixture.file)
        assertThat(javaFile.getDocument()).isSameAs(myFixture.editor.document)
        assertThat(myFixture.languageId()).isEqualTo("JAVA")
    }

    private fun CodeInsightTestFixture.languageId() =
        file.virtualFile
            .getPsiFile(project)
            .language.id
}
