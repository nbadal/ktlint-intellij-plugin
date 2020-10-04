import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Comparing
import com.intellij.ui.IdeBorderFactory
import com.nbadal.ktlint.KtlintConfigStorage
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class KtlintConfigForm(private val project: Project, private val config: KtlintConfigStorage) {

    private lateinit var mainPanel: JPanel
    private lateinit var enableKtlint: JCheckBox
    private lateinit var enableExperimental: JCheckBox
    private lateinit var treatAsErrors: JCheckBox
    private lateinit var disabledRules: JTextField
    private lateinit var editorConfigPath: TextFieldWithBrowseButton

    fun createComponent(): JComponent {
        mainPanel.border = IdeBorderFactory.createTitledBorder("Ktlint Settings")

        enableKtlint.addChangeListener {
            val enabled = enableKtlint.isSelected
            enableExperimental.isEnabled = enabled
            treatAsErrors.isEnabled = enabled
            disabledRules.isEnabled = enabled
            editorConfigPath.isEnabled = enabled
        }

        editorConfigPath.addBrowseFolderListener(
            null,
            null,
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )

        return mainPanel
    }

    fun apply() {
        config.enableKtlint = enableKtlint.isSelected
        config.useExperimental = enableExperimental.isSelected
        config.treatAsErrors = treatAsErrors.isSelected
        config.disabledRules = disabledRules.text
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        config.editorConfigPath = editorConfigPath.text
            .trim()
            .let { if (it.isNotBlank()) it else null }
    }

    fun reset() {
        enableKtlint.isSelected = config.enableKtlint
        enableExperimental.isSelected = config.useExperimental
        treatAsErrors.isSelected = config.treatAsErrors
        disabledRules.text = config.disabledRules.joinToString(", ")
        editorConfigPath.text = config.editorConfigPath ?: ""
    }

    val isModified
        get() = !(
            Comparing.equal(config.enableKtlint, enableKtlint.isSelected) &&
                Comparing.equal(config.useExperimental, enableExperimental.isSelected) &&
                Comparing.equal(config.treatAsErrors, treatAsErrors.isSelected) &&
                Comparing.equal(config.disabledRules, disabledRules.text) &&
                Comparing.equal(config.editorConfigPath, editorConfigPath.text)
            )
}
