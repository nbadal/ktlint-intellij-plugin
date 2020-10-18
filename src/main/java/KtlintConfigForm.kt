import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.uiDesigner.core.GridConstraints
import com.nbadal.ktlint.KtlintConfigStorage
import com.nbadal.ktlint.getAllRules
import java.awt.Desktop
import java.awt.Dimension
import java.net.URI
import java.util.Objects
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class KtlintConfigForm(private val project: Project, private val config: KtlintConfigStorage) {

    private lateinit var mainPanel: JPanel
    private lateinit var enableKtlint: JCheckBox
    private lateinit var androidMode: JCheckBox
    private lateinit var enableExperimental: JCheckBox
    private lateinit var treatAsErrors: JCheckBox
    private lateinit var disabledRulesContainer: JPanel
    private lateinit var externalJarPaths: TextFieldWithBrowseButton
    private lateinit var editorConfigPath: TextFieldWithBrowseButton
    private lateinit var githubButton: JButton

    private lateinit var disabledRules: TextFieldWithAutoCompletion<String>

    fun createUIComponents() {
        // Stub.
    }

    fun createComponent(): JComponent {
        // Manually create and insert disabled rules field
        disabledRules = TextFieldWithAutoCompletion.create(project, getAllRules(config), false, "")
        disabledRulesContainer.add(
            disabledRules,
            @Suppress("MagicNumber")
            GridConstraints(
                0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW or GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                Dimension(-1, -1), Dimension(150, -1), Dimension(-1, -1)
            )
        )

        mainPanel.border = IdeBorderFactory.createTitledBorder("Ktlint Settings")

        // Disable fields when plugin disabled
        val fieldsToDisable = listOf(
            enableExperimental,
            androidMode,
            treatAsErrors,
            disabledRules,
            externalJarPaths,
            editorConfigPath
        )
        enableKtlint.addChangeListener { fieldsToDisable.forEach { it.isEnabled = enableKtlint.isSelected } }

        externalJarPaths.addActionListener {
            val descriptor = FileChooserDescriptor(false, false, true, true, false, true)
            FileChooser.chooseFiles(descriptor, project, null) { files ->
                externalJarPaths.text = files.joinToString(", ") { it.path }
            }
        }

        editorConfigPath.addBrowseFolderListener(
            null,
            null,
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )

        // If we're able to launch the browser, show the github button!
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
        config.androidMode = androidMode.isSelected
        config.useExperimental = enableExperimental.isSelected
        config.treatAsErrors = treatAsErrors.isSelected
        config.disabledRules = disabledRules.text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        config.externalJarPaths = externalJarPaths.text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        config.editorConfigPath = editorConfigPath.text
            .trim()
            .let { if (it.isNotBlank()) it else null }
    }

    fun reset() {
        enableKtlint.isSelected = config.enableKtlint
        androidMode.isSelected = config.androidMode
        enableExperimental.isSelected = config.useExperimental
        treatAsErrors.isSelected = config.treatAsErrors
        disabledRules.text = config.disabledRules.joinToString(", ")
        externalJarPaths.text = config.externalJarPaths.joinToString(", ")
        editorConfigPath.text = config.editorConfigPath ?: ""
    }

    val isModified
        get() = !(
            Objects.equals(config.enableKtlint, enableKtlint.isSelected) &&
                Objects.equals(config.androidMode, androidMode.isSelected) &&
                Objects.equals(config.useExperimental, enableExperimental.isSelected) &&
                Objects.equals(config.treatAsErrors, treatAsErrors.isSelected) &&
                Objects.equals(config.disabledRules, disabledRules.text) &&
                Objects.equals(config.externalJarPaths, externalJarPaths.text) &&
                Objects.equals(config.editorConfigPath, editorConfigPath.text)
            )
}
