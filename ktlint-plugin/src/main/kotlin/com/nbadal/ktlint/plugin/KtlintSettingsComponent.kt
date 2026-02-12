package com.nbadal.ktlint.plugin

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBoxWithWidePopup
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.panels.ListLayout
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.NamedColorUtil
import com.intellij.util.ui.UIUtil
import com.nbadal.ktlint.connector.KtlintConnector
import com.nbadal.ktlint.connector.KtlintVersion
import com.nbadal.ktlint.plugin.KtlintRuleEngineWrapper.KtlintVersionConfiguration
import java.awt.Cursor
import java.awt.Desktop
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import java.util.Objects
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JSeparator
import javax.swing.SwingConstants

class KtlintSettingsComponent(
    private val project: Project,
    private val ktlintProjectSettings: KtlintProjectSettings,
) {
    private val showBannerCheckBox = JBCheckBox(MessageBundle.message("showBannerWhenNotInitializedLabel"))

    val distractFreeModeRadioButton =
        JRadioButton(MessageBundle.message("distractFreeModeLabel"))
            .apply {
                addChangeListener { setFormatFieldsVisibility() }
            }

    private val manualModeRadioButton =
        JRadioButton(MessageBundle.message("manualModeLabel")).apply {
            addChangeListener { setFormatFieldsVisibility() }
        }

    private val disabledModeRadioButton =
        JRadioButton(MessageBundle.message("disabledModeLabel"))
            .apply {
                addChangeListener { setFormatFieldsVisibility() }
            }

    private val attachToIntellijFormattingCheckbox = JBCheckBox(MessageBundle.message("attachToIntellijFormattingLabel"))
    private val attachToIntellijFormattingPanel =
        FormBuilder
            .createFormBuilder()
            .addComponent(attachToIntellijFormattingCheckbox)
            .addTooltip(MessageBundle.message("attachToIntellijFormattingToolTip"))
            .panel

    val formatOnSaveCheckbox = JBCheckBox(MessageBundle.message("formatOnSaveLabel"))
    private val formatOnSavePanel =
        FormBuilder
            .createFormBuilder()
            .addComponent(formatOnSaveCheckbox)
            .addTooltip(MessageBundle.message("formatOnSaveToolTip"))
            .panel

    private val baselinePathTextFieldWithBrowseButton =
        TextFieldWithBrowseButton()
            .apply {
                val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("xml")
                addActionListener {
                    FileChooser.chooseFile(descriptor, project, null) {
                        text = it.path
                    }
                }
            }
    private var baselinePath: String?
        get() =
            baselinePathTextFieldWithBrowseButton
                .text
                .trim()
                .let { it.ifBlank { null } }
        set(value) {
            baselinePathTextFieldWithBrowseButton.text = value.orEmpty()
        }
    private val baselinePathPanel =
        FormBuilder
            .createFormBuilder()
            .addLabeledComponent(JLabel(MessageBundle.message("baselineLabel")), baselinePathTextFieldWithBrowseButton)
            .addTooltip(MessageBundle.message("baselineToolTip"))
            .panel

    private abstract class KtlintVersionComponent {
        abstract fun getPanel(): JPanel

        abstract var ktlintVersion: KtlintVersion
    }

    private class KtlintVersionComponentDefault(
        val ktlintConnector: KtlintConnector,
    ) : KtlintVersionComponent() {
        private val ktlintVersionComboBoxWithWidePopup =
            ComboBoxWithWidePopup(
                ktlintConnector
                    .supportedKtlintVersions
                    .map { it.label }
                    .toTypedArray(),
            ).apply {
                @Suppress("UsePropertyAccessSyntax")
                setMinLength(40)
                addActionListener { event ->
                    alternativeKtlintVersionUsedJLabel.apply {
                        isVisible = false
                        val selectedItem = (event.source as ComboBoxWithWidePopup<*>).selectedItem as String
                        ktlintConnector.supportedKtlintVersions
                            .firstOrNull { it.label == selectedItem }
                            ?.alternativeKtlintVersionLabel
                            ?.let { alternativeKtlintVersionLabel ->
                                isVisible = true
                                text = MessageBundle.message("usingAlternativeKtlintVersionWarning", alternativeKtlintVersionLabel)
                            }
                    }
                }
            }

        override var ktlintVersion: KtlintVersion
            get() =
                ktlintConnector.findSupportedKtlintVersionByLabel(ktlintVersionComboBoxWithWidePopup.selectedItem as String)
                    ?: KtlintVersion.DEFAULT
            set(value) {
                ktlintVersionComboBoxWithWidePopup.selectedItem = value.label
            }

        private val alternativeKtlintVersionUsedJLabel =
            JLabel("", AllIcons.General.Warning, SwingConstants.LEFT)
                .apply { isVisible = false }

        override fun getPanel(): JPanel =
            FormBuilder
                .createFormBuilder()
                .addLabeledComponent(
                    JLabel(MessageBundle.message("rulesetVersionLabel")),
                    FormBuilder
                        .createFormBuilder()
                        .addComponent(
                            JPanel(ListLayout.horizontal(horGap = 10))
                                .apply {
                                    add(ktlintVersionComboBoxWithWidePopup)
                                    add(
                                        JLabel(AllIcons.General.ContextHelp)
                                            .apply {
                                                toolTipText =
                                                    MessageBundle.message(
                                                        "rulesetVersionEditableContextHelp",
                                                        KTLINT_PLUGINS_VERSION_PROPERTY,
                                                        KTLINT_PLUGINS_PROPERTIES_FILE_NAME,
                                                    )
                                            },
                                    )
                                    add(alternativeKtlintVersionUsedJLabel)
                                },
                        ).panel,
                ).addTooltip(MessageBundle.message("rulesetVersionTooltip"))
                .panel
    }

    private class KtlintVersionComponentWithSharedPluginProperties(
        private val ktlintConnector: KtlintConnector,
        private val sharedPluginPropertyKtlintVersion: KtlintVersion,
    ) : KtlintVersionComponent() {
        override fun getPanel(): JPanel =
            FormBuilder
                .createFormBuilder()
                .addLabeledComponent(
                    JLabel(MessageBundle.message("rulesetVersionLabel")),
                    JPanel(ListLayout.horizontal(horGap = 10))
                        .apply {
                            add(JLabel(sharedPluginPropertyKtlintVersion.label))
                            add(
                                JLabel(AllIcons.General.ContextHelp)
                                    .apply {
                                        toolTipText =
                                            MessageBundle.message(
                                                "rulesetVersionNonEditableContextHelp",
                                                KTLINT_PLUGINS_VERSION_PROPERTY,
                                                KTLINT_PLUGINS_PROPERTIES_FILE_NAME,
                                            )
                                    },
                            )
                            if (ktlintConnector.supportedKtlintVersions.none { it.label == sharedPluginPropertyKtlintVersion.label }) {
                                add(
                                    JLabel(
                                        MessageBundle.message("unsupportedRulesetVersionError", ""),
                                        AllIcons.General.Error,
                                        SwingConstants.LEFT,
                                    ).apply {
                                        setForeground(NamedColorUtil.getErrorForeground())
                                    },
                                )
                            }
                            ktlintConnector
                                .supportedKtlintVersions
                                .firstOrNull { it.label == sharedPluginPropertyKtlintVersion.label }
                                ?.alternativeKtlintVersionLabel
                                ?.let { alternativeKtlintVersionLabel ->
                                    add(
                                        JLabel(
                                            MessageBundle.message("usingAlternativeKtlintVersionWarning", alternativeKtlintVersionLabel),
                                            AllIcons.General.Warning,
                                            SwingConstants.LEFT,
                                        ).apply {
                                            setForeground(NamedColorUtil.getErrorForeground())
                                        },
                                    )
                                }
                        },
                ).panel

        override var ktlintVersion: KtlintVersion
            get() = sharedPluginPropertyKtlintVersion
            set(
                @Suppress("unused") value,
            ) {
                throw UnsupportedOperationException("Can not set ktlintVersion when it is defined as shared property")
            }
    }

    private val ktlintVersionComponent =
        KtlintRuleEngineWrapper
            .instance
            .ktlintVersionConfiguration(project)
            .let { ktlintVersionConfiguration ->
                when (ktlintVersionConfiguration.location) {
                    KtlintVersionConfiguration.Location.SHARED_PLUGIN_PROPERTIES -> {
                        KtlintVersionComponentWithSharedPluginProperties(
                            ProjectWrapper.instance.ktlintConnector(project),
                            ktlintVersionConfiguration.ktlintVersion,
                        )
                    }

                    else -> {
                        KtlintVersionComponentDefault(
                            ProjectWrapper.instance.ktlintConnector(project),
                        )
                    }
                }
            }

    private val ktlintVersionPanel = ktlintVersionComponent.getPanel()

    private val externalRulesetJarPathsTextFieldWithBrowseButton =
        TextFieldWithBrowseButton()
            .apply {
                addActionListener {
                    val descriptor = FileChooserDescriptor(false, false, true, true, false, true)
                    FileChooser.chooseFiles(descriptor, project, null) { files ->
                        text = files.joinToString(", ") { it.path }
                    }
                }
            }
    private var externalRulesetJarPaths: List<String>
        get() =
            externalRulesetJarPathsTextFieldWithBrowseButton
                .text
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
        set(value) {
            externalRulesetJarPathsTextFieldWithBrowseButton.text =
                value.joinToString()
        }
    private val externalRulesetJarPathsPanel =
        FormBuilder
            .createFormBuilder()
            .addLabeledComponent(
                JLabel(MessageBundle.message("externalRulesetJarPathsLabel")),
                externalRulesetJarPathsTextFieldWithBrowseButton,
            ).addTooltip(MessageBundle.message("externalRulesetJarPathsToolTip"))
            .panel

    private val popularExternalRulesetsLabel = JLabel(MessageBundle.message("popularExternalRulesetsLabel"))
    private val jetpackComposeLabel = JLink(MessageBundle.message("jetpackComposeLabel"), jetpackComposeUri)
    private val ktlintProjectLabel = JLink(MessageBundle.message("ktlintProjectLabel"), ktlintProjectUri)
    private val ktlintPluginProjectLabel = JLink(MessageBundle.message("ktlintPluginProjectLabel"), ktlintPluginProjectUri)

    private fun ktlintSettingsPanel(): JPanel =
        FormBuilder
            .createFormBuilder()
            .addComponent(applicationSettingsPanel())
            .addComponent(projectSettingsPanel())
            .addComponent(referencesPanel())
            .panel

    private fun applicationSettingsPanel(): JPanel =
        FormBuilder
            .createFormBuilder()
            .addComponent(sectionTitle(MessageBundle.message("applicationSettingsLabel")))
            // Show banner row
            .addComponent(showBannerCheckBox)
            .addTooltip(MessageBundle.message("showBannerWhenNotInitializedTooltip"))
            .addVerticalGap(UIUtil.LARGE_VGAP)
            .panel

    private val ktlintModeOptionsPanel: JPanel =
        FormBuilder
            .createFormBuilder()
            .addVerticalGap(UIUtil.DEFAULT_VGAP)
            .setFormLeftIndent(UIUtil.DEFAULT_HGAP * 2)
            .addComponent(sectionTitle("Options"))
            .addComponent(ktlintVersionPanel)
            .addComponent(attachToIntellijFormattingPanel)
            .addComponent(formatOnSavePanel)
            .addComponent(baselinePathPanel)
            .addComponent(externalRulesetJarPathsPanel)
            .panel

    private fun projectSettingsPanel(): JPanel =
        FormBuilder
            .createFormBuilder()
            .addComponent(sectionTitle(MessageBundle.message("projectSettingsLabel")))
            .addComponent(ktlintModePanel())
            .addComponent(ktlintModeOptionsPanel)
            .addVerticalGap(UIUtil.LARGE_VGAP)
            .panel

    private fun ktlintModePanel() =
        FormBuilder
            .createFormBuilder()
            .addComponent(JLabel(MessageBundle.message("modeLabel")))
            .setFormLeftIndent(UIUtil.DEFAULT_HGAP)
            .addComponent(distractFreeModePanel())
            .addComponent(manualModePanel())
            .addComponent(disabledModePanel())
            .panel
            .also {
                // Make the modes mutual exclusive
                ButtonGroup().apply {
                    add(distractFreeModeRadioButton)
                    add(manualModeRadioButton)
                    add(disabledModeRadioButton)
                }
            }

    private fun distractFreeModePanel() =
        FormBuilder
            .createFormBuilder()
            .addComponent(distractFreeModeRadioButton)
            .addTooltip(MessageBundle.message("distractFreeModeTooltip"))
            .panel

    private fun manualModePanel() =
        FormBuilder
            .createFormBuilder()
            .addComponent(manualModeRadioButton)
            .addTooltip(MessageBundle.message("manualModeTooltip"))
            .panel

    private fun disabledModePanel() =
        FormBuilder
            .createFormBuilder()
            .addComponent(disabledModeRadioButton)
            .addTooltip(MessageBundle.message("disabledModeTooltip"))
            .panel

    private fun referencesPanel(): JPanel =
        FormBuilder
            .createFormBuilder()
            .addComponent(sectionTitle(null))
            // Popular external rulesets
            .addComponent(popularExternalRulesetsLabel)
            .addComponent(JPanel())
            .addComponentToRightColumn(jetpackComposeLabel)
            // Feedback
            .addComponent(JPanel())
            .addComponent(JLabel(MessageBundle.message("feedbackLabel")))
            .addComponent(JPanel())
            .addComponentToRightColumn(ktlintProjectLabel)
            .addTooltip(MessageBundle.message("ktlintProjectTip"))
            .addComponent(JPanel())
            .addComponentToRightColumn(ktlintPluginProjectLabel)
            .addTooltip(MessageBundle.message("ktlintPluginProjectTip"))
            .panel

    fun createComponent(): JComponent {
        val panel = ktlintSettingsPanel()

        showBannerCheckBox.isSelected = KtlintApplicationSettings.getInstance().state.showBanner

        setFormatFieldsVisibility()

        return panel
    }

    fun apply() {
        KtlintApplicationSettings.getInstance().state.showBanner = showBannerCheckBox.isSelected

        ktlintProjectSettings.ktlintMode = ktlintMode
        ktlintProjectSettings.ktlintVersionLabel = ktlintVersionComponent.ktlintVersion.label
        ktlintProjectSettings.formatOnSave = formatOnSaveCheckbox.isSelected
        ktlintProjectSettings.attachToIntellijFormat = attachToIntellijFormattingCheckbox.isSelected
        ktlintProjectSettings.externalJarPaths = externalRulesetJarPaths
        ktlintProjectSettings.baselinePath = baselinePath

        with(KtlintRuleEngineWrapper.instance) {
            reset(project)
            formatAllOpenFiles(
                project = project,
                ktlintFormatAutoCorrectHandler = KtlintFileAutocorrectHandler,
                triggeredBy = "KtlintSettingsComponent",
            )
        }
    }

    fun reset() {
        showBannerCheckBox.isSelected = KtlintApplicationSettings.getInstance().state.showBanner

        when (ktlintProjectSettings.ktlintMode) {
            KtlintMode.DISTRACT_FREE -> distractFreeModeRadioButton.isSelected = true
            KtlintMode.MANUAL -> manualModeRadioButton.isSelected = true
            KtlintMode.DISABLED -> disabledModeRadioButton.isSelected = true
            else -> Unit
        }

        if (ktlintVersionComponent is KtlintVersionComponentDefault) {
            ktlintVersionComponent.ktlintVersion = ktlintProjectSettings.ktlintVersion() ?: KtlintVersion.DEFAULT
        }
        formatOnSaveCheckbox.isSelected = ktlintProjectSettings.formatOnSave
        attachToIntellijFormattingCheckbox.isSelected = ktlintProjectSettings.attachToIntellijFormat
        baselinePath = ktlintProjectSettings.baselinePath
        externalRulesetJarPaths = ktlintProjectSettings.externalJarPaths
    }

    private val ktlintMode
        get() =
            when {
                distractFreeModeRadioButton.isSelected -> KtlintMode.DISTRACT_FREE
                manualModeRadioButton.isSelected -> KtlintMode.MANUAL
                disabledModeRadioButton.isSelected -> KtlintMode.DISABLED
                else -> KtlintMode.NOT_INITIALIZED
            }

    val isModified
        get() =
            !(
                Objects.equals(KtlintApplicationSettings.getInstance().state.showBanner, showBannerCheckBox.isSelected) &&
                    Objects.equals(ktlintProjectSettings.ktlintMode, ktlintMode) &&
                    Objects.equals(ktlintProjectSettings.ktlintVersionLabel, ktlintVersionComponent.ktlintVersion.label) &&
                    Objects.equals(ktlintProjectSettings.formatOnSave, formatOnSaveCheckbox.isSelected) &&
                    Objects.equals(ktlintProjectSettings.attachToIntellijFormat, attachToIntellijFormattingCheckbox.isSelected) &&
                    Objects.equals(ktlintProjectSettings.baselinePath, baselinePath) &&
                    Objects.equals(ktlintProjectSettings.externalJarPaths, externalRulesetJarPaths)
            )

    private fun setFormatFieldsVisibility() {
        ktlintModeOptionsPanel.isVisible = distractFreeModeRadioButton.isSelected || manualModeRadioButton.isSelected

        formatOnSavePanel.isVisible = distractFreeModeRadioButton.isSelected
        attachToIntellijFormattingPanel.isVisible = manualModeRadioButton.isSelected
    }

    private companion object {
        val jetpackComposeUri =
            URI("https://mrmans0n.github.io/compose-rules/ktlint/#using-with-ktlint-cli-or-the-ktlint-unofficial-intellij-plugin")
        val ktlintPluginProjectUri = URI("https://github.com/nbadal/ktlint-intellij-plugin/issues")
        val ktlintProjectUri = URI("https://github.com/pinterest/ktlint/issues")
    }
}

private class JLink(
    label: String,
    uri: URI,
) : JLabel("<html><a href=\"\">$label</a></html>") {
    init {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            setCursor(Cursor(Cursor.HAND_CURSOR))
            addMouseListener(
                object : MouseAdapter() {
                    @Override
                    override fun mouseClicked(e: MouseEvent?) {
                        Desktop.getDesktop().browse(uri)
                    }
                },
            )
        } else {
            isVisible = false
        }
    }
}

private fun sectionTitle(title: String?): JPanel =
    FormBuilder
        .createFormBuilder()
        .apply {
            if (title.isNullOrBlank()) {
                addComponent(JSeparator())
            } else {
                addLabeledComponent(JLabel(title), JSeparator())
            }
        }.addVerticalGap(UIUtil.DEFAULT_VGAP)
        .panel
