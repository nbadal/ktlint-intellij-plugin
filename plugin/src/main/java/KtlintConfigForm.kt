import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.IdeBorderFactory
import com.nbadal.ktlint.KtlintConfigStorage
import java.awt.Desktop
import java.net.URI
import java.util.Objects
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class KtlintConfigForm(private val project: Project, private val config: KtlintConfigStorage) {
    private lateinit var mainPanel: JPanel
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
        config.enableKtlint = enableKtlint.isSelected
        config.externalJarPaths =
            externalJarPaths.text
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        config.baselinePath =
            baselinePath.text
                .trim()
                .let { it.ifBlank { null } }
    }

    fun reset() {
        enableKtlint.isSelected = config.enableKtlint
        externalJarPaths.text = config.externalJarPaths.joinToString(", ")
    }

    val isModified
        get() =
            !(
                Objects.equals(config.enableKtlint, enableKtlint.isSelected) &&
                    Objects.equals(config.externalJarPaths, externalJarPaths.text)
            )
}
