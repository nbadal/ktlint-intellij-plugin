import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.IdeBorderFactory
import com.nbadal.ktlint.KtlintConfigStorage
import com.nbadal.ktlint.KtlintMode.DISABLED
import com.nbadal.ktlint.KtlintMode.DISTRACT_FREE
import com.nbadal.ktlint.KtlintMode.MANUAL
import com.nbadal.ktlint.KtlintMode.NOT_INITIALIZED
import com.nbadal.ktlint.resetKtlintAnnotator
import java.awt.Desktop
import java.net.URI
import java.util.Objects
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton

class KtlintConfigForm(
    private val project: Project,
    private val ktlintConfigStorage: KtlintConfigStorage,
) {
    private lateinit var mainPanel: JPanel
    lateinit var distractFreeMode: JRadioButton
        private set
    lateinit var manualMode: JRadioButton
        private set
    lateinit var disabledMode: JRadioButton
        private set
    private lateinit var formatLabel: JLabel
        private set
    lateinit var formatOnSave: JCheckBox
        private set
    private lateinit var externalJarPaths: TextFieldWithBrowseButton
    private lateinit var baselinePath: TextFieldWithBrowseButton
    private lateinit var githubButton: JButton

    fun createUIComponents() {
        // Stub.
    }

    fun createComponent(): JComponent {
        mainPanel.border = IdeBorderFactory.createTitledBorder("Ktlint Format Settings")

        formatLabel.isVisible = distractFreeMode.isSelected
        formatOnSave.isVisible = distractFreeMode.isSelected
        distractFreeMode.addChangeListener {
            formatLabel.isVisible = distractFreeMode.isSelected
            formatOnSave.isVisible = distractFreeMode.isSelected
        }

        disabledMode.addChangeListener {
            val isNotDisabledMode = !disabledMode.isSelected
            formatOnSave.isEnabled = isNotDisabledMode
            externalJarPaths.isEnabled = isNotDisabledMode
            baselinePath.isEnabled = isNotDisabledMode
        }

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
        project.resetKtlintAnnotator()

        ktlintConfigStorage.ktlintMode = ktlintMode
        ktlintConfigStorage.formatOnSave = formatOnSave.isSelected
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
        when (ktlintConfigStorage.ktlintMode) {
            DISTRACT_FREE -> distractFreeMode.isSelected = true
            MANUAL -> manualMode.isSelected = true
            DISABLED -> disabledMode.isSelected = true
            else -> Unit
        }
        formatOnSave.isSelected = ktlintConfigStorage.formatOnSave
        baselinePath.text = ktlintConfigStorage.baselinePath.orEmpty()
        externalJarPaths.text = ktlintConfigStorage.externalJarPaths.joinToString(", ")
    }

    private val ktlintMode
        get() =
            when {
                distractFreeMode.isSelected -> DISTRACT_FREE
                manualMode.isSelected -> MANUAL
                disabledMode.isSelected -> DISABLED
                else -> NOT_INITIALIZED
            }

    val isModified
        get() =
            !(
                Objects.equals(ktlintConfigStorage.ktlintMode, ktlintMode) &&
                    Objects.equals(ktlintConfigStorage.formatOnSave, formatOnSave.isSelected) &&
                    Objects.equals(ktlintConfigStorage.baselinePath, baselinePath.text) &&
                    Objects.equals(ktlintConfigStorage.externalJarPaths, externalJarPaths.text)
            )
}
