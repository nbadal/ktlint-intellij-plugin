import com.intellij.openapi.util.Comparing
import com.intellij.ui.IdeBorderFactory
import com.nbadal.ktlint.KtlintConfigStorage
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class KtlintConfigForm(private val config: KtlintConfigStorage) {

    private lateinit var mainPanel: JPanel
    private lateinit var enableKtlint: JCheckBox
    private lateinit var enableExperimental: JCheckBox
    private lateinit var treatAsErrors: JCheckBox

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
    }

    fun reset() {
        enableKtlint.isSelected = config.enableKtlint
        enableExperimental.isSelected = config.useExperimental
        treatAsErrors.isSelected = config.treatAsErrors
    }

    val isModified
        get() = !(
            Comparing.equal(config.enableKtlint, enableKtlint.isSelected) &&
                Comparing.equal(config.useExperimental, enableExperimental.isSelected) &&
                Comparing.equal(config.treatAsErrors, treatAsErrors.isSelected)
            )
}
