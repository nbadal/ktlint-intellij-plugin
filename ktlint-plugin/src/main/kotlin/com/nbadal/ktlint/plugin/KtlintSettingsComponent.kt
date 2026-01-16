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
import com.nbadal.ktlint.KtlintRulesetVersion
import java.awt.Cursor
import java.awt.Desktop
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import java.util.*
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

    private abstract class RulesetComponent {
        abstract fun getPanel(): JPanel

        abstract var rulesetVersion: KtlintRulesetVersion
    }

    private class KtlintPluginRulesetVersionComponent : RulesetComponent() {
        private val rulesetVersionComboBoxWithWidePopup =
            ComboBoxWithWidePopup(
                KtlintRulesetVersion.entries.map { it.label() }.toTypedArray(),
            ).apply {
                @Suppress("UsePropertyAccessSyntax")
                setMinLength(40)
                addActionListener { event ->
                    val selectedItem = (event.source as ComboBoxWithWidePopup<*>).selectedItem as String
                    KtlintRulesetVersion
                        .findByLabelOrDefault(selectedItem)
                        .alternativeRulesetVersion
                        .let { alternativeRulesetVersion ->
                            if (alternativeRulesetVersion == null) {
                                alternativeVersionUsedJLabel.apply { isVisible = false }
                            } else {
                                alternativeVersionUsedJLabel
                                    .apply {
                                        isVisible = true
                                        text =
                                            MessageBundle.message(
                                                "usingAlternativeRulesetVersionWarning",
                                                alternativeRulesetVersion.label(),
                                            )
                                    }
                            }
                        }
                }
            }

        override var rulesetVersion: KtlintRulesetVersion
            get() = KtlintRulesetVersion.findByLabelOrDefault(rulesetVersionComboBoxWithWidePopup.selectedItem as String)
            set(value) {
                rulesetVersionComboBoxWithWidePopup.selectedItem = value.label()
            }

        private val alternativeVersionUsedJLabel =
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
                                    add(rulesetVersionComboBoxWithWidePopup)
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
                                    add(alternativeVersionUsedJLabel)
                                },
                        ).panel,
                ).addTooltip(MessageBundle.message("rulesetVersionTooltip"))
                .panel
    }

    private class AllKtlintPluginsSharedRulesetVersionComponent(
        private val rulesetVersionProperty: String,
    ) : RulesetComponent() {
        override fun getPanel(): JPanel =
            FormBuilder
                .createFormBuilder()
                .addLabeledComponent(
                    JLabel(MessageBundle.message("rulesetVersionLabel")),
                    JPanel(ListLayout.horizontal(horGap = 10))
                        .apply {
                            add(JLabel(rulesetVersionProperty))
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
                            if (KtlintRulesetVersion.entries.none { it.label() == rulesetVersionProperty }) {
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
                            KtlintRulesetVersion
                                .entries
                                .firstOrNull { it.label() == rulesetVersionProperty }
                                ?.alternativeRulesetVersion
                                ?.let { alternativeRulesetVersion ->
                                    add(
                                        JLabel(
                                            MessageBundle.message(
                                                "usingAlternativeRulesetVersionWarning",
                                                alternativeRulesetVersion.label(),
                                            ),
                                            AllIcons.General.Warning,
                                            SwingConstants.LEFT,
                                        ).apply {
                                            setForeground(NamedColorUtil.getErrorForeground())
                                        },
                                    )
                                }
                        },
                ).panel

        override var rulesetVersion: KtlintRulesetVersion
            get() = KtlintRulesetVersion.findByLabelOrDefault(rulesetVersionProperty)
            set(
                @Suppress("unused") value,
            ) {
                throw UnsupportedOperationException("Can not set rulesetVersion when it is defined as shared property")
            }
    }

    private val rulesetComponent =
        KtlintRuleEngineWrapper
            .instance
            .ktlintVersion(project)
            .let { ktlintVersion ->
                when (ktlintVersion.source) {
                    KtlintRuleEngineWrapper.KtlintVersion.Source.SHARED_PLUGIN_PROPERTIES -> {
                        AllKtlintPluginsSharedRulesetVersionComponent(ktlintVersion.version)
                    }

                    else -> {
                        KtlintPluginRulesetVersionComponent()
                    }
                }
            }

    private val rulesetVersionPanel = rulesetComponent.getPanel()

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
            .addComponent(rulesetVersionPanel)
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
        ktlintProjectSettings.ktlintRulesetVersion = rulesetComponent.rulesetVersion
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

        if (rulesetComponent is KtlintPluginRulesetVersionComponent) {
            rulesetComponent.rulesetVersion = ktlintProjectSettings.ktlintRulesetVersion ?: KtlintRulesetVersion.DEFAULT
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
                    Objects.equals(ktlintProjectSettings.ktlintRulesetVersion, rulesetComponent.rulesetVersion) &&
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
