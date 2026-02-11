package com.nbadal.ktlint.plugin

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
import com.nbadal.ktlint.lib.AutocorrectDecision
import com.nbadal.ktlint.lib.BaselineError
import com.nbadal.ktlint.lib.Code
import com.nbadal.ktlint.lib.KtlintConnector
import com.nbadal.ktlint.lib.KtlintEditorConfigOptionDescriptor
import com.nbadal.ktlint.lib.KtlintVersion
import com.nbadal.ktlint.lib.LintError
import com.nbadal.ktlint.lib.SuppressionAtOffset
import com.nbadal.ktlint.plugin.KtlintRuleEngineWrapper.KtlintVersionConfiguration.Location
import org.ec4j.core.parser.ParseException
import java.nio.file.Path

private val logger = KtlintLogger()

internal class KtlintRuleEngineWrapper internal constructor() {
    private lateinit var project: Project

    fun lint(
        psiFile: PsiFile,
        triggeredBy: String,
    ) = if (psiFile.virtualFile.isKotlinFile()) {
        executeKtlint(KtlintExecutionType.LINT, psiFile, KtlintFileAutocorrectHandler, triggeredBy)
    } else {
        KtlintResult(KtlintResult.Status.NOT_STARTED)
    }

    fun format(
        psiFile: PsiFile,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
        forceFormat: Boolean = false,
    ): KtlintResult {
        var ktlintResult = KtlintResult(KtlintResult.Status.NOT_STARTED)
        if (psiFile.virtualFile.isKotlinFile() &&
            (psiFile.project.config().ktlintMode == KtlintMode.DISTRACT_FREE || forceFormat)
        ) {
            val project = psiFile.project
            val document = psiFile.viewProvider.document
            PsiDocumentManager
                .getInstance(project)
                .doPostponedOperationsAndUnblockDocument(document)
            WriteCommandAction.runWriteCommandAction(project) {
                ktlintResult = executeKtlint(KtlintExecutionType.FORMAT, psiFile, ktlintFormatAutoCorrectHandler, triggeredBy)
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
            .filter { it.isKotlinFile() }
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

        val ktlintConnector = psiFile.project.ktlintConnector()

        ktlintConnector
            .loadExternalRulesetJars(externalJarPaths = psiFile.project.config().externalJarPaths)
            .takeIf { it.isNotEmpty() }
            ?.let { errors ->
                // Report the external ruleset that was not loaded successfully. But continue with format of file with the rule providers
                // that have been loaded successfully.
                KtlintNotifier
                    .notifyError(
                        notificationGroup = KtlintNotifier.KtlintNotificationGroup.RULE,
                        project = psiFile.project,
                        title = "Invalid external ruleset JAR",
                        message = errors.joinToString(separator = "\n"),
                    ) {
                        addAction(OpenSettingsAction(psiFile.project))
                    }
                // Do not prevent formatting of file with the rulesets that were loaded successfully.
            }

        val baselineErrors =
            psiFile
                .project
                .baselineProvider()
                .baselineErrors(psiFile)
        val lintErrors = mutableListOf<LintError>()
        var fileChangedByFormat = false
        // The psiFile may contain unsaved changes. Get the content via the PsiDocumentManager instead of from "psiFile.text" directly. In case
        // the content of an active editor window is changed via a global find and replace, the document text is updated but the Psi (and
        // PsiFile) have not yet been changed. Add the virtual path based on path of the psiFile so that the correct '.editorconfig' is picked
        // up by ktlint.
        val code =
            Code(
                content = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile)!!.text,
                // For compatibility with unit tests the NioPath cannot be directly determined via the virtual file
                filePath = Path.of(psiFile.virtualFile.path),
            )

        try {
            if (ktlintExecutionType == KtlintExecutionType.LINT) {
                ktlintConnector.lint(code) { lintError -> lintErrors.add(lintError) }
            } else {
                val formattedCode =
                    when (ktlintFormatAutoCorrectHandler) {
                        is KtlintFileAutocorrectHandler -> {
                            ktlintConnector.format(code) { lintError ->
                                if (baselineErrors.contains(lintError)) {
                                    AutocorrectDecision.NO_AUTOCORRECT
                                } else {
                                    lintErrors.add(lintError)
                                    AutocorrectDecision.ALLOW_AUTOCORRECT
                                }
                            }
                        }

                        is KtlintBlockAutocorrectHandler -> {
                            ktlintConnector.format(code) { lintError ->
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
                            ktlintConnector.format(code) { lintError ->
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
                KtlintResult.Status.SUCCESS,
                lintErrors.filterNot { baselineErrors.contains(it) },
                fileChangedByFormat,
            )
        } catch (_: KtlintConnector.ParseException) {
            // ParseException occur very frequently while typing code, and a save operation is executed while code cannot be compiled at
            // that moment. Display a notification is distracting, and not helpful.
            return KtlintResult(KtlintResult.Status.FILE_RELATED_ERROR)
        } catch (ktLintRuleException: KtlintConnector.RuleException) {
            KtlintNotifier.notifyError(
                notificationGroup = KtlintNotifier.KtlintNotificationGroup.RULE,
                project = psiFile.project,
                title = "KtLintRuleException",
                message =
                    """
                    An error occurred in a rule. Please see stacktrace below for rule that caused the problem and contact maintainer of the
                    rule when the error can be reproduced.
                    ${ktLintRuleException.stackTraceToString()}
                    """.trimIndent(),
            )
            return KtlintResult(KtlintResult.Status.FILE_RELATED_ERROR)
        } catch (parseException: ParseException) {
            KtlintNotifier.notifyError(
                notificationGroup = KtlintNotifier.KtlintNotificationGroup.DEFAULT,
                project = psiFile.project,
                title = "Invalid editorconfig",
                // The exception message already contains the path to the file, so don't repeat it
                message =
                    """
                    An error occurred while reading the '.editorconfig':
                    ${parseException.message}
                    """.trimIndent(),
            )
            return KtlintResult(KtlintResult.Status.FILE_RELATED_ERROR)
        } catch (exception: Exception) {
            if (exception is IllegalStateException && exception.message?.startsWith("Skipping rule(s)") == true) {
                KtlintNotifier.notifyError(
                    notificationGroup = KtlintNotifier.KtlintNotificationGroup.CONFIGURATION,
                    project = psiFile.project,
                    title = "Invalid editorconfig configuration",
                    message = exception.message!!,
                )
            } else {
                KtlintNotifier.notifyError(
                    notificationGroup = KtlintNotifier.KtlintNotificationGroup.DEFAULT,
                    project = psiFile.project,
                    title = "Uncategorized error",
                    message =
                        """
                        An error occurred while processing file '${psiFile.virtualFile.path}':
                        ${exception.stackTraceToString()}
                        """.trimIndent(),
                )
            }
            return KtlintResult(KtlintResult.Status.FILE_RELATED_ERROR)
        }
    }

    fun insertSuppression(
        psiFile: PsiFile,
        code: Code,
        suppressionAtOffset: SuppressionAtOffset,
    ): String =
        psiFile
            .project
            .ktlintConnector()
            .insertSuppression(code, suppressionAtOffset)

    fun ktlintVersionConfiguration(project: Project) =
        project
            .ktlintPluginsPropertiesReader()
            .ktlintVersion
            ?.let { ktlintVersion -> KtlintVersionConfiguration(ktlintVersion, Location.SHARED_PLUGIN_PROPERTIES) }
            ?: KtlintVersionConfiguration(project.config().ktlintVersion() ?: KtlintVersion.DEFAULT, Location.NATIVE_PLUGIN_CONFIGURATION)

    /**
     * Ensure that the KtlintConnector is up to date with the project for which the current file is being edited.
     */
    fun reset(project: Project) {
        if (!::project.isInitialized || this.project != project) {
            this.project = project
            logger.debug { "Reset KtlintRuleEngineWrapper because (another) project is selected" }
            project.updateProjectWrapper()
            project.resetKtlintAnnotatorUserData()
        }
    }

    fun getEditorConfigOptionDescriptors(project: Project): List<KtlintEditorConfigOptionDescriptor> =
        project
            .ktlintConnector()
            .getEditorConfigOptionDescriptors()

    internal data class KtlintVersionConfiguration(
        val ktlintVersion: KtlintVersion,
        val location: Location,
    ) {
        enum class Location {
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
             * Ktlint can not run due to error related to the file.
             */
            FILE_RELATED_ERROR,
        }
    }

    companion object {
        val instance = KtlintRuleEngineWrapper()
    }
}

/**
 * Keeps the state of loaded baseline. It serves as a cache so that the baseline does not need to be reloaded from the file system on each
 * invocation of ktlint format.
 */
class BaselineProvider {
    private var baselinePath: String? = null
    private var error: String? = null
    private var baselineErrors: List<BaselineError> = emptyList()

    fun setProject(project: Project) {
        val baselinePath = project.config().baselinePath
        if (baselinePath != this.baselinePath) {
            this.baselinePath = project.config().baselinePath
            error = null
            baselineErrors = emptyList()
            if (baselinePath != null) {
                try {
                    baselineErrors =
                        project
                            .ktlintConnector()
                            .loadBaselineErrorsToIgnore(baselinePath)
                            .also { logger.debug { "Load baseline from file '$baselinePath'" } }
                } catch (e: KtlintConnector.BaselineLoadingException) {
                    logger.debug(e) { error }
                }
            }
        }
    }

    fun baselineErrors(psiFile: PsiFile): List<BaselineError> {
        error?.run {
            KtlintNotifier.notifyError(
                notificationGroup = KtlintNotifier.KtlintNotificationGroup.CONFIGURATION,
                project = psiFile.project,
                title = "Loading baseline",
                message = this,
            ) {
                addAction(OpenSettingsAction(psiFile.project))
            }
            return emptyList()
        }

        return psiFile
            .pathRelativeToProjectBase()
            .let { pathRelativeTo -> baselineErrors.filter { it.filePath == pathRelativeTo } }
    }

    private fun PsiFile.pathRelativeToProjectBase(): String = virtualFile.path.pathRelativeTo(project.basePath)

    private fun String.pathRelativeTo(projectBasePath: String?): String =
        if (projectBasePath.isNullOrBlank()) {
            this
        } else {
            removePrefix(projectBasePath).removePrefix("/")
        }
}

fun List<BaselineError>.contains(lintError: LintError): Boolean =
    any { baselineError ->
        with(lintError) {
            baselineError.line == line && baselineError.col == col && baselineError.ruleId == ruleId.value
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
