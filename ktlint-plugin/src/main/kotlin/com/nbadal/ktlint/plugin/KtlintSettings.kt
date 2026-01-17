package com.nbadal.ktlint.plugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Tag
import com.nbadal.ktlint.lib.KtlintRulesetVersion

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
        // those other projects, as that would result in creating a plugin settings file inside the '.idea' folder of the project.
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
    var ktlintMode: KtlintMode = KtlintMode.NOT_INITIALIZED

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

    override fun getState(): KtlintProjectSettings = this

    override fun loadState(state: KtlintProjectSettings) {
        // If the ktlint mode which is actually stored is not a valid enum value, the field 'state.ktlintMode' contains a null value
        // although the field is not nullable.
        @Suppress("USELESS_ELVIS")
        this.ktlintMode = state.ktlintMode ?: KtlintMode.NOT_INITIALIZED

        this.ktlintRulesetVersion = state.ktlintRulesetVersion
        this.formatOnSave = state.formatOnSave
        this.baselinePath = state.baselinePath
        this.externalJarPaths = state.externalJarPaths
    }
}
