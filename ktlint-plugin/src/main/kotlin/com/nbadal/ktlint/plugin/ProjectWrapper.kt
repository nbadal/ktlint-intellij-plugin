package com.nbadal.ktlint.plugin

import com.intellij.openapi.project.Project
import com.nbadal.ktlint.connector.KtlintConnector
import com.nbadal.ktlint.connector.KtlintVersion

private val logger = KtlintLogger()

class ProjectWrapper private constructor() {
    private lateinit var project: Project
    private var baselineProvider = BaselineProvider()
    private var ktlintPluginsPropertiesReader = KtlintPluginsPropertiesReader()

    private fun updateProject(project: Project): KtlintConnector {
        if (::project.isInitialized && this.project != project) {
            // Ktlint has a static cache which is shared across all instances of the KtlintRuleEngine. Creating a new KtlintRuleEngine
            // to load changes in the editorconfig is therefore not sufficient. The memory needs to be cleared explicitly.
            KtlintConnectorProvider.instance.trimMemory()
        }
        this.project = project
        baselineProvider.setProject(project)
        ktlintPluginsPropertiesReader.setProject(project)

        return KtlintConnectorProvider
            .instance
            .apply {
                with(project.config()) {
                    loadRulesets(ktlintVersionFromSharedPropertiesOrKtlintConfiguration())
                    loadExternalRulesetJars(externalJarPaths)
                }
            }
    }

    private fun KtlintProjectSettings.ktlintVersionFromSharedPropertiesOrKtlintConfiguration() =
        ktlintRulesetVersionFromSharedPropertiesFile()
            ?: ktlintRulesetVersionFromKtlintConfiguration()
            ?: defaultKtlintRulesetVersion()

    private fun ktlintRulesetVersionFromSharedPropertiesFile() =
        ktlintPluginsPropertiesReader
            .ktlintVersion
            ?.also { logger.debug { "Use Ktlint version $it defined in property shared by all ktlint plugins" } }

    private fun KtlintProjectSettings.ktlintRulesetVersionFromKtlintConfiguration() =
        ktlintVersion()
            ?.also { logger.debug { "Use Ktlint version $it defined in ktlint-intellij-plugin configuration" } }

    private fun defaultKtlintRulesetVersion() =
        KtlintVersion
            .DEFAULT
            .also { logger.debug { "Use default Ktlint version $it as ktlint-intellij-plugin configuration is not found" } }

    fun ktlintConnector(project: Project): KtlintConnector = updateProject(project)

    fun ktlintPluginsPropertiesReader(project: Project): KtlintPluginsPropertiesReader {
        updateProject(project)
        return ktlintPluginsPropertiesReader
    }

    fun baselineProvider(project: Project): BaselineProvider {
        updateProject(project)
        return baselineProvider
    }

    companion object {
        val instance = ProjectWrapper()
    }
}

fun Project.updateProjectWrapper() {
    ProjectWrapper.instance.ktlintConnector(this)
}

fun Project.ktlintConnector(): KtlintConnector = ProjectWrapper.instance.ktlintConnector(this)

fun Project.ktlintPluginsPropertiesReader(): KtlintPluginsPropertiesReader = ProjectWrapper.instance.ktlintPluginsPropertiesReader(this)

fun Project.baselineProvider(): BaselineProvider = ProjectWrapper.instance.baselineProvider(this)
