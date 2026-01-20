package com.nbadal.ktlint.lib

import com.nbadal.ktlint.connector.AutocorrectDecision
import com.nbadal.ktlint.connector.BaselineError
import com.nbadal.ktlint.connector.Code
import com.nbadal.ktlint.connector.KtlintConnector
import com.nbadal.ktlint.connector.KtlintEditorConfigOptionDescriptor
import com.nbadal.ktlint.connector.KtlintEditorConfigOptionDescriptor.KtlintEditorConfigOptionEnableOrDisableDescriptor
import com.nbadal.ktlint.connector.KtlintEditorConfigOptionDescriptor.KtlintEditorConfigOptionEnumDescriptor
import com.nbadal.ktlint.connector.KtlintVersion
import com.nbadal.ktlint.connector.LintError
import com.nbadal.ktlint.connector.RuleId
import com.nbadal.ktlint.connector.SuppressionAtOffset
import com.pinterest.ktlint.cli.reporter.baseline.BaselineErrorHandling
import com.pinterest.ktlint.cli.reporter.baseline.BaselineLoaderException
import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.rule.engine.api.KtLintParseException
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.KtLintRuleException
import com.pinterest.ktlint.rule.engine.api.KtlintSuppressionAtOffset
import com.pinterest.ktlint.rule.engine.api.insertSuppression
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import org.ec4j.core.model.PropertyType.LowerCasingPropertyType

class KtlintConnectImpl : KtlintConnector {
    private val externalRuleSetJarLoader = ExternalRuleSetJarLoader()
    private var externalRuleSetJarRuleProviders = emptySet<RuleProvider>()

    private val standardRuleSetLoader = StandardRuleSetLoader()
    private var standardRuleProviders = emptySet<RuleProvider>()

    private lateinit var ktlintRuleEngine: KtLintRuleEngine

    private val _supportedKtlintVersions =
        KtlintRulesetVersion.entries.map { KtlintVersion(it.label(), it.alternativeRulesetVersion?.label()) }

    override fun loadExternalRulesetJars(externalJarPaths: List<String>) =
        externalRuleSetJarLoader
            .loadRuleProviders(externalJarPaths)
            .let { (ruleProviders, errors) ->
                if (ruleProviders != externalRuleSetJarRuleProviders) {
                    externalRuleSetJarRuleProviders = ruleProviders
                    resetKtlintRuleEngine()
                }
                errors
            }

    override fun loadRulesets(ktlintVersion: KtlintVersion) {
        standardRuleSetLoader
            .loadRuleProviders(KtlintRulesetVersion.findByLabelOrDefault(ktlintVersion.label))
            .takeIf { ruleProviders -> ruleProviders != standardRuleProviders }
            ?.let { ruleProviders ->
                this.standardRuleProviders = ruleProviders
                resetKtlintRuleEngine()
            }
    }

    private fun resetKtlintRuleEngine() =
        standardRuleProviders
            .plus(externalRuleSetJarRuleProviders)
            .takeUnless { it.isEmpty() }
            ?.let { ruleProviders ->
                ktlintRuleEngine =
                    KtLintRuleEngine(
                        editorConfigOverride = EMPTY_EDITOR_CONFIG_OVERRIDE,
                        ruleProviders = ruleProviders,
                    )
            }

    override fun lint(
        code: Code,
        callback: (LintError) -> Unit,
    ) {
        try {
            ktlintRuleEngine.lint(code.toKtlintCoreCode())
        } catch (ktlintParseException: KtLintParseException) {
            throw KtlintConnector.ParseException(
                line = ktlintParseException.line,
                col = ktlintParseException.col,
                message = ktlintParseException.message,
            )
        } catch (ktlintRuleException: KtLintRuleException) {
            throw KtlintConnector.RuleException(
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
            throw KtlintConnector.ParseException(
                line = ktlintParseException.line,
                col = ktlintParseException.col,
                message = ktlintParseException.message,
            )
        } catch (ktlintRuleException: KtLintRuleException) {
            throw KtlintConnector.RuleException(
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
        ktlintRuleEngine.trimMemory()
    }

    override fun insertSuppression(
        code: Code,
        suppression: SuppressionAtOffset,
    ): String = ktlintRuleEngine.insertSuppression(code.toKtlintCoreCode(), suppression.toKtlintCoreSuppression())

    private fun SuppressionAtOffset.toKtlintCoreSuppression() = KtlintSuppressionAtOffset(line, col, ruleId.toKtlintCoreRuleId())

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
                .filter { (_, errors) -> errors.any { it.status == KtlintCliError.Status.BASELINE_IGNORED } }
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
            throw KtlintConnector.BaselineLoadingException(
                // The exception message produced by ktlint already contains sufficient context of the error, but it is
                // missing the baseline path
                e.message ?: "Exception while loading baseline file '$baselinePath'",
                e,
            )
        }

    override fun supportedKtlintVersions(): List<KtlintVersion> = _supportedKtlintVersions

    override fun findSupportedKtlintVersionByLabel(label: String?) = _supportedKtlintVersions.firstOrNull { it.label == label }
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
