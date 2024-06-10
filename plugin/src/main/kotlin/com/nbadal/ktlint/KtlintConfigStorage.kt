package com.nbadal.ktlint

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

private val logger = KtlintLogger(KtlintConfigStorage::class.qualifiedName)

@State(
    name = "KtLint plugin",
    storages = [Storage("ktlint-plugin.xml")],
)
@Service(Service.Level.PROJECT)
class KtlintConfigStorage : PersistentStateComponent<KtlintConfigStorage> {
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
    var ktlintRulesetVersion: KtlintRulesetVersion = KtlintRulesetVersion.findByLabelOrDefault("")

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
                _ruleSetProviders = RuleSetProviders(ktlintRulesetVersion, externalJarPaths)
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

    override fun getState(): KtlintConfigStorage = this

    override fun loadState(state: KtlintConfigStorage) {
        // If the ktlint mode which is actually stored is not a valid enum value, the field 'state.ktlintMode' contains a null value
        // although the field is not nullable.
        @Suppress("USELESS_ELVIS")
        this.ktlintMode = state.ktlintMode ?: NOT_INITIALIZED

        this.ktlintRulesetVersion = state.ktlintRulesetVersion
        this.formatOnSave = state.formatOnSave
        this.baselinePath = state.baselinePath
        this.externalJarPaths = state.externalJarPaths

        this._ruleSetProviders = RuleSetProviders(ktlintRulesetVersion, externalJarPaths)
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
                            "Added ${ktlintRulesetVersion.ruleProviders().size} from default ktlint ruleset version ${ktlintRulesetVersion.label}"
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
