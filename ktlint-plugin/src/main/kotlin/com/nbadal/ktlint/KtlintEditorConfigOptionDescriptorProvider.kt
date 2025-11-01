package com.nbadal.ktlint

import com.intellij.openapi.project.Project
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType
import org.editorconfig.language.extensions.EditorConfigOptionDescriptorProvider
import org.editorconfig.language.schema.descriptors.EditorConfigDescriptor
import org.editorconfig.language.schema.descriptors.EditorConfigMutableDescriptor
import org.editorconfig.language.schema.descriptors.impl.EditorConfigConstantDescriptor
import org.editorconfig.language.schema.descriptors.impl.EditorConfigOptionDescriptor
import org.editorconfig.language.schema.descriptors.impl.EditorConfigStringDescriptor
import org.editorconfig.language.schema.descriptors.impl.EditorConfigUnionDescriptor

class KtlintEditorConfigOptionDescriptorProvider : EditorConfigOptionDescriptorProvider {
    private lateinit var ktlintRuleIds: List<RuleId>
    private lateinit var ktlintEditorConfigProperties: List<EditorConfigProperty<*>>

    override fun initialize(project: Project) {
        KtlintRuleEngineWrapper
            .instance
            .ruleProviders(project)
            .let { ruleProviders ->
                ktlintRuleIds = ruleProviders.map { it.ruleId }
                ktlintEditorConfigProperties =
                    ruleProviders
                        .map { it.createNewRuleInstance().usesEditorConfigProperties }
                        .flatten()
                        .distinct()
            }
    }

    override fun getOptionDescriptors(project: Project): List<EditorConfigOptionDescriptor> {
        val editorConfigOptionDescriptors =
            mutableListOf<EditorConfigOptionDescriptor>().apply {
                add(enableOrDisableAllRulesEditorConfigOptionDescriptor())
                add(enableOrDisableExperimentalRulesEditorConfigOptionDescriptor())
                addAll(enableOrDisableRulesetEditorConfigOptionDescriptor())
                addAll(enableOrDisableRuleEditorConfigOptionDescriptors())
                addAll(miscellaneousKtlintEditorConfigOptionDescriptor())
            }
        return editorConfigOptionDescriptors.toList()
    }

    private fun enableOrDisableAllRulesEditorConfigOptionDescriptor() =
        editorConfigOptionDescriptor(
            key = editorConfigConstantDescriptor("ktlint", "Enables or disables all rules in all ktlint/custom rule sets"),
            value = enabledDisabledPropertyValueDescriptor,
        )

    private fun enableOrDisableExperimentalRulesEditorConfigOptionDescriptor() =
        // Experimental rules live inside standard ruleset, but can be enabled/disabled with separate property
        editorConfigOptionDescriptor(
            key =
                editorConfigConstantDescriptor(
                    "ktlint_experimental",
                    "Enables or disables experimental rules in all ktlint/custom rule sets",
                ),
            value = enabledDisabledPropertyValueDescriptor,
        )

    private fun enableOrDisableRulesetEditorConfigOptionDescriptor(): List<EditorConfigOptionDescriptor> =
        ktlintRuleIds
            .map { it.ruleSetId }
            .distinct()
            .map { ktlintRuleSetId ->
                editorConfigOptionDescriptor(
                    key =
                        editorConfigConstantDescriptor(
                            "ktlint_${ktlintRuleSetId.value}",
                            "Enables or disables all rules in rule set '$ktlintRuleSetId'",
                        ),
                    value = enabledDisabledPropertyValueDescriptor,
                )
            }

    private fun enableOrDisableRuleEditorConfigOptionDescriptors(): List<EditorConfigOptionDescriptor> =
        ktlintRuleIds
            .map { ktlintRuleId ->
                editorConfigOptionDescriptor(
                    key =
                        editorConfigConstantDescriptor(ktlintRuleId.toKtlintRulePropertyName(), "Enables or disables rule '$ktlintRuleId"),
                    value = enabledDisabledPropertyValueDescriptor,
                )
            }

    private fun RuleId.toKtlintRulePropertyName(): String = "ktlint_${value.replaceFirst(":", "_")}"

    private fun miscellaneousKtlintEditorConfigOptionDescriptor(): List<EditorConfigOptionDescriptor> =
        ktlintEditorConfigProperties
            .map { ktlintEditorConfigProperty ->
                editorConfigOptionDescriptor(
                    key = ktlintEditorConfigProperty.toEditorConfigConstantDescriptorForKey(),
                    value = ktlintEditorConfigProperty.toEditorConfigDescriptorForValue(),
                )
            }

    private fun EditorConfigProperty<*>.toEditorConfigConstantDescriptorForKey(): EditorConfigConstantDescriptor =
        EditorConfigConstantDescriptor(name, type.description, null)

    private fun EditorConfigProperty<*>.toEditorConfigDescriptorForValue(): EditorConfigMutableDescriptor =
        if (type is PropertyType.LowerCasingPropertyType && type.possibleValues.isNotEmpty()) {
            EditorConfigUnionDescriptor(
                children = type.possibleValues.map { editorConfigConstantDescriptor(it) },
                documentation = null,
                deprecation = null,
            )
        } else {
            EditorConfigStringDescriptor(null, null, null)
        }

    override fun requiresFullSupport(): Boolean = true

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
}
