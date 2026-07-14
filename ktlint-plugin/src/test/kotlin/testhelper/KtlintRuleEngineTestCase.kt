package testhelper

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.utils.vfs.getDocument
import com.intellij.util.messages.MessageBusConnection
import com.nbadal.ktlint.KtlintMode
import com.nbadal.ktlint.KtlintProjectSettings
import com.nbadal.ktlint.config
import com.pinterest.ktlint.ruleset.standard.KtlintRulesetVersion
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

/**
 * Most tests for the Ktlint plugin eventually invoke the ktlint rule engine. Test in the plugin should however not test the ktlint
 * functionality, but only the integration with it. So for all tests we can the exact same kotlin file.
 */
abstract class KtlintRuleEngineTestCase : BasePlatformTestCase() {
    protected val configMock =
        mockk<KtlintProjectSettings> {
            every { formatOnSave } returns true
            every { attachToIntellijFormat } returns true
            every { baselinePath } returns null
            every { ktlintMode } returns KtlintMode.DISTRACT_FREE
            every { ktlintRulesetVersion } returns KtlintRulesetVersion.DEFAULT
            every { externalJarPaths } returns emptyList()
        }
    private val files = mutableListOf<VirtualFile>()

    private val _notifications = mutableListOf<SimpleNotification>()
    val notifications: List<SimpleNotification>
        get() = _notifications.toList()

    private lateinit var messageBusConnection: MessageBusConnection

    override fun setUp() {
        super.setUp()

        // Mock extension functions defined in Utils.kt.
        // The string is the fully qualified name of the class generated from the file.
        mockkStatic("com.nbadal.ktlint.UtilsKt")
        every { project.config() } returns configMock

        // Listen to all notifications
        messageBusConnection = project.messageBus.connect()
        messageBusConnection.subscribe(
            Notifications.TOPIC,
            object : Notifications {
                override fun notify(notification: Notification) {
                    _notifications.add(
                        // The Notification group is interesting, but it can not be checked. While running the tests, the NotificationGroups
                        // are not registered, and replaced with a common fallback group.
                        SimpleNotification(notification.type, notification.title, notification.content),
                    )
                }
            },
        )
    }

    override fun tearDown() {
        if (::messageBusConnection.isInitialized) {
            messageBusConnection.disconnect()
        }
        unmockkStatic("com.nbadal.ktlint.UtilsKt")
        super.tearDown()
    }

    /**
     * The Kotlin file is always created with some unformatted code. As the plugin tests should not test whether ktlint is working, the
     * content of the file is fixed. After creating one or more files with [createKotlinFile] or [createFile], the documents should be
     * opened in the editor with [openFilesAsUnsavedDocuments].
     */
    fun createKotlinFile(fileName: String = "Foo.kt"): VirtualFile =
        myFixture
            .createFile(fileName, SOME_UNFORMATTED_KOTLIN_CODE)
            .also { files += it }
            ?: throw IllegalStateException("Unable to create document for file $fileName")

    /**
     * Creates a file with given [contents]. After creating one or more files with [createKotlinFile] or [createFile], the documents should
     * be opened in the editor with [openFilesAsUnsavedDocuments].
     */
    fun createFile(
        fileName: String,
        contents: String,
    ): VirtualFile =
        myFixture
            .createFile(fileName, contents)
            .also { files += it }
            ?: throw IllegalStateException("Unable to create document for file $fileName")

    /**
     * Configure all files which previously were created. Note that the files are being copied during configuration. The references to the
     * copied files are returned.
     */
    fun configureFiles(): List<VirtualFile> =
        if (files.isEmpty()) {
            throw IllegalStateException("No files have been created yet.")
        } else {
            files
                .ifNotEmpty {
                    myFixture
                        .configureByFiles(*files.map { it.name }.toTypedArray())
                        .map { it.virtualFile }
                        .toList()
                        .also { files.clear() }
                }
                ?: emptyList()
        }

    /**
     * Opens the given [virtualFiles] in the editor as unsaved documents.
     */
    fun openFilesAsUnsavedDocuments(vararg virtualFiles: VirtualFile): Array<Document> {
        virtualFiles.map { it.name }.toTypedArray().run {
            myFixture.configureByFiles(*this)
        }
        return virtualFiles.map { it.toDocument().setUnsaved() }.toTypedArray()
    }

    private fun Document.setUnsaved() =
        apply {
            WriteCommandAction.runWriteCommandAction(project) { setText(text) }
        }

    private fun VirtualFile.toDocument(): Document =
        FileEditorManager
            .getInstance(myFixture.project)
            .openFiles
            .toList()
            .first { it == this }
            .getDocument()

    /**
     * The Kotlin file is always filled with [SOME_UNFORMATTED_KOTLIN_CODE] as content. So, it can be easily checked whether ktlint has
     * succeeded by comparing it to [SOME_FORMATTED_KOTLIN_CODE].
     */
    fun Document.isFormattedWithKtlint() = text == SOME_FORMATTED_KOTLIN_CODE

    fun VirtualFile.isFormattedWithKtlint() = toDocument().isFormattedWithKtlint()

    /**
     * Checks whether the document is saved (e.g. has no unsaved changes).
     */
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

data class SimpleNotification(
    val type: NotificationType,
    val title: String,
    val message: String,
)
