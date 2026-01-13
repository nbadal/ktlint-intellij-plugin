package com.nbadal.ktlint

import com.nbadal.ktlint.connector.AutocorrectDecision
import com.nbadal.ktlint.connector.BaselineError
import com.nbadal.ktlint.connector.Code
import com.nbadal.ktlint.connector.KtlintEditorConfigOptionDescriptor
import com.nbadal.ktlint.connector.KtlintEditorConfigOptionDescriptor.KtlintEditorConfigOptionEnableOrDisableDescriptor
import com.nbadal.ktlint.connector.KtlintEditorConfigOptionDescriptor.KtlintEditorConfigOptionEnumDescriptor
import com.nbadal.ktlint.connector.KtlintRuleEngineExecutor
import com.nbadal.ktlint.connector.LintError
import com.nbadal.ktlint.connector.RuleId
import com.nbadal.ktlint.connector.SuppressionAtOffset
import com.pinterest.ktlint.cli.reporter.baseline.BaselineErrorHandling
import com.pinterest.ktlint.cli.reporter.baseline.BaselineLoaderException
import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError.Status.BASELINE_IGNORED
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintParseException
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.KtLintRuleException
import com.pinterest.ktlint.rule.engine.api.insertSuppression
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType.LowerCasingPropertyType

open class KtlintRuleEngineProvider {
    /**
     * The set of ruleset providers that are loaded into the KtLintRuleEngine
     */
    private lateinit var ruleSetProviders: RuleSetProviders

    private lateinit var _ktlintRuleEngineExecutor: KtlintRuleEngineExecutor

    val ktlintRuleEngineExecutor: KtlintRuleEngineExecutor
        get() = _ktlintRuleEngineExecutor

    fun configure(
        ktlintRulesetVersion: KtlintRulesetVersion,
        externalJarPaths: List<String>,
    ) {
        if (!::ruleSetProviders.isInitialized ||
            ruleSetProviders.ktlintRulesetVersion != ktlintRulesetVersion ||
            ruleSetProviders.externalJarPaths != externalJarPaths
        ) {
            ktlintLibLogger.info("Configure KtlintRuleEngineWrapper $ktlintRulesetVersion, $externalJarPaths")
            ruleSetProviders = RuleSetProviders(ktlintRulesetVersion, externalJarPaths)
            _ktlintRuleEngineExecutor =
                object : KtlintRuleEngineExecutor {
                    val ktlintRuleEngine =
                        KtLintRuleEngine(
                            editorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
                            ruleProviders = ruleSetProviders.ruleProviders,
                        )

                    override fun lint(
                        code: Code,
                        callback: (LintError) -> Unit,
                    ) {
                        try {
                            ktlintRuleEngine.lint(code.toKtlintCoreCode())
                        } catch (ktlintParseException: KtLintParseException) {
                            throw KtlintRuleEngineExecutor.ParseException(
                                line = ktlintParseException.line,
                                col = ktlintParseException.col,
                                message = ktlintParseException.message,
                            )
                        } catch (ktlintRuleException: KtLintRuleException) {
                            throw KtlintRuleEngineExecutor.RuleException(
                                line = ktlintRuleException.line,
                                col = ktlintRuleException.col,
                                ruleId = ktlintRuleException.ruleId,
                                message = ktlintRuleException.message,
                                cause = ktlintRuleException.cause,
                            )
                        }
                    }

                    override fun format(
                        code: Code,
                        rerunAfterAutocorrect: Boolean,
                        defaultAutocorrect: Boolean,
                        callback: (LintError) -> AutocorrectDecision,
                    ): String =
                        try {
                            ktlintRuleEngine.format(
                                code.toKtlintCoreCode(),
                                rerunAfterAutocorrect,
                                defaultAutocorrect,
                            ) { ktlintCoreLintError ->
                                callback(ktlintCoreLintError.toLintError()).toKtlintCoreAutocorrectDecision()
                            }
                        } catch (ktlintParseException: KtLintParseException) {
                            throw KtlintRuleEngineExecutor.ParseException(
                                line = ktlintParseException.line,
                                col = ktlintParseException.col,
                                message = ktlintParseException.message,
                            )
                        } catch (ktlintRuleException: KtLintRuleException) {
                            throw KtlintRuleEngineExecutor.RuleException(
                                line = ktlintRuleException.line,
                                col = ktlintRuleException.col,
                                ruleId = ktlintRuleException.ruleId,
                                message = ktlintRuleException.message,
                                cause = ktlintRuleException.cause,
                            )
                        }

                    private fun Code.toKtlintCoreCode() =
                        com.pinterest.ktlint.rule.engine.api.Code
                            .fromSnippetWithPath(content, filePath)

                    private fun com.pinterest.ktlint.rule.engine.api.LintError.toLintError() =
                        LintError(
                            line = line,
                            col = col,
                            ruleId = RuleId(ruleId.value),
                            detail = detail,
                            canBeAutoCorrected = canBeAutoCorrected,
                        )

                    private fun LintError.toKtlintCoreLintError() =
                        com.pinterest.ktlint.rule.engine.api.LintError(
                            line = line,
                            col = col,
                            ruleId = ruleId.toKtlintCoreRuleId(),
                            detail = detail,
                            canBeAutoCorrected = canBeAutoCorrected,
                        )

                    private fun RuleId.toKtlintCoreRuleId() =
                        com.pinterest.ktlint.rule.engine.core.api
                            .RuleId("$value:${ruleSetId.value}")

                    private fun AutocorrectDecision.toKtlintCoreAutocorrectDecision() =
                        com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
                            .valueOf(name)

                    override fun trimMemory() {
                        TODO("Not yet implemented")
                    }

                    override fun insertSuppression(
                        code: Code,
                        suppression: SuppressionAtOffset,
                    ): String = ktlintRuleEngine.insertSuppression(code.toKtlintCoreCode(), suppression.toKtlintCoreSuppression())

                    private fun SuppressionAtOffset.toKtlintCoreSuppression() =
                        com.pinterest.ktlint.rule.engine.api
                            .KtlintSuppressionAtOffset(line, col, ruleId.toKtlintCoreRuleId())

                    override fun getEditorConfigOptionDescriptors(): List<KtlintEditorConfigOptionDescriptor> =
                        EditorConfigOptionDescriptorsProvider(ktlintRuleEngine.ruleProviders)
                            .getEditorConfigOptionDescriptors()

                    override fun ruleIdsWithAutocorrectApproveHandler(): Set<RuleId> =
                        ktlintRuleEngine
                            .ruleProviders
                            .map { it.createNewRuleInstance() }
                            .filter { it is RuleAutocorrectApproveHandler }
                            .map { RuleId(it.ruleId.value) }
                            .toSet()

                    // Reconsider whether to remove the Baseline functionality from Ktlint Core entirely. In many cases it not seems to work
                    // properly. When creating the baseline, all errors from that run are stored. On next format run, other errors may pop
                    // up after the errors of the first run were suppressed. But, those errors can not be appended to the already existing
                    // baseline.
                    // Also, before running ktlint format, the IDEA formatting also has run. This may already have changed the layout of the
                    // file, resulting in changing offsets, as of which the ignored errors are no longer matched, and as of that not
                    // suppressed.
                    override fun loadBaselineErrorsToIgnore(baselinePath: String): List<BaselineError> =
                        try {
                            loadBaseline(baselinePath, BaselineErrorHandling.EXCEPTION)
                                .lintErrorsPerFile
                                .filter { (_, errors) -> errors.any { it.status == BASELINE_IGNORED } }
                                .flatMap { (filePath, errors) ->
                                    errors.map {
                                        BaselineError(
                                            filePath = filePath,
                                            line = it.line,
                                            col = it.col,
                                            ruleId = it.ruleId,
                                        )
                                    }
                                }
                        } catch (e: BaselineLoaderException) {
                            throw KtlintRuleEngineExecutor.BaselineLoadingException(
                                // The exception message produced by ktlint already contains sufficient context of the error, but it is
                                // missing the baseline path
                                e.message ?: "Exception while loading baseline file '$baselinePath'",
                                e,
                            )
                        }
                }
        }
    }

    fun errorLoadingExternalRulesetJar(): String? = ruleSetProviders.errorLoadingExternalRulesetJar
}

private class EditorConfigOptionDescriptorsProvider(
    ruleProviders: Set<RuleProvider>,
) {
    private val ktlintRuleIds: List<RuleId> = ruleProviders.map { RuleId(it.ruleId.value) }
    private val ktlintEditorConfigProperties: List<EditorConfigProperty<*>> =
        ruleProviders
            .map { it.createNewRuleInstance().usesEditorConfigProperties }
            .flatten()
            .distinct()

    fun getEditorConfigOptionDescriptors(): List<KtlintEditorConfigOptionDescriptor> =
        listOf(
            enableOrDisableAllRulesEditorConfigOptionDescriptor(),
            enableOrDisableExperimentalRulesEditorConfigOptionDescriptor(),
            enableOrDisableRulesetEditorConfigOptionDescriptor(),
            enableOrDisableRuleEditorConfigOptionDescriptors(),
            miscellaneousKtlintEditorConfigOptionDescriptor(),
        ).flatten()

    private fun enableOrDisableAllRulesEditorConfigOptionDescriptor() =
        listOf(KtlintEditorConfigOptionEnableOrDisableDescriptor("ktlint", "Enables or disables all rules in all ktlint/custom rule sets"))

    private fun enableOrDisableExperimentalRulesEditorConfigOptionDescriptor() =
        listOf(
            KtlintEditorConfigOptionEnableOrDisableDescriptor(
                "ktlint_experimental",
                "Enables or disables experimental rules in all ktlint/custom rule sets",
            ),
        )

    private fun enableOrDisableRulesetEditorConfigOptionDescriptor() =
        ktlintRuleIds
            .map { it.ruleSetId }
            .distinct()
            .map { ktlintRuleSetId ->
                KtlintEditorConfigOptionEnableOrDisableDescriptor(
                    option = "ktlint_${ktlintRuleSetId.value}",
                    description = "Enables or disables all rules in rule set '${ktlintRuleSetId.value}'",
                )
            }

    private fun enableOrDisableRuleEditorConfigOptionDescriptors() =
        ktlintRuleIds
            .map { ktlintRuleId ->
                KtlintEditorConfigOptionEnableOrDisableDescriptor(
                    option = ktlintRuleId.toKtlintRulePropertyName(),
                    description = "Enables or disables rule '${ktlintRuleId.value}",
                )
            }

    private fun RuleId.toKtlintRulePropertyName(): String = "ktlint_${value.replaceFirst(":", "_")}"

    private fun miscellaneousKtlintEditorConfigOptionDescriptor() =
        ktlintEditorConfigProperties
            .map { ktlintEditorConfigProperty ->
                KtlintEditorConfigOptionEnumDescriptor(
                    option = ktlintEditorConfigProperty.name,
                    description = ktlintEditorConfigProperty.type.description,
                    values =
                        ktlintEditorConfigProperty
                            .type
                            .takeIf { it is LowerCasingPropertyType }
                            ?.possibleValues
                            ?.toList(),
                )
            }
}
