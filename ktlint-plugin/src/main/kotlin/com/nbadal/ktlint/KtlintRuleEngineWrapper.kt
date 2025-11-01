package com.nbadal.ktlint

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintMode.DISTRACT_FREE
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintExecutionType.FORMAT
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintExecutionType.LINT
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.FILE_RELATED_ERROR
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.NOT_STARTED
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.PLUGIN_CONFIGURATION_ERROR
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.SUCCESS
import com.pinterest.ktlint.cli.reporter.baseline.BaselineErrorHandling
import com.pinterest.ktlint.cli.reporter.baseline.BaselineLoaderException
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintParseException
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.KtLintRuleException
import com.pinterest.ktlint.rule.engine.api.KtlintSuppressionAtOffset
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.api.insertSuppression
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.ruleset.standard.KtlintRulesetVersion
import org.ec4j.core.parser.ParseException
import java.io.File

private val logger = KtlintLogger("com.nbdal.ktlint.KtlintFormat")

class KtlintRuleEngineWrapper internal constructor() {
    /**
     * The project that was used to initialize the KtlintRuleEngine
     */
    private lateinit var project: Project

    /**
     * The set of ruleset providers that are loaded into the KtLintRuleEngine
     */
    private lateinit var ruleSetProviders: RuleSetProviders

    private lateinit var ktlintRuleEngine: KtLintRuleEngine

    /**
     * Keeps the state of the last loaded baseline. It serves as a cache so that the baseline does not need to be reloaded from the file
     * system on each invocation of ktlint format.
     */
    private var baseline: Baseline? = null

    fun configure(
        ktlintRulesetVersion: KtlintRulesetVersion?,
        externalJarPaths: List<String>,
        baselinePath: String?,
    ) {
        this.baselinePath = baselinePath
        if (!::ruleSetProviders.isInitialized ||
            ruleSetProviders.ktlintRulesetVersion != ktlintRulesetVersion ||
            ruleSetProviders.externalJarPaths != externalJarPaths
        ) {
            logger.info("Configure KtlintRuleEngineWrapper $ktlintRulesetVersion, $externalJarPaths, $baselinePath")
            ruleSetProviders = RuleSetProviders(ktlintRulesetVersion ?: KtlintRulesetVersion.DEFAULT, externalJarPaths)

            ruleSetProviders
                .ruleProviders
                ?.let { ruleProviders ->
                    ktlintRuleEngine =
                        KtLintRuleEngine(
                            editorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
                            ruleProviders = ruleProviders,
                        )
                }
        }
    }

    fun configure(project: Project): KtlintRuleEngineWrapper {
        if (!::project.isInitialized || this.project != project) {
            logger.info("Configure KtlintRuleEngineWrapper for project ${project.name} (${project.basePath})")
            with(project.config()) {
                configure(
                    ktlintRulesetVersion = ktlintRulesetVersion,
                    externalJarPaths = externalJarPaths,
                    baselinePath = baselinePath,
                )
            }
            this.project = project
        }
        return this
    }

    fun ruleIdsWithAutocorrectApproveHandler(psiFile: PsiFile): Set<RuleId> =
        configure(psiFile.project)
            .ruleSetProviders
            .ruleProviders
            .orEmpty()
            .map { it.createNewRuleInstance() }
            .filter { it is RuleAutocorrectApproveHandler }
            .map { it.ruleId }
            .toSet()

    internal fun lint(
        psiFile: PsiFile,
        triggeredBy: String,
    ) = if (psiFile.virtualFile.isKotlinFile()) {
        executeKtlint(LINT, psiFile, KtlintFileAutocorrectHandler, triggeredBy)
    } else {
        KtlintResult(NOT_STARTED)
    }

    internal fun format(
        psiFile: PsiFile,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
        forceFormat: Boolean = false,
    ): KtlintResult {
        var ktlintResult = KtlintResult(NOT_STARTED)
        if (psiFile.virtualFile.isKotlinFile() &&
            (psiFile.project.config().ktlintMode == DISTRACT_FREE || forceFormat)
        ) {
            val project = psiFile.project
            val document = psiFile.viewProvider.document
            PsiDocumentManager
                .getInstance(project)
                .doPostponedOperationsAndUnblockDocument(document)
            WriteCommandAction.runWriteCommandAction(project) {
                ktlintResult = executeKtlint(FORMAT, psiFile, ktlintFormatAutoCorrectHandler, triggeredBy)
            }
            FileDocumentManager.getInstance().saveDocument(document)
            DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
            PsiDocumentManager
                .getInstance(project)
                .doPostponedOperationsAndUnblockDocument(document)
        }
        return ktlintResult
    }

    private fun executeKtlint(
        ktlintExecutionType: KtlintExecutionType,
        psiFile: PsiFile,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
    ): KtlintResult {
        configure(psiFile.project)

        logger.debug { "Start ktlintFormat on file '${psiFile.virtualFile.name}' triggered by '$triggeredBy'" }

        ruleSetProviders
            .takeIf { !it.isLoaded }
            ?.let {
                KtlintNotifier
                    .notifyError(
                        project = project,
                        title = "Error in external ruleset JAR",
                        message =
                            """
                            One or more of the external rule set JAR's defined in the ktlint settings, can not be loaded.
                            Error: ${ruleSetProviders.error.orEmpty()}
                            """.trimMargin(),
                        forceSettingsDialog = true,
                    )
                return KtlintResult(PLUGIN_CONFIGURATION_ERROR)
            }

        val baselineErrors =
            with(psiFile) {
                getBaselineErrors(virtualFile.path.pathRelativeTo(project.basePath))
            }

        val lintErrors = mutableListOf<LintError>()
        var fileChangedByFormat = false
        // The psiFile may contain unsaved changes. Get the content via the PsiDocumentManager instead of from "psiFile.text" directly. In case
        // the content of an active editor window is changed via a global find and replace, the document text is updated but the Psi (and
        // PsiFile) have not yet been changed. Add the virtual path based on path of the psiFile so that the correct '.editorconfig' is picked
        // up by ktlint.
        val code =
            Code.fromSnippetWithPath(
                content = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!.text,
                virtualPath = psiFile.virtualFile.toNioPath(),
            )

        try {
            if (ktlintExecutionType == LINT) {
                ktlintRuleEngine.lint(code) { lintError -> lintErrors.add(lintError) }
            } else {
                val formattedCode =
                    when (ktlintFormatAutoCorrectHandler) {
                        is KtlintFileAutocorrectHandler -> {
                            ktlintRuleEngine.format(code) { lintError ->
                                if (lintError.isIgnoredInBaseline(baselineErrors)) {
                                    AutocorrectDecision.NO_AUTOCORRECT
                                } else {
                                    lintErrors.add(lintError)
                                    AutocorrectDecision.ALLOW_AUTOCORRECT
                                }
                            }
                        }

                        is KtlintBlockAutocorrectHandler -> {
                            ktlintRuleEngine.format(code) { lintError ->
                                if (lintError.isIgnoredInBaseline(baselineErrors)) {
                                    AutocorrectDecision.NO_AUTOCORRECT
                                } else {
                                    val offset = psiFile.getLineStartOffset(lintError)
                                    if (ktlintFormatAutoCorrectHandler.isRangeContainingOffset(offset)) {
                                        // Do not add the lint error to the list of lint errors as it will be autocorrected
                                        AutocorrectDecision.ALLOW_AUTOCORRECT
                                    } else {
                                        lintErrors.add(lintError)
                                        AutocorrectDecision.NO_AUTOCORRECT
                                    }
                                }
                            }
                        }

                        is KtlintViolationAutocorrectHandler -> {
                            ktlintRuleEngine.format(code) { lintError ->
                                if (lintError == ktlintFormatAutoCorrectHandler.lintError) {
                                    AutocorrectDecision.ALLOW_AUTOCORRECT
                                } else {
                                    AutocorrectDecision.NO_AUTOCORRECT
                                }
                            }
                        }
                    }
                if (formattedCode != code.content) {
                    psiFile.viewProvider.document.setText(formattedCode)
                    fileChangedByFormat = true
                }
            }
            logger.debug { "Finished ktlintFormat on file '${psiFile.virtualFile.name}' triggered by '$triggeredBy' successfully" }
            return KtlintResult(
                SUCCESS,
                lintErrors.filterNot { it.isIgnoredInBaseline(baselineErrors) },
                fileChangedByFormat,
            )
        } catch (ktLintParseException: KtLintParseException) {
            // Most likely the file contains a compilation error which prevents it from being parsed. The user should resolve those errors.
            // The stacktrace is excluded from the message as it would distract from resolving the error.
            KtlintNotifier.notifyWarning(
                project = project,
                title = "Parsing error",
                message =
                    """
                    File '${psiFile.virtualFile.path}' can not be parsed by ktlint. Please resolve all (compilation) errors first.
                    Error: ${ktLintParseException.message}
                    """.trimIndent(),
            )
            return KtlintResult(FILE_RELATED_ERROR)
        } catch (ktLintRuleException: KtLintRuleException) {
            KtlintNotifier.notifyError(
                project = project,
                title = "KtLintRuleException",
                message =
                    """
                    An error occurred in a rule. Please see stacktrace below for rule that caused the problem and contact maintainer of the
                    rule when the error can be reproduced.
                    ${ktLintRuleException.stackTraceToString()}
                    """.trimIndent(),
            )
            return KtlintResult(FILE_RELATED_ERROR)
        } catch (parseException: ParseException) {
            KtlintNotifier.notifyError(
                project = project,
                title = "Invalid editorconfig",
                // The exception message already contains the path to the file, so don't repeat it
                message =
                    """
                    An error occurred while reading the '.editorconfig':
                    ${parseException.message}
                    """.trimIndent(),
            )
            return KtlintResult(FILE_RELATED_ERROR)
        } catch (exception: Exception) {
            KtlintNotifier.notifyError(
                project = project,
                title = "Invalid editorconfig",
                message =
                    """
                    An error occurred while processing file '${psiFile.virtualFile.path}':
                    ${exception.stackTraceToString()}
                    """.trimIndent(),
            )
            return KtlintResult(FILE_RELATED_ERROR)
        }
    }

    private enum class KtlintExecutionType { LINT, FORMAT }

    internal data class KtlintResult(
        val status: Status,
        val lintErrors: List<LintError> = emptyList(),
        val fileChangedByFormat: Boolean = false,
    ) {
        enum class Status {
            /**
             * Ktlint was not (yet) executed
             */
            NOT_STARTED,

            /**
             * Ktlint ran successfully.
             */
            SUCCESS,

            /**
             * Ktlint can not run due to error in plugin configuration.
             */
            PLUGIN_CONFIGURATION_ERROR,

            /**
             * Ktlint can not run due to error related to the file.
             */
            FILE_RELATED_ERROR,
        }
    }

    private fun LintError.isIgnoredInBaseline(baselineErrors: List<KtlintCliError>) =
        baselineErrors
            .any { baselineError ->
                baselineError.line == line &&
                    baselineError.col == col &&
                    baselineError.ruleId == ruleId.value &&
                    baselineError.status == KtlintCliError.Status.BASELINE_IGNORED
            }

    private fun String.pathRelativeTo(projectBasePath: String?): String =
        if (projectBasePath.isNullOrBlank()) {
            this
        } else {
            removePrefix(projectBasePath).removePrefix("/")
        }

    fun insertSuppression(
        psiFile: PsiFile,
        code: Code,
        ktlintSuppressionAtOffset: KtlintSuppressionAtOffset,
    ): String {
        configure(psiFile.project)
        return ktlintRuleEngine.insertSuppression(code, ktlintSuppressionAtOffset)
    }

    private var baselinePath: String? = null

    /**
     * Clears the ".editorconfig" cache so that it gets reloaded. This should only be called after saving a modified ".editorconfig".
     */
    fun resetKtlintRuleEngine() = ktlintRuleEngine.trimMemory()

    private data class RuleSetProviders(
        val ktlintRulesetVersion: KtlintRulesetVersion,
        val externalJarPaths: List<String>,
    ) {
        private var _error: String? = null

        val error: String?
            get() = _error

        private var _isLoaded = false

        val isLoaded: Boolean
            get() = _isLoaded

        val ruleProviders =
            try {
                _error = null
                _isLoaded = true
                externalJarPaths
                    .map { File(it).toURI().toURL() }
                    .loadCustomRuleProviders()
                    .also { logger.info { "Loaded ${it.size} rules from custom rule providers $externalJarPaths" } }
                    .plus(ktlintRulesetVersion.ruleProviders())
                    .also {
                        logger.info {
                            "Loaded ${ktlintRulesetVersion.ruleProviders().size} rules from default ktlint ruleset version '${ktlintRulesetVersion.label()}'"
                        }
                    }
            } catch (throwable: Throwable) {
                _isLoaded = false
                _error = throwable.toString()
                null
            }
    }

    data class Baseline(
        val baselinePath: String?,
        val lintErrorsPerFile: Map<String, List<KtlintCliError>>,
    )

    fun getBaselineErrors(filePath: String): List<KtlintCliError> {
        if (baseline?.baselinePath != baselinePath) {
            baseline = loadBaseline()
        }
        return baseline
            ?.lintErrorsPerFile
            ?.get(filePath)
            ?: emptyList()
    }

    private fun loadBaseline() =
        baselinePath
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->
                try {
                    Baseline(
                        baselinePath = baselinePath,
                        com.pinterest.ktlint.cli.reporter.baseline
                            .loadBaseline(path, BaselineErrorHandling.EXCEPTION)
                            .lintErrorsPerFile
                            .also { logger.debug { "Load baseline from file '$path'" } },
                    )
                } catch (e: BaselineLoaderException) {
                    // The exception message produced by ktlint already contains sufficient context of the error
                    val message = e.message ?: "Exception while loading baseline file '$baselinePath'"
                    KtlintNotifier.notifyError(
                        project = project,
                        title = "Loading baseline",
                        message = message,
                        forceSettingsDialog = true,
                    )
                    logger.debug(e) { message }
                    Baseline(baselinePath, emptyMap())
                }
            }
            ?: Baseline(baselinePath, emptyMap())

    fun ruleProviders(project: Project) =
        configure(project)
            .ruleSetProviders
            .ruleProviders
            .orEmpty()

    companion object {
        val instance = KtlintRuleEngineWrapper()
    }
}
