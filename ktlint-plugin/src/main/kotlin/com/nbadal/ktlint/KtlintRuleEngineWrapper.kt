package com.nbadal.ktlint

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintExecutionResult.FileRelatedError
import com.nbadal.ktlint.KtlintExecutionResult.FormatResult
import com.nbadal.ktlint.KtlintExecutionResult.LintResult
import com.nbadal.ktlint.KtlintExecutionResult.NotExecuted
import com.nbadal.ktlint.KtlintMode.DISTRACT_FREE
import com.nbadal.ktlint.KtlintNotifier.KtlintNotificationGroup.CONFIGURATION
import com.nbadal.ktlint.KtlintNotifier.KtlintNotificationGroup.DEFAULT
import com.nbadal.ktlint.KtlintNotifier.KtlintNotificationGroup.RULE
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintExecutionType.FORMAT
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintExecutionType.LINT
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintVersion.Source.NATIVE_PLUGIN_CONFIGURATION
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintVersion.Source.SHARED_PLUGIN_PROPERTIES
import com.pinterest.ktlint.cli.reporter.baseline.BaselineErrorHandling
import com.pinterest.ktlint.cli.reporter.baseline.BaselineLoaderException
import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
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
import kotlinx.coroutines.CancellationException
import org.ec4j.core.parser.ParseException
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import java.nio.file.Path

private val logger = KtlintLogger()

internal class KtlintRuleEngineWrapper internal constructor() {
    private val ktlintRuleWrapperConfig = KtlintRuleWrapperConfig()

    fun ruleIdsWithAutocorrectApproveHandler(psiFile: PsiFile): Set<RuleId> =
        ktlintRuleWrapperConfig.ruleIdsWithAutocorrectApproveHandler(psiFile.project)

    /**
     * Runs ktlint lint on the [ktlintDocumentSnapshot]. Should not be called while holding a read lock, as ktlint can run for a long time
     * and does not respond to cancellation. This blocks any write action, and thereby the EDT.
     */
    fun lint(
        ktlintDocumentSnapshot: KtlintDocumentSnapshot,
        triggeredBy: String,
    ) = ktlintDocumentSnapshot
        .takeIf { ktlintDocumentSnapshot.psiFile.virtualFile.isKotlinFile() }
        ?.let { executeKtlint(LINT, ktlintDocumentSnapshot, KtlintFileAutocorrectHandler, triggeredBy) }
        ?: NotExecuted

    fun isFormatAllowed(
        psiFile: PsiFile,
        forceFormat: Boolean,
    ) = psiFile.virtualFile.isKotlinFile() && (psiFile.project.config().ktlintMode == DISTRACT_FREE || forceFormat)

    /**
     * Runs ktlint format on the [ktlintDocumentSnapshot] without changing the document. The caller is responsible for applying the
     * formatted code to the document. Should not be called while holding a lock, unless the platform leaves no alternative. See
     * [formatBlocking].
     */
    fun format(
        ktlintDocumentSnapshot: KtlintDocumentSnapshot,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
    ): KtlintExecutionResult = executeKtlint(FORMAT, ktlintDocumentSnapshot, ktlintFormatAutoCorrectHandler, triggeredBy)

    /**
     * Only for [KtlintPostFormatProcessor], as the file has to be formatted when the processor returns to the platform. In all other cases
     * use the [KtlintFormatService] as this function blocks the calling thread for as long as ktlint is running. The platform calls the
     * [KtlintPostFormatProcessor] inside a write action, which also grants read access.
     */
    fun formatBlocking(
        psiFile: PsiFile,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
    ): KtlintExecutionResult {
        if (!isFormatAllowed(psiFile, forceFormat = true)) {
            return NotExecuted
        }

        val project = psiFile.project
        val document = psiFile.viewProvider.document
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)
        val ktlintDocumentSnapshot = KtlintDocumentSnapshot.from(psiFile) ?: return NotExecuted
        val ktlintFormatServiceResult = format(ktlintDocumentSnapshot, ktlintFormatAutoCorrectHandler, triggeredBy)
        if (ktlintFormatServiceResult is FormatResult && ktlintFormatServiceResult.contentChanged) {
            WriteCommandAction.runWriteCommandAction(project) {
                @Suppress("UsePropertyAccessSyntax")
                document.setText(ktlintFormatServiceResult.formattedCode)
                FileDocumentManager.getInstance().saveDocument(document)
                DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)
            }
        }
        return ktlintFormatServiceResult
    }

    private fun executeKtlint(
        ktlintExecutionType: KtlintExecutionType,
        ktlintDocumentSnapshot: KtlintDocumentSnapshot,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
    ): KtlintExecutionResult {
        val psiFile = ktlintDocumentSnapshot.psiFile
        logger.debug { "Start executeKtlint on file '${psiFile.virtualFile.path}' triggered by '$triggeredBy'" }
        return measureTimeMillisWithResult {
            // On each invocation of Ktlint repeat the error when an external ruleset was not properly loaded
            notifyLoadingExternalRulesetJarError(psiFile)

            val baselineErrors = loadBaselineErrors(psiFile)
            val code = ktlintDocumentSnapshot.code()
            try {
                val ktlintRuleEngine = ktlintRuleWrapperConfig.ktLintRuleEngine(psiFile.project)
                if (ktlintExecutionType == LINT) {
                    executeKtlintLint(ktlintRuleEngine, code, baselineErrors)
                } else {
                    executeKtlintFormat(ktlintFormatAutoCorrectHandler, ktlintRuleEngine, code, baselineErrors, ktlintDocumentSnapshot)
                }
            } catch (cancellationException: CancellationException) {
                // Covers both the ProcessCanceledException of the platform and the cancellation of a coroutine. Cancellation is not an
                // error, and has to be propagated so that the caller stops.
                throw cancellationException
            } catch (_: KtLintParseException) {
                // ParseException occur very frequently while typing code, and a save operation is executed while code cannot be compiled at
                // that moment. Display a notification is distracting, and not helpful.
                return FileRelatedError
            } catch (ktLintRuleException: KtLintRuleException) {
                notifyKtlintRuleException(psiFile, ktLintRuleException)
                return FileRelatedError
            } catch (parseException: ParseException) {
                notifyEditorconfigParseException(psiFile, parseException)
                return FileRelatedError
            } catch (exception: Exception) {
                if (exception is IllegalStateException && exception.message?.startsWith("Skipping rule(s)") == true) {
                    notifyInvalidEditorconfigConfiguration(psiFile, exception)
                } else {
                    notifyUncategorizedError(psiFile, exception)
                }
                return FileRelatedError
            }
        }.let { (millis, ktlintExecutionResult) ->
            logger.debug {
                "Finished executeKtlint '$ktlintExecutionType' on file '${psiFile.virtualFile.path}' triggered by '$triggeredBy' successfully in $millis ms"
            }
            ktlintExecutionResult
        }
    }

    private fun loadBaselineErrors(psiFile: PsiFile): BaselineErrors =
        ktlintRuleWrapperConfig
            .baselineProvider(psiFile.project)
            .baselineErrors(psiFile)

    private fun KtlintDocumentSnapshot.code(): Code =
        Code.fromSnippetWithPath(
            content = content,
            // Add the virtual path based on path of the psiFile so that the correct '.editorconfig' is picked up by ktlint.
            // For compatibility with unit tests the NioPath cannot be directly determined via the virtual file
            virtualPath = Path.of(psiFile.virtualFile.path),
        )

    private fun notifyLoadingExternalRulesetJarError(psiFile: PsiFile) {
        ktlintRuleWrapperConfig
            .errorLoadingExternalRulesetJar(psiFile.project)
            ?.let { error ->
                // Report the external ruleset that was not loaded successfully. But continue with format of file with the rule providers
                // that have been loaded successfully.
                KtlintNotifier
                    .notifyError(
                        notificationGroup = RULE,
                        project = psiFile.project,
                        title = "Invalid external ruleset JAR",
                        message = error,
                    ) {
                        addAction(OpenSettingsAction(psiFile.project))
                    }
                // Do not prevent formatting of file with the rulesets that were loaded successfully.
            }
    }

    private fun executeKtlintLint(
        ktlintRuleEngine: KtLintRuleEngine,
        code: Code,
        baselineErrors: BaselineErrors,
    ): LintResult {
        val lintErrors = mutableListOf<LintError>()
        ktlintRuleEngine.lint(code) { lintError -> lintErrors.add(lintError) }
        return LintResult(lintErrors.filterNot { baselineErrors.contains(it) })
    }

    private fun executeKtlintFormat(
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        ktlintRuleEngine: KtLintRuleEngine,
        code: Code,
        baselineErrors: BaselineErrors,
        ktlintDocumentSnapshot: KtlintDocumentSnapshot,
    ): FormatResult {
        val lintErrors = mutableListOf<LintError>()
        val formattedCode =
            when (ktlintFormatAutoCorrectHandler) {
                is KtlintFileAutocorrectHandler -> {
                    ktlintRuleEngine.format(code) { lintError ->
                        if (baselineErrors.contains(lintError)) {
                            AutocorrectDecision.NO_AUTOCORRECT
                        } else {
                            lintErrors.add(lintError)
                            AutocorrectDecision.ALLOW_AUTOCORRECT
                        }
                    }
                }

                is KtlintBlockAutocorrectHandler -> {
                    ktlintRuleEngine.format(code) { lintError ->
                        if (baselineErrors.contains(lintError)) {
                            AutocorrectDecision.NO_AUTOCORRECT
                        } else {
                            val offset = ktlintDocumentSnapshot.document.getLineStartOffset(lintError)
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
        return FormatResult(
            lintErrors = lintErrors.filterNot { baselineErrors.contains(it) },
            formattedCode = formattedCode,
            contentChanged = formattedCode != code.content,
        )
    }

    private fun notifyKtlintRuleException(
        psiFile: PsiFile,
        ktLintRuleException: KtLintRuleException,
    ) {
        KtlintNotifier.notifyError(
            notificationGroup = RULE,
            project = psiFile.project,
            title = "KtLintRuleException",
            message =
                """
                An error occurred in a rule. Please see stacktrace below for rule that caused the problem and contact maintainer of the
                rule when the error can be reproduced.
                ${ktLintRuleException.stackTraceToString()}
                """.trimIndent(),
        )
    }

    private fun notifyEditorconfigParseException(
        psiFile: PsiFile,
        parseException: ParseException,
    ) {
        KtlintNotifier.notifyError(
            notificationGroup = DEFAULT,
            project = psiFile.project,
            title = "Invalid editorconfig",
            // The exception message already contains the path to the file, so don't repeat it
            message =
                """
                An error occurred while reading the '.editorconfig':
                ${parseException.message}
                """.trimIndent(),
        )
    }

    private fun notifyInvalidEditorconfigConfiguration(
        psiFile: PsiFile,
        illegalStateException: IllegalStateException,
    ) {
        KtlintNotifier.notifyError(
            notificationGroup = CONFIGURATION,
            project = psiFile.project,
            title = "Invalid editorconfig configuration",
            message = illegalStateException.message!!,
        )
    }

    private fun notifyUncategorizedError(
        psiFile: PsiFile,
        exception: Exception,
    ) {
        KtlintNotifier.notifyError(
            notificationGroup = DEFAULT,
            project = psiFile.project,
            title = "Uncategorized error",
            message =
                """
                An error occurred while processing file '${psiFile.virtualFile.path}':
                ${exception.stackTraceToString()}
                """.trimIndent(),
        )
    }

    fun insertSuppression(
        psiFile: PsiFile,
        code: Code,
        ktlintSuppressionAtOffset: KtlintSuppressionAtOffset,
    ): String = ktlintRuleWrapperConfig.ktLintRuleEngine(psiFile.project).insertSuppression(code, ktlintSuppressionAtOffset)

    fun ruleProviders(project: Project) = ktlintRuleWrapperConfig.ktLintRuleEngine(project).ruleProviders

    fun ktlintVersion(project: Project) =
        ktlintRuleWrapperConfig
            .ktlintPluginsPropertiesReader(project)
            .ktlintVersion()
            ?.let { version -> KtlintVersion(version, SHARED_PLUGIN_PROPERTIES) }
            ?: KtlintVersion(
                project.config().ktlintRulesetVersion?.name ?: KtlintRulesetVersion.DEFAULT.name,
                NATIVE_PLUGIN_CONFIGURATION,
            )

    fun reset(project: Project) {
        ktlintRuleWrapperConfig.reset(project)
        project.resetKtlintAnnotatorUserData()
    }

    internal data class KtlintVersion(
        val version: String,
        val source: Source,
    ) {
        enum class Source {
            NATIVE_PLUGIN_CONFIGURATION,
            SHARED_PLUGIN_PROPERTIES,
        }
    }

    private enum class KtlintExecutionType { LINT, FORMAT }

    companion object {
        val instance = KtlintRuleEngineWrapper()
    }
}

/**
 * Accessed from the EDT and from background threads. The lock is only held while configuring, and not while ktlint is running.
 */
private class KtlintRuleWrapperConfig {
    private lateinit var ktlintRuleEngineProvider: KtlintRuleEngineProvider

    private lateinit var baselineProvider: BaselineProvider

    private lateinit var ktlintPluginsPropertiesReader: KtlintPluginsPropertiesReader

    init {
        reset(null)
    }

    @Synchronized
    fun reset(project: Project?) {
        // Ktlint has a static cache which is shared across all instances of the KtlintRuleEngine. Creates a new KtlintRuleEngine to load
        // changes in the editorconfig is therefore not sufficient. The memory needs to be cleared explicitly.
        if (::ktlintRuleEngineProvider.isInitialized) {
            ktlintRuleEngineProvider.trimMemoryKtlintRuleEngine()
        }

        ktlintRuleEngineProvider = KtlintRuleEngineProvider()
        baselineProvider = BaselineProvider()
        ktlintPluginsPropertiesReader = KtlintPluginsPropertiesReader()
        if (project != null) {
            configure(project)
        }
    }

    @Synchronized
    fun ktLintRuleEngine(project: Project): KtLintRuleEngine = configure(project).ktlintRuleEngineProvider.ktlintRuleEngine

    @Synchronized
    fun errorLoadingExternalRulesetJar(project: Project): String? =
        configure(project).ktlintRuleEngineProvider.errorLoadingExternalRulesetJar

    @Synchronized
    fun ruleIdsWithAutocorrectApproveHandler(project: Project): Set<RuleId> =
        configure(project).ktlintRuleEngineProvider.ruleIdsWithAutocorrectApproveHandler

    @Synchronized
    fun configure(project: Project): KtlintRuleWrapperConfig =
        apply {
            with(project.config()) {
                baselineProvider.configure(baselinePath)
                ktlintPluginsPropertiesReader.configure(project)
                ktlintRuleEngineProvider.configure(ktlintRulesetVersionWithOrigin(), externalJarPaths)
            }
        }

    private fun KtlintProjectSettings.ktlintRulesetVersionWithOrigin() =
        ktlintRulesetVersionFromSharedPropertiesFile()
            ?: ktlintRulesetVersionFromKtlintConfiguration()
            ?: defaultKtlintRulesetVersion()

    private fun ktlintRulesetVersionFromSharedPropertiesFile() =
        ktlintPluginsPropertiesReader
            .ktlintRulesetVersion()
            ?.let { KtlintRulesetVersionWithOrigin(it, "Defined in property shared by all ktlint plugins") }

    private fun KtlintProjectSettings.ktlintRulesetVersionFromKtlintConfiguration() =
        ktlintRulesetVersion
            ?.let { KtlintRulesetVersionWithOrigin(it, "Defined in ktlint-intellij-plugin configuration") }

    private fun defaultKtlintRulesetVersion() =
        KtlintRulesetVersion
            .DEFAULT
            .let { KtlintRulesetVersionWithOrigin(it, "Default Ktlint version as ktlint-intellij-plugin configuration is not found") }

    @Synchronized
    fun ktlintPluginsPropertiesReader(project: Project): KtlintPluginsPropertiesReader {
        configure(project)
        return ktlintPluginsPropertiesReader
    }

    @Synchronized
    fun baselineProvider(project: Project): BaselineProvider {
        configure(project)
        return baselineProvider
    }
}

private class KtlintRuleEngineProvider {
    /**
     * The set of ruleset providers that are loaded into the KtLintRuleEngine
     */
    private lateinit var ruleSetProviders: RuleSetProviders

    lateinit var ktlintRuleEngine: KtLintRuleEngine
        private set

    fun configure(
        ktlintRulesetVersionWithOrigin: KtlintRulesetVersionWithOrigin,
        externalJarPaths: List<String>,
    ) {
        if (!::ruleSetProviders.isInitialized ||
            ruleSetProviders.ktlintRulesetVersion != ktlintRulesetVersionWithOrigin.ktlintRulesetVersion ||
            ruleSetProviders.externalJarPaths != externalJarPaths
        ) {
            logger.info("Configure KtlintRuleEngineWrapper $ktlintRulesetVersionWithOrigin, $externalJarPaths")
            ruleSetProviders = RuleSetProviders(ktlintRulesetVersionWithOrigin.ktlintRulesetVersion, externalJarPaths)
            ktlintRuleEngine =
                KtLintRuleEngine(
                    editorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
                    ruleProviders = ruleSetProviders.ruleProviders,
                )
        }
    }

    val errorLoadingExternalRulesetJar: String?
        get() = ruleSetProviders.errorLoadingExternalRulesetJar

    val ruleIdsWithAutocorrectApproveHandler: Set<RuleId>
        get() = ruleSetProviders.ruleIdsWithAutocorrectApproveHandler

    fun trimMemoryKtlintRuleEngine() {
        if (::ktlintRuleEngine.isInitialized) {
            ktlintRuleEngine.trimMemory()
        }
    }
}

private data class RuleSetProviders(
    val ktlintRulesetVersion: KtlintRulesetVersion,
    val externalJarPaths: List<String>,
) {
    var errorLoadingExternalRulesetJar: String? = null
        private set

    val ruleProviders =
        externalJarPaths
            .flatMap { externalJarRuleProviders(it) }
            .plus(standardKtlintRuleProviders())
            .toSet()

    val ruleIdsWithAutocorrectApproveHandler: Set<RuleId> by lazy {
        // Need to instantiate each rule in order to check whether it has an RuleAutocorrectHandler. It only needs to be determined when
        // changing the ruleset version, or when adding/removing additional external rulesets.
        ruleProviders
            .map { it.createNewRuleInstance() }
            .filter { it is RuleAutocorrectApproveHandler }
            .map { it.ruleId }
            .toSet()
    }

    private fun externalJarRuleProviders(path: String) =
        try {
            loadRuleProvidersFromExternalJarPath(path)
        } catch (throwable: Throwable) {
            logger.error(throwable) { "Cannot load external ruleset jar '$path" }
            // It is not possible to direct call KtlintNotifier to display a notification. This results in endless loop while trying to load
            // the settings dialog
            errorLoadingExternalRulesetJar =
                "An error occurred while reading external ruleset file '$path'. No ktlint ruleset can be loaded from this file."
            emptyList()
        }

    private fun standardKtlintRuleProviders() =
        ktlintRulesetVersion
            .ruleProviders()
            .also { logger.info { "Loaded ${it.size} rules from default ktlint ruleset version '${ktlintRulesetVersion.label()}'" } }
}

/**
 * Keeps the state of loaded baseline. It serves as a cache so that the baseline does not need to be reloaded from the file system on each
 * invocation of ktlint format.
 */
class BaselineProvider {
    private var baselinePath: String? = null
    private var error: String? = null
    private var lintErrorsPerFile: Map<String, List<KtlintCliError>> = emptyMap()

    fun configure(baselinePath: String?) {
        if (baselinePath != this.baselinePath) {
            this.baselinePath = baselinePath
            error = null
            lintErrorsPerFile = emptyMap()
            if (baselinePath != null) {
                try {
                    lintErrorsPerFile =
                        loadBaseline(baselinePath, BaselineErrorHandling.EXCEPTION)
                            .lintErrorsPerFile
                            .also { logger.debug { "Load baseline from file '$baselinePath'" } }
                } catch (e: BaselineLoaderException) {
                    // The exception message produced by ktlint already contains sufficient context of the error
                    error = e.message ?: "Exception while loading baseline file '$baselinePath'"
                    logger.debug(e) { error }
                }
            }
        }
    }

    fun baselineErrors(psiFile: PsiFile): BaselineErrors {
        error?.run {
            KtlintNotifier.notifyError(
                notificationGroup = CONFIGURATION,
                project = psiFile.project,
                title = "Loading baseline",
                message = this,
            ) {
                addAction(OpenSettingsAction(psiFile.project))
            }
            return BaselineErrors(emptyList())
        }

        return with(psiFile) {
            val pathRelativeTo = virtualFile.path.pathRelativeTo(project.basePath)
            lintErrorsPerFile[pathRelativeTo]
                .orEmpty()
                .let { BaselineErrors(it) }
        }
    }

    private fun String.pathRelativeTo(projectBasePath: String?): String =
        if (projectBasePath.isNullOrBlank()) {
            this
        } else {
            removePrefix(projectBasePath).removePrefix("/")
        }
}

class BaselineErrors(
    private val errors: List<KtlintCliError>,
) {
    fun contains(lintError: LintError): Boolean =
        errors
            .any { baselineError ->
                with(lintError) {
                    baselineError.line == line &&
                        baselineError.col == col &&
                        baselineError.ruleId == ruleId.value &&
                        baselineError.status == KtlintCliError.Status.BASELINE_IGNORED
                }
            }
}

private class OpenSettingsAction(
    val project: Project,
) : NotificationAction("Open ktlint settings...") {
    override fun actionPerformed(
        e: AnActionEvent,
        notification: Notification,
    ) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, KtlintConfig::class.java)
    }
}

private data class KtlintRulesetVersionWithOrigin(
    val ktlintRulesetVersion: KtlintRulesetVersion,
    val origin: String,
)
