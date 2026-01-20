package com.nbadal.ktlint.connector

import java.io.File
import java.net.URLClassLoader
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

/**
 * The [KtlintConnector] is an abstraction that aims to decouple the Ktlint Core code in the "ktlint-lib" module from the IntelliJ IDEA
 * plugin code in mode "ktlint-plugin".
 */
interface KtlintConnector {
    fun loadRulesets(ktlintVersion: KtlintVersion)

    fun loadExternalRulesetJars(externalJarPaths: List<String>): List<String>

    /**
     * Check the [code] for lint errors. If [code] is path as file reference then the '.editorconfig' files on the path to file are taken
     * into account. For each lint violation found, the [callback] is invoked.
     *
     * @throws KtLintParseException if text is not a valid Kotlin code
     * @throws KtLintRuleException in case of internal failure caused by a bug in rule implementation
     */
    fun lint(
        code: Code,
        callback: (LintError) -> Unit = { },
    )

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
        rerunAfterAutocorrect: Boolean = true,
        defaultAutocorrect: Boolean = true,
        callback: (LintError) -> AutocorrectDecision,
    ): String

    /**
     * Reduce memory usage by cleaning internal caches.
     */
    fun trimMemory()

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
    ): String

    /**
     * Get a list of ".editorconfig" option descriptors for the rule sets, rules, and properties defined for the rule providers of the
     * [KtlintRuleEngine]
     */
    fun getEditorConfigOptionDescriptors(): List<KtlintEditorConfigOptionDescriptor>

    fun ruleIdsWithAutocorrectApproveHandler(): Set<RuleId>

    fun loadBaselineErrorsToIgnore(baselinePath: String): List<BaselineError>

    fun supportedKtlintVersions(): List<KtlintVersion>

    fun findSupportedKtlintVersionByLabel(label: String?): KtlintVersion?

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
        private val _instance = loadKtlintConnectorImpl()

        // TODO: replace function with variable
        fun getInstance() = _instance
    }
}

fun loadKtlintConnectorImpl(): KtlintConnector =
    try {
        with(KtlintConnector::class.java) {
            ServiceLoader
                .load(this, URLClassLoader(arrayOf(File("jar:ktlint-lib.jar").toURI().toURL()), this.classLoader))
                .single()
        }
    } catch (e: ServiceConfigurationError) {
        throw KtlintConnectorException("Failed to load KtlintConnector", e)
    }

class KtlintConnectorException(
    message: String,
    throwable: Throwable? = null,
) : RuntimeException(message, throwable)
