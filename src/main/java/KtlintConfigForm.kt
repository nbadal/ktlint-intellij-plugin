import com.intellij.openapi.util.Comparing
import com.intellij.ui.IdeBorderFactory
import com.nbadal.ktlint.KtlintConfigStorage
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class KtlintConfigForm(private val config: KtlintConfigStorage) {

    private lateinit var mainPanel: JPanel
    private lateinit var enableKtlint: JCheckBox
    private lateinit var enableExperimental: JCheckBox
    private lateinit var treatAsErrors: JCheckBox
    private lateinit var disabledRules: JTextField

    fun createComponent(): JComponent {
        mainPanel.border = IdeBorderFactory.createTitledBorder("Ktlint Settings")

        enableKtlint.addChangeListener {
            val enabled = enableKtlint.isSelected
            enableExperimental.isEnabled = enabled
            treatAsErrors.isEnabled = enabled
        }

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
    }

    fun reset() {
        enableKtlint.isSelected = config.enableKtlint
        enableExperimental.isSelected = config.useExperimental
        treatAsErrors.isSelected = config.treatAsErrors
        disabledRules.text = config.disabledRules.joinToString(", ")
    }

    val isModified
        get() = !(
            Comparing.equal(config.enableKtlint, enableKtlint.isSelected) &&
                Comparing.equal(config.useExperimental, enableExperimental.isSelected) &&
                Comparing.equal(config.treatAsErrors, treatAsErrors.isSelected) &&
                Comparing.equal(config.disabledRules, disabledRules.text)
            )
}
