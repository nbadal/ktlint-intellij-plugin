package com.nbadal.ktlint

import com.intellij.ide.actionsOnSave.ActionOnSaveBackedByOwnConfigurable
import com.intellij.ide.actionsOnSave.ActionOnSaveComment
import com.intellij.ide.actionsOnSave.ActionOnSaveContext
import com.intellij.ide.actionsOnSave.ActionOnSaveInfo
import com.intellij.ide.actionsOnSave.ActionOnSaveInfoProvider

private const val ACTION_ON_SAVE_NAME = "Format with ktlint"
private const val KTLINT_FORMAT_CONFIGURABLE_ID = "preferences.ktlint-plugin"

class KtlintActionOnSaveInfoProvider : ActionOnSaveInfoProvider() {
    override fun getActionOnSaveInfos(context: ActionOnSaveContext): List<ActionOnSaveInfo> = listOf(KtlintOnSaveActionInfo(context))

    override fun getSearchableOptions(): Collection<String> {
        return listOf(ACTION_ON_SAVE_NAME)
    }
}

private class KtlintOnSaveActionInfo(
    actionOnSaveContext: ActionOnSaveContext,
) : ActionOnSaveBackedByOwnConfigurable<KtlintConfig>(
        actionOnSaveContext,
        KTLINT_FORMAT_CONFIGURABLE_ID,
        KtlintConfig::class.java,
    ) {
    override fun getActionOnSaveName() = ACTION_ON_SAVE_NAME

    override fun getCommentAccordingToStoredState() = getComment(project.ktlintMode() == KtlintConfigStorage.KtlintMode.DISTRACT_FREE)

    override fun getCommentAccordingToUiState(configurable: KtlintConfig) = getComment(configurable.distractFreeMode.isSelected)

    private fun getComment(ktlintEnabled: Boolean): ActionOnSaveComment? {
        if (!ktlintEnabled) {
            val message = "Distract free mode of Ktlint plugin is not enabled"
            // no need to show warning if save action is disabled
            return if (isActionOnSaveEnabled) ActionOnSaveComment.warning(message) else ActionOnSaveComment.info(message)
        }

        return null
    }

    override fun isActionOnSaveEnabledAccordingToStoredState() = project.config().formatOnSave

    override fun isActionOnSaveEnabledAccordingToUiState(ktlintConfig: KtlintConfig) = ktlintConfig.formatOnSaveCheckbox.isSelected

    override fun setActionOnSaveEnabled(
        configurable: KtlintConfig,
        enabled: Boolean,
    ) {
        configurable.formatOnSaveCheckbox.isSelected = enabled
    }

    override fun getActionLinks() = listOf(createGoToPageInSettingsLink(KTLINT_FORMAT_CONFIGURABLE_ID))
}
