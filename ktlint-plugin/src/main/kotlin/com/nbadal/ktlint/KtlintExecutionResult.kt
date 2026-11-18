package com.nbadal.ktlint

import com.pinterest.ktlint.rule.engine.api.LintError

/**
 * The result of executing Ktlint on a file.
 */
internal sealed class KtlintExecutionResult {
    // Code has not been linted/formatted, or result has been discarded because document has changed while snapshot was processed in
    // background
    object NotExecuted : KtlintExecutionResult()

    // Exception while linting/formatting
    object FileRelatedError : KtlintExecutionResult()

    // Result of linting
    data class LintResult(
        val lintErrors: List<LintError>,
    ) : KtlintExecutionResult()

    // Result of formatting
    data class FormatResult(
        val lintErrors: List<LintError>,
        val formattedCode: String,
        val contentChanged: Boolean,
    ) : KtlintExecutionResult()
}
