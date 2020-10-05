package com.nbadal.ktlint

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Tag

@State(
    name = "KtlintProjectConfiguration",
    storages = [Storage("ktlint.xml")]
)
class KtlintConfigStorage : PersistentStateComponent<KtlintConfigStorage> {

    @Tag
    var enableKtlint = true

    @Tag
    var androidMode = false

    @Tag
    var useExperimental = false

    @Tag
    var treatAsErrors = true

    @Tag
    var disabledRules: List<String> = emptyList()

    @Tag
    var editorConfigPath: String? = null

    @Tag
    var externalJarPaths: List<String> = emptyList()

    override fun getState(): KtlintConfigStorage = this

    override fun loadState(state: KtlintConfigStorage) {
        this.enableKtlint = state.enableKtlint
        this.androidMode = state.androidMode
        this.useExperimental = state.useExperimental
        this.treatAsErrors = state.treatAsErrors
        this.disabledRules = state.disabledRules
        this.editorConfigPath = state.editorConfigPath
        this.externalJarPaths = state.externalJarPaths
    }

    companion object {
        fun instance(project: Project) = ServiceManager.getService(project, KtlintConfigStorage::class.java)!!
    }
}
