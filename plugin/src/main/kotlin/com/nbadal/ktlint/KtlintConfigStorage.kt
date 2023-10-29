package com.nbadal.ktlint

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Tag
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.NOT_INITIALIZED

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
    var baselinePath: String? = null

    @Tag
    var externalJarPaths: List<String> = emptyList()

    override fun getState(): KtlintConfigStorage = this

    override fun loadState(state: KtlintConfigStorage) {
        this.ktlintMode = state.ktlintMode
        this.baselinePath = state.baselinePath
        this.externalJarPaths = state.externalJarPaths
    }

    enum class KtlintMode {
        /**
         * Ktlint plugin settings have not yet been saved for this project. Ktlint may only be run in Lint mode and it
         * should ask the developer to make a choice to enable or disable Ktlint.
         */
        NOT_INITIALIZED,

        /**
         * Ktlint is fully enabled for the project. Source code will be formatted.
         */
        ENABLED,

        /**
         * Ktlint is fully disabled for the project. Neither lint nor format will run.
         */
        DISABLED,
    }
}
