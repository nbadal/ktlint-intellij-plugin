package com.nbadal.ktlint

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Tag
import com.nbadal.ktlint.KtlintMode.NOT_INITIALIZED
import com.pinterest.ktlint.cli.reporter.baseline.BaselineErrorHandling
import com.pinterest.ktlint.cli.reporter.baseline.BaselineLoaderException
import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.ruleset.standard.KtlintRulesetVersion
import java.io.File

private val logger = KtlintLogger(KtlintProjectSettings::class.qualifiedName)

/**
 * Application wide configuration settings. Those settings are stored in a file  outside the '.idea' folder of the project. Those settings
 * apply to all projects. In this way the user does not have to set the setting for each project.
 */
@Service(Service.Level.APP)
@State(
    name = "com.nbadal.ktlint.KtlintApplicationSettings",
    // Application wide ktlint settings are stored in a file outside the '.idea' folder of the project.
    storages = [Storage("ktlint-plugin.xml")],
)
class KtlintApplicationSettings : PersistentStateComponent<KtlintApplicationSettings.State> {
    data class State(
        // Some users work on projects for which only a subselection is using ktlint. They do not want to see the ktlint banner advocating
        // to configure the ktlint project in the project that are not using ktlint. Neither do they want to disable ktlint explicitly in
        // those other projects, as that would result in creating a plugin settings file inside hte '.idea' folder of the project.
        var showBanner: Boolean = true,
    )

    private var _state = State()

    override fun getState(): State = _state

    override fun loadState(state: State) {
        _state = state
    }

    companion object {
        fun getInstance(): KtlintApplicationSettings = ApplicationManager.getApplication().getService(KtlintApplicationSettings::class.java)
    }
}

/**
 * Project specific configuration settings. Those settings are stored inside the '.idea' folder of the project. Those settings apply to
 * all projects. In this way the user does not have to set the setting for each project.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "com.nbadal.ktlint.KtlintProjectSettings",
    // Project specific application settings are stored in a file inside the '.idea' folder of the project.
    storages = [Storage("ktlint-plugin.xml")],
)
class KtlintProjectSettings : PersistentStateComponent<KtlintProjectSettings> {
    /**
     * The plugin can not detect whether ktlint should run or not on a newly loaded project that contains Kotlin code.
     * Enabling the plugin by default on all projects including formatting of code may be not acceptable in some
     * projects. On the other hand, if the plugin has to be enabled manually on each project, it may be forgotten to do
     * so. The default status [KtlintMode.NOT_INITIALIZED] asks the developer to either enabled or disable Ktlint for
     * this project.
     */
    @Tag
    var ktlintMode: KtlintMode = NOT_INITIALIZED

    @Tag
    var ktlintRulesetVersion: KtlintRulesetVersion? = null

    @Tag
    var formatOnSave: Boolean = true

    @Tag
    var attachToIntellijFormat: Boolean = true

    @Tag
    var baselinePath: String? = null

    @Tag
    var externalJarPaths: List<String> = emptyList()

    /**
     * Keeps the state of the last loaded set of rule set jars. It serves as a cache so that the rule set providers do not need to be
     * reloaded from the file system on each invocation of ktlint format.
     */
    private lateinit var _ruleSetProviders: RuleSetProviders

    val ruleSetProviders: RuleSetProviders
        get() {
            if (!::_ruleSetProviders.isInitialized ||
                _ruleSetProviders.ktlintRulesetVersion != ktlintRulesetVersion ||
                _ruleSetProviders.externalJarPaths != externalJarPaths
            ) {
                _ruleSetProviders = RuleSetProviders(ktlintRulesetVersion ?: KtlintRulesetVersion.DEFAULT, externalJarPaths)
                _ktlintRuleEngine = null
            }
            return _ruleSetProviders
        }

    val ruleIdsWithAutocorrectApproveHandler: Set<RuleId>
        get() =
            ruleSetProviders
                .ruleProviders
                .orEmpty()
                .map { it.createNewRuleInstance() }
                .filter { it is RuleAutocorrectApproveHandler }
                .map { it.ruleId }
                .toSet()

    /**
     * Keeps the state of the last loaded baseline. It serves as a cache so that the baseline does not need to be reloaded from the file
     * system on each invocation of ktlint format.
     */
    private var baseline: Baseline? = null

    private var _ktlintRuleEngine: KtLintRuleEngine? = null

    val ktlintRuleEngine: KtLintRuleEngine?
        get() {
            if (_ktlintRuleEngine == null ||
                _ruleSetProviders.ktlintRulesetVersion != ktlintRulesetVersion ||
                _ruleSetProviders.externalJarPaths != externalJarPaths
            ) {
                _ruleSetProviders
                    .ruleProviders
                    ?.let { ruleProviders ->
                        _ktlintRuleEngine =
                            KtLintRuleEngine(
                                editorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
                                ruleProviders = ruleProviders,
                            )
                    }
            }
            return _ktlintRuleEngine
        }

    /**
     * Clears the ".editorconfig" cache so that it gets reloaded. This should only be called after saving a modified ".editorconfig".
     */
    fun resetKtlintRuleEngine() = _ktlintRuleEngine?.trimMemory()

    override fun getState(): KtlintProjectSettings = this

    override fun loadState(state: KtlintProjectSettings) {
        // If the ktlint mode which is actually stored is not a valid enum value, the field 'state.ktlintMode' contains a null value
        // although the field is not nullable.
        @Suppress("USELESS_ELVIS")
        this.ktlintMode = state.ktlintMode ?: NOT_INITIALIZED

        this.ktlintRulesetVersion = state.ktlintRulesetVersion
        this.formatOnSave = state.formatOnSave
        this.baselinePath = state.baselinePath
        this.externalJarPaths = state.externalJarPaths

        reloadRuleSetProviders()
    }

    fun reloadRuleSetProviders() {
        _ruleSetProviders = RuleSetProviders(ktlintRulesetVersion ?: KtlintRulesetVersion.DEFAULT, externalJarPaths)
        _ktlintRuleEngine = null
    }

    data class RuleSetProviders(
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

    fun getBaselineErrors(
        project: Project,
        filePath: String,
    ): List<KtlintCliError> {
        if (baseline?.baselinePath != baselinePath) {
            baseline = project.loadBaseline()
        }
        return baseline
            ?.lintErrorsPerFile
            ?.get(filePath)
            ?: emptyList()
    }

    private fun Project.loadBaseline() =
        baselinePath
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->
                try {
                    Baseline(
                        baselinePath = baselinePath,
                        loadBaseline(path, BaselineErrorHandling.EXCEPTION)
                            .lintErrorsPerFile
                            .also { logger.debug { "Load baseline from file '$path'" } },
                    )
                } catch (e: BaselineLoaderException) {
                    // The exception message produced by ktlint already contains sufficient context of the error
                    val message = e.message ?: "Exception while loading baseline file '$baselinePath'"
                    KtlintNotifier.notifyError(
                        project = this,
                        title = "Loading baseline",
                        message = message,
                        forceSettingsDialog = true,
                    )
                    logger.debug(e) { message }
                    Baseline(baselinePath, emptyMap())
                }
            }
            ?: Baseline(baselinePath, emptyMap())
}
