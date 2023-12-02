package com.nbadal.ktlint

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Tag
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.NOT_INITIALIZED
import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.propertyTypes
import java.io.File
import java.nio.file.Path

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
     * Keeps the state of the last loaded set of rule set jars. It serves as a cache so that the rule set providers do
     * not need to be reloaded from the file system on each invocation of ktlint format.
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

    private var filePath: Path? = null

    private var ktlintRuleEngine: KtLintRuleEngine? = null

    /**
     * Gets the KtlintRuleEngine for given [filePath].
     *
     * TODO: Make independent of [filePath] in Ktlint 1.1.0 as editor config defaults no longer have to be loaded in the KtLintRuleEngine
     *  when it becomes possible to create code snippet with a path.
     */
    fun ktlintRuleEngine(filePath: Path?) =
        ktlintRuleEngine
            ?.takeIf { this.filePath == filePath && filePath != null }
            ?: ruleSetProviders
                .ruleProviders
                ?.let { ruleProviders ->
                    KtLintRuleEngine(
                        editorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
                        ruleProviders = ruleProviders,
                        // TODO: remove when Code.fromSnippet takes a path as parameter in Ktlint 1.1.0.
                        //  Drawback of this method is that it ignores property "root" in '.editorconfig' file.
                        editorConfigDefaults =
                            EditorConfigDefaults.load(
                                path = filePath,
                                propertyTypes = ruleProviders.propertyTypes(),
                            ),
                    )
                }.also { ktlintRuleEngine = it }

    /**
     * Clears the ".editorconfig" cache so that it gets reloaded.
     */
    fun resetKtlintRuleEngine() = ktlintRuleEngine?.trimMemory()

    override fun getState(): KtlintConfigStorage = this

    override fun loadState(state: KtlintConfigStorage) {
        this.ktlintMode = state.ktlintMode
        this.formatOnSave = state.formatOnSave
        this.baselinePath = state.baselinePath
        this.externalJarPaths = state.externalJarPaths
    }

    enum class KtlintMode {
        /**
         * Ktlint plugin settings have not yet been saved for this project. Ktlint
         * may only be run in Lint mode, and it should ask the developer to make a
         * choice to enable or disable Ktlint.
         */
        NOT_INITIALIZED,

        /** Ktlint is fully enabled for the project. Source code will be formatted. */
        ENABLED,

        /**
         * Ktlint is fully disabled for the project. Neither lint nor format will
         * run.
         */
        DISABLED,
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
                        .also { println("Load baseline from file '$path'") }
                }
                ?: emptyMap<String, List<KtlintCliError>>().also { println("Clear baseline") }
    }
}
