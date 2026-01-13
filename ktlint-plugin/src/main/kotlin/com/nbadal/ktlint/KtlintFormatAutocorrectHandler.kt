package com.nbadal.ktlint

import com.nbadal.ktlint.connector.LintError

internal sealed interface KtlintFormatAutocorrectHandler

/**
 * Autocorrect all lint violations in the file.
 */
internal data object KtlintFileAutocorrectHandler : KtlintFormatAutocorrectHandler

/**
 * Autocorrect all lint violations in a given block between [startOffset] and [endOffsetInclusive] in the file.
 */
internal data class KtlintBlockAutocorrectHandler(
    val startOffset: Int,
    val endOffsetInclusive: Int,
) : KtlintFormatAutocorrectHandler {
    fun isRangeContainingOffset(offset: Int?) = offset in startOffset..endOffsetInclusive
}

/**
 * Autocorrect given [lintError] only.
 */
internal data class KtlintViolationAutocorrectHandler(
    val lintError: LintError,
) : KtlintFormatAutocorrectHandler
