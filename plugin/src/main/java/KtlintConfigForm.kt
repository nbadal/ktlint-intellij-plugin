import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.IdeBorderFactory
import com.nbadal.ktlint.KtlintConfigStorage
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.DISABLED
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.ENABLED
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.NOT_INITIALIZED
import java.awt.Desktop
import java.net.URI
import java.util.Objects
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class KtlintConfigForm(
    private val project: Project,
    private val ktlintConfigStorage: KtlintConfigStorage,
) {
    private lateinit var mainPanel: JPanel
    private var ktlintMode = NOT_INITIALIZED
    lateinit var enableKtlint: JCheckBox
        private set
    private lateinit var externalJarPaths: TextFieldWithBrowseButton
    private lateinit var baselinePath: TextFieldWithBrowseButton
    private lateinit var githubButton: JButton

    fun createUIComponents() {
        // Stub.
    }

    fun createComponent(): JComponent {
        mainPanel.border = IdeBorderFactory.createTitledBorder("Ktlint Format Settings")

        // Disable fields when plugin disabled
        val fieldsToDisable =
            listOf(
                externalJarPaths,
                baselinePath,
            )
        enableKtlint.addChangeListener { fieldsToDisable.forEach { it.isEnabled = enableKtlint.isSelected } }

        externalJarPaths.addActionListener {
            val descriptor = FileChooserDescriptor(false, false, true, true, false, true)
            FileChooser.chooseFiles(descriptor, project, null) { files ->
                externalJarPaths.text = files.joinToString(", ") { it.path }
            }
        }

        baselinePath.addBrowseFolderListener(
            null,
            null,
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor("xml"),
        )

        // If we're able to launch the browser, show the GitHub button!
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            githubButton.addActionListener {
                Desktop.getDesktop().browse(URI("https://github.com/nbadal/ktlint-intellij-plugin"))
            }
        } else {
            githubButton.isVisible = false
        }

        return mainPanel
    }

    fun apply() {
        ktlintConfigStorage.ktlintMode =
            if (enableKtlint.isSelected) {
                ENABLED
            } else {
                DISABLED
            }
        ktlintConfigStorage.externalJarPaths =
            externalJarPaths
                .text
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        ktlintConfigStorage.baselinePath =
            baselinePath
                .text
                .trim()
                .let { it.ifBlank { null } }
    }

    fun reset() {
        ktlintMode = ktlintConfigStorage.ktlintMode
        enableKtlint.isSelected = (ktlintConfigStorage.ktlintMode != DISABLED)
        baselinePath.text = ktlintConfigStorage.baselinePath.orEmpty()
        externalJarPaths.text = ktlintConfigStorage.externalJarPaths.joinToString(", ")
    }

    val isModified
        get() =
            !(
                Objects.equals(ktlintConfigStorage.ktlintMode, ktlintMode) &&
                    Objects.equals(ktlintConfigStorage.baselinePath, baselinePath.text) &&
                    Objects.equals(ktlintConfigStorage.externalJarPaths, externalJarPaths.text)
            )
}
