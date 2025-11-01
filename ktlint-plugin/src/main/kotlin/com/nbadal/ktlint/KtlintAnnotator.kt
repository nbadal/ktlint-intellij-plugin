package com.nbadal.ktlint

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity.ERROR
import com.intellij.lang.annotation.HighlightSeverity.INFORMATION
import com.intellij.lang.annotation.HighlightSeverity.WARNING
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintFeature.AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND
import com.nbadal.ktlint.KtlintFeature.DISPLAY_ALL_VIOLATIONS
import com.nbadal.ktlint.KtlintFeature.DISPLAY_PROBLEM_WITH_NUMBER_OF_VIOLATIONS_FOUND
import com.nbadal.ktlint.KtlintFeature.DISPLAY_VIOLATION_WHICH_CAN_NOT_BE_AUTOCORRECTED_AS_ERROR
import com.nbadal.ktlint.actions.ForceFormatIntention
import com.nbadal.ktlint.actions.KtlintAutocorrectIntention
import com.nbadal.ktlint.actions.KtlintRuleSuppressIntention
import com.nbadal.ktlint.actions.ShowAllKtlintViolationsIntention
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

internal class KtlintAnnotator : ExternalAnnotator<List<LintError>, List<LintError>>() {
    private val logger = KtlintLogger(this::class.qualifiedName)

    override fun collectInformation(
        psiFile: PsiFile,
        editor: Editor,
        hasErrors: Boolean,
    ): List<LintError>? =
        when {
            hasErrors -> {
                // Ignore ktlint when other errors (for example compilation errors) are found
                null
            }

            psiFile.project.isEnabled(DISPLAY_VIOLATION_WHICH_CAN_NOT_BE_AUTOCORRECTED_AS_ERROR) ||
                psiFile.project.isEnabled(DISPLAY_ALL_VIOLATIONS) ||
                psiFile.project.isEnabled(DISPLAY_PROBLEM_WITH_NUMBER_OF_VIOLATIONS_FOUND) ||
                (
                    psiFile.project.isEnabled(AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND) &&
                        KtlintApplicationSettings.getInstance().state.showBanner
                )
            -> {
                if (editor.document.ktlintAnnotatorUserData?.modificationTimestamp == editor.document.modificationStamp) {
                    // Document is unchanged since last time ktlint has run. Reuse lint errors from user data. It also has the advantage that
                    // a notification from the lint/format process in case on error is not displayed again.
                    logger.debug { "Do not run ktlint as ktlintAnnotatorUserData has not changed on document ${psiFile.virtualFile.name}" }
                    editor.document.ktlintAnnotatorUserData?.lintErrors
                } else {
                    KtlintRuleEngineWrapper
                        .instance
                        .lint(psiFile, "KtlintAnnotator")
                        .also { ktlintResult -> editor.document.setKtlintResult(ktlintResult) }
                        .lintErrors
                }
            }

            else -> {
                null
            }
        }

    override fun doAnnotate(collectedInfo: List<LintError>?): List<LintError>? =
        collectedInfo?.sortedWith(compareBy(LintError::line).thenComparingInt(LintError::col))

    override fun apply(
        psiFile: PsiFile,
        errors: List<LintError>?,
        annotationHolder: AnnotationHolder,
    ) {
        val displayAllKtlintViolations = psiFile.project.isEnabled(DISPLAY_ALL_VIOLATIONS) || psiFile.displayAllKtlintViolations
        val ignoreViolationsPredicate: (LintError) -> Boolean =
            if (psiFile.project.isEnabled(DISPLAY_VIOLATION_WHICH_CAN_NOT_BE_AUTOCORRECTED_AS_ERROR) &&
                !psiFile.project.isEnabled(DISPLAY_ALL_VIOLATIONS)
            ) {
                // By default, hide all violations which can be autocorrected unless the current editor is configured to display all
                // violations. Hiding aims to distract the developer as little as possible as those violations can be resolved by using
                // ktlint format. Showing all violations supports the developer in suppressing violations which may not be autocorrected.
                { lintError -> lintError.canBeAutoCorrected && !displayAllKtlintViolations }
            } else {
                { !displayAllKtlintViolations }
            }

        createAnnotationsPerViolation(psiFile, errors, annotationHolder, ignoreViolationsPredicate)
        if (KtlintApplicationSettings.getInstance().state.showBanner) {
            createAnnotationSummaryForIgnoredViolations(psiFile, errors, annotationHolder, ignoreViolationsPredicate)
        }
    }

    private fun createAnnotationsPerViolation(
        psiFile: PsiFile,
        errors: List<LintError>?,
        annotationHolder: AnnotationHolder,
        shouldIgnore: (LintError) -> Boolean,
    ) {
        errors
            ?.filterNot { shouldIgnore(it) }
            ?.forEach { lintError ->
                errorTextRange(psiFile, lintError)
                    ?.let { errorTextRange ->
                        if (lintError.canBeAutoCorrected) {
                            annotationHolder.createAutocorrectIntention(psiFile, lintError, errorTextRange)
                        }
                        annotationHolder.createSuppressIntention(psiFile, lintError, errorTextRange)
                    }
            }
    }

    private fun AnnotationHolder.createAutocorrectIntention(
        psiFile: PsiFile,
        lintError: LintError,
        errorTextRange: TextRange,
    ) {
        when {
            lintError.ruleId in psiFile.project.config().ruleIdsWithAutocorrectApproveHandler -> {
                // Fixing of individual lint errors is supported for this rule. No tooltip needed.
                newAnnotation(WARNING, lintError.errorMessage())
                    .range(errorTextRange)
                    .withFix(KtlintAutocorrectIntention(lintError))
                    .create()
            }

            lintError.ruleId.ruleSetId == RuleSetId.STANDARD -> {
                // Rules provided by Ktlint 1.3.0+ all support manual fixing of individual lint errors and as of that are already
                // handled by previous when-condition.
                newSilentAnnotation(WARNING)
                    .range(errorTextRange)
                    .tooltip(
                        "<i>${lintError.errorMessage()}</i><p>" +
                            "Manual fixing of individual Ktlint violations is not supported by version " +
                            "'${psiFile.project.ktlintRulesetVersion().label()}' of the ruleset. <strong>Upgrade the ruleset in the Ktlint " +
                            "Plugin Settings to at least 1.3.0.</strong>",
                    ).create()
            }

            else -> {
                // Custom rules that provide support for fixing individual violations are already handled in a when-condition above.
                newSilentAnnotation(WARNING)
                    .range(errorTextRange)
                    .tooltip(
                        "<i>${lintError.errorMessage()}</i><p>" +
                            "Manual fixing of individual Ktlint violations is not supported for rule '${lintError.ruleId.value}'. " +
                            "<strong>Upgrade to a later version of this ruleset if possible.</strong> This rule is provided via a custom" +
                            " ruleset.<strong>Contact the maintainer of the '${lintError.ruleId.ruleSetId.value}' ruleset to provide a " +
                            "compatible ruleset.</strong>",
                    ).create()
            }
        }
    }

    private fun AnnotationHolder.createSuppressIntention(
        psiFile: PsiFile,
        lintError: LintError,
        errorTextRange: TextRange,
    ) {
        val severity =
            when {
                lintError.canBeAutoCorrected -> WARNING
                psiFile.project.isEnabled(DISPLAY_VIOLATION_WHICH_CAN_NOT_BE_AUTOCORRECTED_AS_ERROR) -> ERROR
                else -> WARNING
            }
        // A violation can always be suppressed. So no tooltip is needed.
        newAnnotation(severity, lintError.errorMessage())
            .range(errorTextRange)
            .withFix(KtlintRuleSuppressIntention(lintError))
            .create()
    }

    private fun createAnnotationSummaryForIgnoredViolations(
        psiFile: PsiFile,
        errors: List<LintError>?,
        annotationHolder: AnnotationHolder,
        shouldIgnore: (LintError) -> Boolean,
    ) {
        val countCanBeAutoCorrected =
            errors
                ?.filter { shouldIgnore(it) }
                ?.count { it.canBeAutoCorrected } ?: 0
        val countCanNotBeAutoCorrected =
            errors
                ?.filter { shouldIgnore(it) }
                ?.count { !it.canBeAutoCorrected } ?: 0
        val message =
            when {
                countCanBeAutoCorrected > 0 && countCanNotBeAutoCorrected > 0 -> {
                    "Ktlint found ${countCanBeAutoCorrected + countCanNotBeAutoCorrected} lint violations of which " +
                        "$countCanBeAutoCorrected can be autocorrected"
                }

                countCanBeAutoCorrected > 0 -> {
                    "Ktlint found $countCanBeAutoCorrected violations which can be autocorrected"
                }

                countCanNotBeAutoCorrected > 0 -> {
                    "Ktlint found $countCanNotBeAutoCorrected violations that should be corrected manually"
                }

                else -> {
                    return
                }
            }

        if (psiFile.project.isEnabled(AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND)) {
            annotationHolder
                .newAnnotation(INFORMATION, message)
                .fileLevel()
                .withFix(KtlintOpenSettingsIntention())
                .withFix(KtlintOpenSettingsDoNotShowAgainIntention())
                .create()
        } else {
            annotationHolder
                .newAnnotation(WARNING, message)
                .range(TextRange(0, 0))
                .withFix(ShowAllKtlintViolationsIntention())
                .withFix(ForceFormatIntention())
                .create()
        }
    }

    private val PsiFile.displayAllKtlintViolations
        get() =
            viewProvider
                .document
                .ktlintAnnotatorUserData
                .also {
                    logger.debug { "Annotator: $it" }
                }?.displayAllKtlintViolations ?: false

    private fun LintError.errorMessage(): String = "$detail (${ruleId.value})"

    private fun errorTextRange(
        psiFile: PsiFile,
        lintError: LintError,
    ): TextRange? {
        val document = psiFile.viewProvider.document!!
        return if (document.textLength == 0) {
            // It is not possible to draw an annotation on empty file
            null
        } else {
            psiFile
                .findElementAt(lintError.offsetFromStartOf(document))
                ?.let { TextRange.from(it.textRange.startOffset, it.textLength) }
                ?: TextRange(lintError.lineStartOffset(document), lintError.getLineEndOffset(document))
        }
    }

    private fun LintError.offsetFromStartOf(document: Document) =
        with(document) {
            val lineStartOffset = lineStartOffset(this)
            (lineStartOffset + (col - 1))
                .coerceIn(lineStartOffset, textLength)
        }

    private fun LintError.lineStartOffset(document: Document) =
        with(document) {
            getLineStartOffset((line - 1).coerceIn(0, lineCount - 1))
                .coerceIn(0, textLength)
        }

    private fun LintError.getLineEndOffset(document: Document) =
        with(document) {
            getLineEndOffset((line - 1).coerceIn(0, lineCount - 1))
                .coerceIn(0, textLength)
        }
}

fun Project.resetKtlintAnnotator() {
    // Reset KtlintRuleEngine as it has cached the '.editorconfig'
    config().resetKtlintRuleEngine()

    // Remove user data from all open documents
    FileEditorManager
        .getInstance(this)
        .openFiles
        .forEach { virtualFile ->
            FileDocumentManager
                .getInstance()
                .getDocument(virtualFile)
                ?.removeKtlintAnnotatorUserData()
        }

    // Restart code analyzer so that open files are scanned again
    DaemonCodeAnalyzer.getInstance(this).restart()
}
