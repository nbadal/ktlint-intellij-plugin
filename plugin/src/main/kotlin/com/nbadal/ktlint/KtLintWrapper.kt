package com.nbadal.ktlint

import com.pinterest.ktlint.core.KtLint

/**
 * Wrapper intended to allow mocking of Ktlint interactions, without invoking all its internals
 */
object KtLintWrapper {
    fun format(params: KtLint.Params) = KtLint.format(params)
    fun lint(params: KtLint.Params) = KtLint.lint(params)
    fun trimMemory() = KtLint.trimMemory()
}
