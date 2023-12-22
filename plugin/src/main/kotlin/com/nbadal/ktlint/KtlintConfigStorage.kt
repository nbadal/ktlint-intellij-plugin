package com.nbadal.ktlint

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Tag
import com.nbadal.ktlint.KtlintMode.NOT_INITIALIZED
import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import java.io.File
import java.nio.file.Path

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
    var formatOnSave: Boolean = true

    @Tag
    var baselinePath: String? = null

    @Tag
    var externalJarPaths: List<String> = emptyList()

    /**
     * Keeps the state of the last loaded set of rule set jars. It serves as a cache so that the rule set providers do not need to be
     * reloaded from the file system on each invocation of ktlint format.
     */
    private var _ruleSetProviders: RuleSetProviders? = null

    val ruleSetProviders: RuleSetProviders
        get() =
            _ruleSetProviders
                ?.takeIf { it.externalJarPaths == externalJarPaths }
                ?: RuleSetProviders(externalJarPaths).also { _ruleSetProviders = it }

    /**
     * Keeps the state of the last loaded baseline. It serves as a cache so that the baseline does not need to be reloaded from the file
     * system on each invocation of ktlint format.
     */
    private var _baseline: Baseline? = null

    val baseline: Baseline
        get() =
            _baseline
                ?.takeIf { it.baselinePath == baselinePath }
                ?: Baseline(baselinePath).also { _baseline = it }

    private var ktlintRuleEngine: KtLintRuleEngine? = null

    fun ktlintRuleEngine() =
        ktlintRuleEngine
            ?: ruleSetProviders
                .ruleProviders
                ?.let { ruleProviders ->
                    KtLintRuleEngine(
                        editorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
                        ruleProviders = ruleProviders,
                    )
                }.also { ktlintRuleEngine = it }

    /**
     * Clears the ".editorconfig" cache so that it gets reloaded. This should only be called after saving a modified ".editorconfig".
     */
    fun resetKtlintRuleEngine() = ktlintRuleEngine?.trimMemory()

    override fun getState(): KtlintConfigStorage = this

    override fun loadState(state: KtlintConfigStorage) {
        // If the ktlint mode which is actually stored is not a valid enum value, the field 'state.ktlintMode' contains a null value
        // although the field is not nullable.
        @Suppress("USELESS_ELVIS")
        this.ktlintMode = state.ktlintMode ?: NOT_INITIALIZED

        this.formatOnSave = state.formatOnSave
        this.baselinePath = state.baselinePath
        this.externalJarPaths = state.externalJarPaths
    }

    data class RuleSetProviders(
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
                    .loadRuleProviders()
            } catch (throwable: Throwable) {
                _isLoaded = false
                _error = throwable.toString()
                null
            }
    }

    data class Baseline(
        val baselinePath: String?,
    ) {
        val lintErrorsPerFile: Map<String, List<KtlintCliError>> =
            baselinePath
                ?.let { path ->
                    loadBaseline(path)
                        .lintErrorsPerFile
                        .also { logger.debug { "Load baseline from file '$path'" } }
                }
                ?: emptyMap<String, List<KtlintCliError>>().also { logger.debug { "Clear baseline" } }
    }
}
