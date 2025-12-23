package testhelper

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.utils.vfs.getDocument
import com.nbadal.ktlint.KtlintMode
import com.nbadal.ktlint.KtlintProjectSettings
import com.nbadal.ktlint.config
import com.pinterest.ktlint.ruleset.standard.KtlintRulesetVersion
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

/**
 * Most tests for the Ktlint plugin eventually invoke the ktlint rule engine. Test in the plugin should however not test the ktlint
 * functionality, but only the integration with it. So for all tests we can the exact same kotlin file.
 */
abstract class KtlintRuleEngineTestCase : BasePlatformTestCase() {
    protected val configMock =
        mockk<KtlintProjectSettings>(relaxed = true) {
            every { baselinePath } returns null
            every { ktlintMode } returns KtlintMode.DISTRACT_FREE
            every { ktlintRulesetVersion } returns KtlintRulesetVersion.DEFAULT
        }

    override fun setUp() {
        super.setUp()

        // Mock extension functions defined in Utils.kt.
        // The string is the fully qualified name of the class generated from the file.
        mockkStatic("com.nbadal.ktlint.UtilsKt")
        every { project.config() } returns configMock
    }

    override fun tearDown() {
        unmockkStatic("com.nbadal.ktlint.UtilsKt")
        super.tearDown()
    }

    /**
     * The Kotlin file is always created with some unformatted code. As the plugin tests should not test whether ktlint is working, the
     * content of the file is fixed.
     */
    fun createKotlinFile(fileName: String = "Foo.kt"): VirtualFile =
        myFixture.createFile(fileName, SOME_UNFORMATTED_KOTLIN_CODE)
            ?: throw IllegalStateException("Unable to create document for file $fileName")

    fun createFile(
        fileName: String,
        contents: String,
    ): VirtualFile =
        myFixture.createFile(fileName, contents)
            ?: throw IllegalStateException("Unable to create document for file $fileName")

    fun openFilesAsUnsavedDocuments(vararg virtualFiles: VirtualFile): Array<Document> {
        virtualFiles.map { it.name }.toTypedArray().run {
            myFixture.configureByFiles(*this)
        }
        return virtualFiles.map { it.toDocument().setUnsaved() }.toTypedArray()
    }

    private fun VirtualFile.toDocument(): Document =
        FileEditorManager
            .getInstance(myFixture.project)
            .openFiles
            .toList()
            .first { it == this }
            .getDocument()

    private fun Document.setUnsaved() =
        apply {
            WriteCommandAction.runWriteCommandAction(project) { setText(text) }
        }

    /**
     * The Kotlin file is always filled with [SOME_UNFORMATTED_KOTLIN_CODE] as content. So, it can be easily checked whether ktlint has
     * succeeded by comparing it to [SOME_FORMATTED_KOTLIN_CODE].
     */
    fun Document.isFormattedWithKtlint() = text == SOME_FORMATTED_KOTLIN_CODE

    fun Document.isSaved() = !FileDocumentManager.getInstance().isDocumentUnsaved(this)

    private companion object {
        val SOME_UNFORMATTED_KOTLIN_CODE =
            """
            |fun foo() {
            |        // Wrongly indented comment in original code, but fixed after ktlint format
            |    val x = 1
            |}
            |
            """.trimMargin()
        val SOME_FORMATTED_KOTLIN_CODE =
            """
            |fun foo() {
            |    // Wrongly indented comment in original code, but fixed after ktlint format
            |    val x = 1
            |}
            |
            """.trimMargin()
    }
}
