package com.nbadal.ktlint.plugin

import com.intellij.openapi.project.Project
import com.nbadal.ktlint.connector.KtlintEditorConfigOptionDescriptor.KtlintEditorConfigOptionEnableOrDisableDescriptor
import com.nbadal.ktlint.connector.KtlintEditorConfigOptionDescriptor.KtlintEditorConfigOptionEnumDescriptor
import org.editorconfig.language.extensions.EditorConfigOptionDescriptorProvider
import org.editorconfig.language.schema.descriptors.EditorConfigDescriptor
import org.editorconfig.language.schema.descriptors.EditorConfigMutableDescriptor
import org.editorconfig.language.schema.descriptors.impl.EditorConfigConstantDescriptor
import org.editorconfig.language.schema.descriptors.impl.EditorConfigOptionDescriptor
import org.editorconfig.language.schema.descriptors.impl.EditorConfigStringDescriptor
import org.editorconfig.language.schema.descriptors.impl.EditorConfigUnionDescriptor

class KtlintEditorConfigOptionDescriptorProvider : EditorConfigOptionDescriptorProvider {
    override fun getOptionDescriptors(project: Project): List<EditorConfigOptionDescriptor> =
        KtlintRuleEngineWrapper
            .instance
            .getEditorConfigOptionDescriptors(project)
            .map {
                when (it) {
                    is KtlintEditorConfigOptionEnableOrDisableDescriptor -> it.toEnableOrDisableEditorConfigOptionDescriptor()
                    is KtlintEditorConfigOptionEnumDescriptor -> it.toEnumEditorConfigOptionDescriptor()
                }
            }.toList()

    private fun KtlintEditorConfigOptionEnableOrDisableDescriptor.toEnableOrDisableEditorConfigOptionDescriptor():
        EditorConfigOptionDescriptor =
        editorConfigOptionDescriptor(
            key = editorConfigConstantDescriptor(text = option, documentation = description),
            value = enabledDisabledPropertyValueDescriptor,
        )

    private fun KtlintEditorConfigOptionEnumDescriptor.toEnumEditorConfigOptionDescriptor(): EditorConfigOptionDescriptor =
        editorConfigOptionDescriptor(
            key = toEditorConfigConstantDescriptorForKey(),
            value = toEditorConfigDescriptorForValue(),
        )

    private fun KtlintEditorConfigOptionEnumDescriptor.toEditorConfigConstantDescriptorForKey(): EditorConfigConstantDescriptor =
        EditorConfigConstantDescriptor(option, description, null)

    private fun KtlintEditorConfigOptionEnumDescriptor.toEditorConfigDescriptorForValue(): EditorConfigMutableDescriptor =
        if (values == null) {
            EditorConfigStringDescriptor(null, null, null)
        } else {
            EditorConfigUnionDescriptor(
                children = (values as List<String>).map { editorConfigConstantDescriptor(it) },
                documentation = null,
                deprecation = null,
            )
        }

    private val enabledDisabledPropertyValueDescriptor =
        EditorConfigUnionDescriptor(
            children =
                listOf(
                    editorConfigConstantDescriptor("enabled"),
                    editorConfigConstantDescriptor("disabled"),
                ),
            documentation = null,
            deprecation = null,
        )

    private fun editorConfigOptionDescriptor(
        key: EditorConfigDescriptor,
        value: EditorConfigDescriptor,
    ) = EditorConfigOptionDescriptor(key = key, value = value, documentation = null, deprecation = null)

    private fun editorConfigConstantDescriptor(
        text: String,
        documentation: String? = null,
    ): EditorConfigConstantDescriptor = EditorConfigConstantDescriptor(text, documentation, null)

    override fun requiresFullSupport(): Boolean = true
}
