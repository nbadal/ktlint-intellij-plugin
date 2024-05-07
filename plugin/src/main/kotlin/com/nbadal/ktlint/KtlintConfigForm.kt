package com.nbadal.ktlint

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.psi.PsiManager
import com.intellij.ui.IdeBorderFactory
import com.nbadal.ktlint.KtlintMode.DISABLED
import com.nbadal.ktlint.KtlintMode.DISTRACT_FREE
import com.nbadal.ktlint.KtlintMode.MANUAL
import com.nbadal.ktlint.KtlintMode.NOT_INITIALIZED
import com.pinterest.ktlint.ruleset.standard.KtlintRulesetVersion
import java.awt.Cursor
import java.awt.Desktop
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import java.util.Objects
import javax.swing.JCheckBox
import javax.swing.JComboBox
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
    private lateinit var manualMode: JRadioButton
    private lateinit var disabledMode: JRadioButton

    private lateinit var rulesetVersion: JComboBox<KtlintRulesetVersion>
    private lateinit var formatLabel: JLabel
    lateinit var formatOnSave: JCheckBox
        private set
    private lateinit var attachToIntellijFormat: JCheckBox
    private lateinit var externalJarPaths: TextFieldWithBrowseButton
    private lateinit var baselinePath: TextFieldWithBrowseButton
    private lateinit var popularExternalRulesetsLabel: JLabel
    private lateinit var jetpackComposeLabel: JLabel
    private lateinit var ktlintProjectLabel: JLabel
    private lateinit var ktlintPluginProjectLabel: JLabel

    fun createUIComponents() {
        // Stub.
    }

    fun createComponent(): JComponent {
        mainPanel.border = IdeBorderFactory.createTitledBorder("Ktlint Format Settings")

        setFormatFieldsVisibility()
        distractFreeMode.addChangeListener { setFormatFieldsVisibility() }
        manualMode.addChangeListener { setFormatFieldsVisibility() }

        disabledMode.addChangeListener {
            setFormatFieldsVisibility()
            val isNotDisabledMode = !disabledMode.isSelected
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
            jetpackComposeLabel.setLinkLabel(jetpackComposeUri)
            ktlintProjectLabel.setLinkLabel(ktlintProjectUri)
            ktlintPluginProjectLabel.setLinkLabel(ktlintPluginProjectUri)
        } else {
            popularExternalRulesetsLabel.isVisible = false
            jetpackComposeLabel.isVisible = false
            ktlintPluginProjectLabel.isVisible = false
        }

        return mainPanel
    }

    private fun JLabel.setLinkLabel(uri: URI) {
        setText("<html><a href=\"\">$text</a></html>")
        setCursor(Cursor(Cursor.HAND_CURSOR))
        addMouseListener(
            object : MouseAdapter() {
                @Override
                override fun mouseClicked(e: MouseEvent?) {
                    Desktop.getDesktop().browse(uri)
                }
            },
        )
    }

    fun apply() {
        ktlintConfigStorage.ktlintMode = ktlintMode
        ktlintConfigStorage.ktlintRulesetVersion = ktlintRulesetVersion
        ktlintConfigStorage.formatOnSave = formatOnSave.isSelected
        ktlintConfigStorage.attachToIntellijFormat = attachToIntellijFormat.isSelected
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

        project.resetKtlintAnnotator()

        FileEditorManager
            .getInstance(project)
            .openFiles
            .forEach { virtualFile ->
                PsiManager
                    .getInstance(project)
                    .findFile(virtualFile)
                    ?.let { psiFile ->
                        ktlintFormat(psiFile, ktlintFormatRange = KtlintFileFormatRange, triggeredBy = "KtlintActionOnSave")
                    }
            }
    }

    fun reset() {
        when (ktlintConfigStorage.ktlintMode) {
            DISTRACT_FREE -> distractFreeMode.isSelected = true
            MANUAL -> manualMode.isSelected = true
            DISABLED -> disabledMode.isSelected = true
            else -> Unit
        }
        rulesetVersion.selectedItem = ktlintConfigStorage.ktlintRulesetVersion.label
        formatOnSave.isSelected = ktlintConfigStorage.formatOnSave
        attachToIntellijFormat.isSelected = ktlintConfigStorage.attachToIntellijFormat
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

    private val ktlintRulesetVersion
        get() = KtlintRulesetVersion.findByLabelOrDefault(rulesetVersion.selectedItem as String)

    val isModified
        get() =
            !(
                Objects.equals(ktlintConfigStorage.ktlintMode, ktlintMode) &&
                    Objects.equals(ktlintConfigStorage.ktlintRulesetVersion, ktlintRulesetVersion) &&
                    Objects.equals(ktlintConfigStorage.formatOnSave, formatOnSave.isSelected) &&
                    Objects.equals(ktlintConfigStorage.attachToIntellijFormat, attachToIntellijFormat.isSelected) &&
                    Objects.equals(ktlintConfigStorage.baselinePath, baselinePath.text) &&
                    Objects.equals(ktlintConfigStorage.externalJarPaths, externalJarPaths.text)
            )

    private fun setFormatFieldsVisibility() {
        formatLabel.isVisible = distractFreeMode.isSelected || manualMode.isSelected
        formatOnSave.isVisible = distractFreeMode.isSelected
        attachToIntellijFormat.isVisible = manualMode.isSelected
    }

    private companion object {
        val jetpackComposeUri =
            URI("https://mrmans0n.github.io/compose-rules/ktlint/#using-with-ktlint-cli-or-the-ktlint-unofficial-intellij-plugin")
        val ktlintPluginProjectUri = URI("https://github.com/nbadal/ktlint-intellij-plugin/issues")
        val ktlintProjectUri = URI("https://github.com/pinterest/ktlint/issues")
    }
}
