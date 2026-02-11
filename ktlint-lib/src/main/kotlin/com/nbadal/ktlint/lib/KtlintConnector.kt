package com.nbadal.ktlint.lib

import com.nbadal.ktlint.lib.KtlintEditorConfigOptionDescriptor.KtlintEditorConfigOptionEnableOrDisableDescriptor
import com.nbadal.ktlint.lib.KtlintEditorConfigOptionDescriptor.KtlintEditorConfigOptionEnumDescriptor
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
import java.net.URL
import java.net.URLClassLoader

class KtlintConnector(
    urlClassloaderFactory: (Array<URL>, ClassLoader) -> URLClassLoader,
) {
    private val externalRuleSetJarLoader = ExternalRuleSetJarLoader(urlClassloaderFactory)
    private var externalRuleSetJarRuleProviders = emptySet<RuleProvider>()

    private val standardRuleSetLoader = StandardRuleSetLoader()
    private var standardRuleProviders = emptySet<RuleProvider>()

    private lateinit var ktlintRuleEngine: KtLintRuleEngine

    private lateinit var _ruleIdsWithAutocorrectApproveHandler: Set<RuleId>
    val ruleIdsWithAutocorrectApproveHandler: Set<RuleId>
        get() = _ruleIdsWithAutocorrectApproveHandler

    fun loadExternalRulesetJars(externalJarPaths: List<String>) =
        externalRuleSetJarLoader
            .loadRuleProviders(externalJarPaths)
            .let { (ruleProviders, errors) ->
                if (ruleProviders != externalRuleSetJarRuleProviders) {
                    externalRuleSetJarRuleProviders = ruleProviders
                    resetKtlintRuleEngine()
                }
                errors
            }

    fun loadRulesets(ktlintVersion: KtlintVersion) {
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
                _ruleIdsWithAutocorrectApproveHandler =
                    ktlintRuleEngine
                        .ruleProviders
                        .map { it.createNewRuleInstance() }
                        .filter { it is RuleAutocorrectApproveHandler }
                        .map { RuleId(it.ruleId.value) }
                        .toSet()
            }

    /**
     * Check the [code] for lint errors. If [code] is path as file reference then the '.editorconfig' files on the path to file are taken
     * into account. For each lint violation found, the [callback] is invoked.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    fun lint(
        code: Code,
        callback: (LintError) -> Unit = {},
    ) {
        try {
            ktlintRuleEngine.lint(code.toKtlintCoreCode())
        } catch (ktlintParseException: KtLintParseException) {
            throw ParseException(
                line = ktlintParseException.line,
                col = ktlintParseException.col,
                message = ktlintParseException.message,
            )
        } catch (ktlintRuleException: KtLintRuleException) {
            throw RuleException(
                line = ktlintRuleException.line,
                col = ktlintRuleException.col,
                ruleId = ktlintRuleException.ruleId,
                message = ktlintRuleException.message,
                cause = ktlintRuleException.cause,
            )
        }
    }

    /**
     * Formats style violations in [code]. Whenever a [LintError] is found the [callback] is invoked. If the [LintError] can be
     * autocorrected *and* the rule that found that the violation has implemented the [RuleAutocorrectApproveHandler] interface, the API
     * Consumer determines whether that [LintError] is to autocorrected, or not.
     *
     * When autocorrecting a [LintError] it is possible that other violations are introduced. By default, format is run up until
     * [MAX_FORMAT_RUNS_PER_FILE] times. It is still possible that violations remain after the last run. This is a trait-off between solving
     * as many errors as possible versus bad performance in case an endless loop of violations exists. In case the [callback] is implemented
     * to let the user of the API Consumer to decide which [LintError] it to be autocorrected, or not, it might be better to disable this
     * behavior by disabling [rerunAfterAutocorrect].
     *
     * In case the rule has not implemented the [RuleAutocorrectApproveHandler] interface, then the result of the [callback] is ignored as
     * the rule is not able to process it. For such rules the [defaultAutocorrect] determines whether autocorrect for this rule is to be
     * applied, or not. By default, the autocorrect will be applied (backwards compatability).
     *
     * [callback] is invoked once for each [LintError] found during any runs. As of that the [callback] might be invoked multiple times for
     * the same [LintError].
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    fun format(
        code: Code,
        callback: (LintError) -> AutocorrectDecision,
    ): String =
        try {
            ktlintRuleEngine.format(
                code.toKtlintCoreCode(),
                rerunAfterAutocorrect = true,
                defaultAutocorrect = true,
            ) { ktlintCoreLintError ->
                callback(ktlintCoreLintError.toLintError()).toKtlintCoreAutocorrectDecision()
            }
        } catch (ktlintParseException: KtLintParseException) {
            throw ParseException(
                line = ktlintParseException.line,
                col = ktlintParseException.col,
                message = ktlintParseException.message,
            )
        } catch (ktlintRuleException: KtLintRuleException) {
            throw RuleException(
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

    /**
     * Reduce memory usage by cleaning internal caches. This function should be called via the companion object only.
     */
    fun trimMemory() {
        ktlintRuleEngine.trimMemory()
    }

    /**
     * A [Suppress] annotation can only be inserted at specific locations. This function is intended for API Consumers. It updates given [code]
     * by inserting a [Suppress] annotation for the given [suppression].
     *
     * Throws [KtlintSuppressionOutOfBoundsException] when the position of the [suppression] can not be found in the [code]. Throws
     * [KtlintSuppressionNoElementFoundException] when no element can be found at the given offset.
     *
     * Returns the code with the inserted/modified suppression. Note that the returned code may not (yet) comply with formatting of all rules.
     * This is intentional as adding a suppression for the [suppression] does not mean that other lint errors which can be autocorrected should
     * be autocorrected.
     */
    fun insertSuppression(
        code: Code,
        suppression: SuppressionAtOffset,
    ): String = ktlintRuleEngine.insertSuppression(code.toKtlintCoreCode(), suppression.toKtlintCoreSuppression())

    private fun SuppressionAtOffset.toKtlintCoreSuppression() = KtlintSuppressionAtOffset(line, col, ruleId.toKtlintCoreRuleId())

    /**
     * Get a list of ".editorconfig" option descriptors for the rule sets, rules, and properties defined for the rule providers of the
     * [KtlintRuleEngine]
     */
    fun getEditorConfigOptionDescriptors(): List<KtlintEditorConfigOptionDescriptor> =
        EditorConfigOptionDescriptorsProvider(ktlintRuleEngine.ruleProviders)
            .getEditorConfigOptionDescriptors()

    // Reconsider whether to remove the Baseline functionality from Ktlint Core entirely. In many cases it not seems to work
    // properly. When creating the baseline, all errors from that run are stored. On next format run, other errors may pop
    // up after the errors of the first run were suppressed. But, those errors can not be appended to the already existing
    // baseline.
    // Also, before running ktlint format, the IDEA formatting also has run. This may already have changed the layout of the
    // file, resulting in changing offsets, as of which the ignored errors are no longer matched, and as of that not
    // suppressed.
    fun loadBaselineErrorsToIgnore(baselinePath: String): List<BaselineError> =
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
            throw BaselineLoadingException(
                // The exception message produced by ktlint already contains sufficient context of the error, but it is
                // missing the baseline path
                e.message ?: "Exception while loading baseline file '$baselinePath'",
                e,
            )
        }

    class ParseException(
        line: Int,
        col: Int,
        message: String?,
    ) : RuntimeException("$line:$col $message")

    class RuleException(
        val line: Int,
        val col: Int,
        val ruleId: String,
        message: String,
        cause: Throwable,
    ) : RuntimeException(message, cause)

    class BaselineLoadingException(
        message: String,
        cause: Throwable,
    ) : RuntimeException(message, cause)

    companion object {
        private lateinit var instance: KtlintConnector

        /**
         * In the ktlint-plugin module use the "Project.ktlintConnector" extension function to get the reference to the KtlintConnector.
         * Note that this extension uses the ProjectWrapper class to actually get a reference to the KtlintConnector, but it also updates
         * the KtlintConnector with relevant project settings.
         */
        fun getInstance(urlClassloaderFactory: (Array<URL>, ClassLoader) -> URLClassLoader): KtlintConnector {
            if (!::instance.isInitialized) {
                instance = KtlintConnector(urlClassloaderFactory)
            }
            return instance
        }

        // Trimming the memory on the KtlintConnector does not require the KtlintConnector to be updated to the active project. The call
        // via the companion object makes this more clear at the call site.
        // Note that the implementation of this method does use the _instance variable which links to an actual implementation of the
        // KtlintConnector. This is required for decoupling the ktlint-lib and ktlint-plugin modules.
        fun trimMemory() = instance.trimMemory()

        // The supportedKtlintVersions are identical for all projects as those versions are provided by the plugin which is shared by all
        // projects. The applicable values still have to be provided by an implementation class, but from the call site the code is more clear
        // when the values are called via the companion object.
        private val _supportedKtlintVersions =
            KtlintRulesetVersion.entries.map { KtlintVersion(it.label(), it.alternativeRulesetVersion?.label()) }

        // Retrieving the supported ktlint versions does not require the KtlintConnector to be updated to the active project. The call
        // via the companion object makes this more clear at the call site.
        // Note that the implementation of this method does use the _instance variable which links to an actual implementation of the
        // KtlintConnector. This is required for decoupling the ktlint-lib and ktlint-plugin modules.
        val supportedKtlintVersions: List<KtlintVersion> =
            _supportedKtlintVersions

        fun findSupportedKtlintVersionByLabel(label: String?): KtlintVersion? = supportedKtlintVersions.firstOrNull { it.label == label }
    }
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
