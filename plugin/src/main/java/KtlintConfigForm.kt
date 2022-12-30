import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.uiDesigner.core.GridConstraints
import com.nbadal.ktlint.KtlintConfigStorage
import com.nbadal.ktlint.KtlintRules
import java.awt.Desktop
import java.awt.Dimension
import java.net.URI
import java.util.Objects
import java.util.ResourceBundle
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class KtlintConfigForm(private val project: Project, private val config: KtlintConfigStorage) {

    private lateinit var mainPanel: JPanel
    lateinit var enableKtlint: JCheckBox
        private set
    private lateinit var androidMode: JCheckBox
    private lateinit var enableExperimental: JCheckBox
    private lateinit var annotateAs: JComboBox<AnnotationMode>
    private lateinit var lintAfterReformat: JCheckBox
    lateinit var formatOnSave: JCheckBox
        private set
    private lateinit var disabledRulesContainer: JPanel
    private lateinit var externalJarPaths: TextFieldWithBrowseButton
    private lateinit var baselinePath: TextFieldWithBrowseButton
    private lateinit var editorConfigPath: TextFieldWithBrowseButton
    private lateinit var githubButton: JButton

    private lateinit var disabledRules: TextFieldWithAutoCompletion<String>

    private enum class AnnotationMode(private val bundleKey: String) {
        ERROR("annotateError"),
        WARNING("annotateWarning"),
        NONE("annotateNone"),
        ;

        override fun toString(): String = ResourceBundle.getBundle("strings").getString(bundleKey)

        companion object {
            fun fromConfig(config: KtlintConfigStorage) = when {
                config.hideErrors -> NONE
                config.treatAsErrors -> ERROR
                else -> WARNING
            }
        }
    }

    fun createUIComponents() {
        // Stub.
    }

    fun createComponent(): JComponent {
        // Manually create and insert disabled rules field
        val rules = try {
            KtlintRules.findRules(config.externalJarPaths, config.useExperimental)
        } catch (ruleErr: Throwable) {
            // UI for rule issues?
            emptyList()
        }

        disabledRules = TextFieldWithAutoCompletion.create(project, rules, false, "")
        disabledRules.toolTipText = ResourceBundle.getBundle("strings").getString("disabledRulesToolTip")
        disabledRulesContainer.add(
            disabledRules,
            @Suppress("MagicNumber")
            GridConstraints(
                0, 0, 1, 1,
                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW or GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                Dimension(-1, -1), Dimension(150, -1), Dimension(-1, -1),
            ),
        )

        mainPanel.border = IdeBorderFactory.createTitledBorder("Ktlint Settings")

        // Disable fields when plugin disabled
        val fieldsToDisable = listOf(
            enableExperimental,
            androidMode,
            annotateAs,
            lintAfterReformat,
            formatOnSave,
            disabledRules,
            externalJarPaths,
            baselinePath,
            editorConfigPath,
        )
        enableKtlint.addChangeListener { fieldsToDisable.forEach { it.isEnabled = enableKtlint.isSelected } }

        AnnotationMode.values().forEach(annotateAs::addItem)

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

        editorConfigPath.addBrowseFolderListener(
            null,
            null,
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
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
        config.treatAsErrors = AnnotationMode.ERROR == annotateAs.selectedItem
        config.hideErrors = AnnotationMode.NONE == annotateAs.selectedItem
        config.lintAfterReformat = lintAfterReformat.isSelected
        config.formatOnSave = formatOnSave.isSelected
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
        config.baselinePath = baselinePath.text
            .trim()
            .let { if (it.isNotBlank()) it else null }
    }

    fun reset() {
        enableKtlint.isSelected = config.enableKtlint
        androidMode.isSelected = config.androidMode
        enableExperimental.isSelected = config.useExperimental
        annotateAs.selectedItem = AnnotationMode.fromConfig(config)
        lintAfterReformat.isSelected = config.lintAfterReformat
        formatOnSave.isSelected = config.formatOnSave
        disabledRules.text = config.disabledRules.joinToString(", ")
        externalJarPaths.text = config.externalJarPaths.joinToString(", ")
        editorConfigPath.text = config.editorConfigPath ?: ""
    }

    val isModified
        get() = !(
            Objects.equals(config.enableKtlint, enableKtlint.isSelected) &&
                Objects.equals(config.androidMode, androidMode.isSelected) &&
                Objects.equals(config.useExperimental, enableExperimental.isSelected) &&
                Objects.equals(AnnotationMode.fromConfig(config), annotateAs.selectedItem) &&
                Objects.equals(config.lintAfterReformat, lintAfterReformat.isSelected) &&
                Objects.equals(config.formatOnSave, formatOnSave.isSelected) &&
                Objects.equals(config.disabledRules, disabledRules.text) &&
                Objects.equals(config.externalJarPaths, externalJarPaths.text) &&
                Objects.equals(config.editorConfigPath, editorConfigPath.text)
            )
}
