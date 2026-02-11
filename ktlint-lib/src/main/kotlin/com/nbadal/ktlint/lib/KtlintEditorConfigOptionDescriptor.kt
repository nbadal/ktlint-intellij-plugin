package com.nbadal.ktlint.lib

sealed class KtlintEditorConfigOptionDescriptor {
    data class KtlintEditorConfigOptionEnableOrDisableDescriptor(
        val option: String,
        val description: String,
    ) : KtlintEditorConfigOptionDescriptor()

    data class KtlintEditorConfigOptionEnumDescriptor(
        val option: String,
        val description: String,
        val values: List<String>?,
    ) : KtlintEditorConfigOptionDescriptor()
}
