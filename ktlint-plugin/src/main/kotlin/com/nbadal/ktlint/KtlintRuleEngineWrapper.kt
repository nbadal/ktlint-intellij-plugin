package com.nbadal.ktlint

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.nbadal.ktlint.KtlintMode.DISTRACT_FREE
import com.nbadal.ktlint.KtlintNotifier.KtlintNotificationGroup.CONFIGURATION
import com.nbadal.ktlint.KtlintNotifier.KtlintNotificationGroup.DEFAULT
import com.nbadal.ktlint.KtlintNotifier.KtlintNotificationGroup.RULE
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintExecutionType.FORMAT
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintExecutionType.LINT
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.FILE_RELATED_ERROR
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.NOT_STARTED
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.PLUGIN_CONFIGURATION_ERROR
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.SUCCESS
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
import org.ec4j.core.parser.ParseException
import java.io.File
import java.lang.IllegalStateException

private val logger = KtlintLogger("com.nbdal.ktlint.KtlintFormat")

internal class KtlintRuleEngineWrapper internal constructor() {
    private val ktlintRuleWrapperConfig = KtlintRuleWrapperConfig()

    fun ruleIdsWithAutocorrectApproveHandler(psiFile: PsiFile): Set<RuleId> =
        ruleProviders(psiFile.project)
            .map { it.createNewRuleInstance() }
            .filter { it is RuleAutocorrectApproveHandler }
            .map { it.ruleId }
            .toSet()

    fun lint(
        psiFile: PsiFile,
        triggeredBy: String,
    ) = if (psiFile.virtualFile.isKotlinFile()) {
        executeKtlint(LINT, psiFile, KtlintFileAutocorrectHandler, triggeredBy)
    } else {
        KtlintResult(NOT_STARTED)
    }

    fun format(
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

    fun formatAllOpenFiles(
        project: Project,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
    ) {
        FileEditorManager
            .getInstance(project)
            .openFiles
            .forEach { virtualFile ->
                PsiManager
                    .getInstance(project)
                    .findFile(virtualFile)
                    ?.let { psiFile ->
                        format(
                            psiFile = psiFile,
                            ktlintFormatAutoCorrectHandler = ktlintFormatAutoCorrectHandler,
                            triggeredBy = triggeredBy,
                        )
                    }
            }
    }

    private fun executeKtlint(
        ktlintExecutionType: KtlintExecutionType,
        psiFile: PsiFile,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
    ): KtlintResult {
        logger.debug { "Start ktlintFormat on file '${psiFile.virtualFile.name}' triggered by '$triggeredBy'" }

        ktlintRuleWrapperConfig
            .configure(psiFile.project)
            .ktlintRuleEngineProvider
            .takeIf { it.hasErrorLoadingExternalRulesetJar() }
            ?.let { ktlintRuleEngineProvider ->
                KtlintNotifier
                    .notifyError(
                        notificationGroup = RULE,
                        project = psiFile.project,
                        title = "Error in external ruleset JAR",
                        message =
                            """
                            One or more of the external rule set JAR's defined in the ktlint settings, can not be loaded.
                            Error: ${ktlintRuleEngineProvider.errorLoadingExternalRulesetJar()}
                            """.trimMargin(),
                    ) {
                        addAction(OpenSettingsAction(psiFile.project))
                    }
                return KtlintResult(PLUGIN_CONFIGURATION_ERROR)
            }

        val baselineErrors =
            ktlintRuleWrapperConfig
                .baselineProvider(psiFile.project)
                .baselineErrors(psiFile)
        val lintErrors = mutableListOf<LintError>()
        var fileChangedByFormat = false
        // The psiFile may contain unsaved changes. Get the content via the PsiDocumentManager instead of from "psiFile.text" directly. In case
        // the content of an active editor window is changed via a global find and replace, the document text is updated but the Psi (and
        // PsiFile) have not yet been changed. Add the virtual path based on path of the psiFile so that the correct '.editorconfig' is picked
        // up by ktlint.
        val code =
            Code.fromSnippetWithPath(
                content = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile)!!.text,
                virtualPath = psiFile.virtualFile.toNioPath(),
            )

        try {
            val ktlintRuleEngine =
                ktlintRuleWrapperConfig
                    .configure(psiFile.project)
                    .ktlintRuleEngineProvider
                    .ktlintRuleEngine
            if (ktlintExecutionType == LINT) {
                ktlintRuleEngine.lint(code) { lintError -> lintErrors.add(lintError) }
            } else {
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
                lintErrors.filterNot { baselineErrors.contains(it) },
                fileChangedByFormat,
            )
        } catch (_: KtLintParseException) {
            // ParseException occur very frequently while typing code, and a save operation is executed while code cannot be compiled at
            // that moment. Display a notification is distracting, and not helpful.
            return KtlintResult(FILE_RELATED_ERROR)
        } catch (ktLintRuleException: KtLintRuleException) {
            KtlintNotifier.notifyError(
                notificationGroup = RULE,
                project = psiFile.project,
                title = "KtLintRuleException",
                message =
                    """
                    An error occurred while processing file '${psiFile.virtualFile.path}'. Please see stacktrace below for the rule that 
                    caused the problem, and then contact maintainer of the rule when the error can be reproduced.
                    ${ktLintRuleException.stackTraceToString()}
                    """.trimIndent(),
            )
            return KtlintResult(FILE_RELATED_ERROR)
        } catch (parseException: ParseException) {
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
            return KtlintResult(FILE_RELATED_ERROR)
        } catch (exception: Exception) {
            if (exception is IllegalStateException && exception.message?.startsWith("Skipping rule(s)") == true) {
                KtlintNotifier.notifyError(
                    notificationGroup = CONFIGURATION,
                    project = psiFile.project,
                    title = "Invalid editorconfig configuration",
                    message = exception.message!!,
                )
            } else {
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
            return KtlintResult(FILE_RELATED_ERROR)
        }
    }

    fun insertSuppression(
        psiFile: PsiFile,
        code: Code,
        ktlintSuppressionAtOffset: KtlintSuppressionAtOffset,
    ): String =
        ktlintRuleWrapperConfig
            .configure(psiFile.project)
            .ktlintRuleEngineProvider
            .ktlintRuleEngine
            .insertSuppression(code, ktlintSuppressionAtOffset)

    fun ruleProviders(project: Project) =
        ktlintRuleWrapperConfig
            .configure(project)
            .ktlintRuleEngineProvider
            .ktlintRuleEngine
            .ruleProviders

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

    companion object {
        val instance = KtlintRuleEngineWrapper()
    }
}

private class KtlintRuleWrapperConfig {
    private lateinit var _ktlintRuleEngineProvider: KtlintRuleEngineProvider

    val ktlintRuleEngineProvider: KtlintRuleEngineProvider
        get() = _ktlintRuleEngineProvider

    private lateinit var baselineProvider: BaselineProvider

    private lateinit var ktlintPluginsPropertiesReader: KtlintPluginsPropertiesReader

    init {
        reset(null)
    }

    fun reset(project: Project?) {
        // Ktlint has a static cache which is shared across all instances of the KtlintRuleEngine. Creates a new KtlintRuleEngine to load
        // changes in the editorconfig is therefore not sufficient. The memory needs to be cleared explicitly.
        if (::_ktlintRuleEngineProvider.isInitialized) {
            _ktlintRuleEngineProvider.ktlintRuleEngine.trimMemory()
        }

        _ktlintRuleEngineProvider = KtlintRuleEngineProvider()
        baselineProvider = BaselineProvider()
        ktlintPluginsPropertiesReader = KtlintPluginsPropertiesReader()
        if (project != null) {
            configure(project)
        }
    }

    fun configure(project: Project): KtlintRuleWrapperConfig =
        apply {
            with(project.config()) {
                baselineProvider.configure(baselinePath)
                ktlintPluginsPropertiesReader.configure(project)
                _ktlintRuleEngineProvider.configure(ktlintRulesetVersion(), externalJarPaths)
            }
        }

    private fun KtlintProjectSettings.ktlintRulesetVersion() =
        ktlintRulesetVersionFromSharedPropertiesFile()
            ?: ktlintRulesetVersionFromKtlintConfiguration()
            ?: defaultKtlintRulesetVersion()

    private fun ktlintRulesetVersionFromSharedPropertiesFile() =
        ktlintPluginsPropertiesReader
            .ktlintRulesetVersion()
            ?.also { logger.debug { "Use Ktlint version $it defined in property shared by all ktlint plugins" } }

    private fun KtlintProjectSettings.ktlintRulesetVersionFromKtlintConfiguration() =
        ktlintRulesetVersion
            ?.also { logger.debug { "Use Ktlint version $it defined in ktlint-intellij-plugin configuration" } }

    private fun defaultKtlintRulesetVersion() =
        KtlintRulesetVersion
            .DEFAULT
            .also { logger.debug { "Use default Ktlint version $it as ktlint-intellij-plugin configuration is not found" } }

    fun ktlintPluginsPropertiesReader(project: Project): KtlintPluginsPropertiesReader {
        configure(project)
        return ktlintPluginsPropertiesReader
    }

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

    private lateinit var _ktlintRuleEngine: KtLintRuleEngine

    val ktlintRuleEngine: KtLintRuleEngine
        get() = _ktlintRuleEngine

    fun configure(
        ktlintRulesetVersion: KtlintRulesetVersion,
        externalJarPaths: List<String>,
    ) {
        if (!::ruleSetProviders.isInitialized ||
            ruleSetProviders.ktlintRulesetVersion != ktlintRulesetVersion ||
            ruleSetProviders.externalJarPaths != externalJarPaths
        ) {
            logger.info("Configure KtlintRuleEngineWrapper $ktlintRulesetVersion, $externalJarPaths")
            ruleSetProviders = RuleSetProviders(ktlintRulesetVersion, externalJarPaths)

            ruleSetProviders
                .ruleProviders
                ?.let { ruleProviders ->

                    _ktlintRuleEngine =
                        KtLintRuleEngine(
                            editorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
                            ruleProviders = ruleProviders,
                        )
                }
        }
    }

    fun hasErrorLoadingExternalRulesetJar(): Boolean = errorLoadingExternalRulesetJar() != null

    fun errorLoadingExternalRulesetJar(): String? = ruleSetProviders.takeIf { it.isLoaded }?.error
}

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
