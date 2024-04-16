package com.nbadal.ktlint

internal sealed interface KtlintFormatRange {
    fun intRange(): IntRange
}

internal data object KtlintFileFormatRange : KtlintFormatRange {
    override fun intRange(): IntRange = IntRange(0, Int.MAX_VALUE)
}

internal data class KtlintBlockFormatRange(
    val startOffset: Int,
    val endOffsetInclusive: Int,
) : KtlintFormatRange {
    override fun intRange(): IntRange = IntRange(startOffset, endOffsetInclusive)
}
