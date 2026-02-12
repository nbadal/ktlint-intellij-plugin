package com.nbadal.ktlint.plugin

import com.intellij.openapi.project.Project
import com.nbadal.ktlint.RelocatingClassLoader
import com.nbadal.ktlint.connector.KtlintConnector
import com.nbadal.ktlint.connector.KtlintVersion
import java.io.File
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

private val logger = KtlintLogger()

class ProjectWrapper private constructor() {
    private lateinit var project: Project
    private var baselineProvider = BaselineProvider()
    private var ktlintPluginsPropertiesReader = KtlintPluginsPropertiesReader()

    private fun updateProject(project: Project): KtlintConnector {
        if (::project.isInitialized && this.project != project) {
            // Ktlint has a static cache which is shared across all instances of the KtlintRuleEngine. Creating a new KtlintRuleEngine
            // to load changes in the editorconfig is therefore not sufficient. The memory needs to be cleared explicitly.
            ktlintConnector.trimMemory()
        }
        this.project = project
        baselineProvider.setProject(project)
        ktlintPluginsPropertiesReader.setProject(project)

        return ktlintConnector
            .apply {
                with(project.config()) {
                    loadRulesets(ktlintVersionFromSharedPropertiesOrKtlintConfiguration())
                    loadExternalRulesetJars(externalJarPaths)
                }
            }
    }

    private val ktlintConnector: KtlintConnector by lazy {
        with(KtlintConnector::class.java) {
            // See: https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#classes-from-plugin-dependencies
            val thread = Thread.currentThread()
            val prevLoader = thread.getContextClassLoader()
            try {
                val loader = ProjectWrapper::class.java.classLoader
                thread.contextClassLoader = loader
                val url = File("jar:ktlint-lib.jar").toUrlArray()
                // The urlClassLoaderFactory delegates the construction of a RelocatingClassLoader to the context of the "ktlint-plugin"
                // module, as this class should use the Intellij IDEA version of [org.jetbrains.org.objectweb.asm.commons.ClassRemapper],
                // and not the version provided via KtLint.
                ServiceLoader
                    .load(this, RelocatingClassLoader(url, loader))
                    .toSet()
                    .singleOrNull()
                    ?.apply {
                        setUrlClassLoaderFactory(
                            { urls, loader -> RelocatingClassLoader(urls, loader) },
                        )
                    } ?: throw IllegalStateException("Cannot load KtlintConnector from 'ktlint-lib.jar'")
            } catch (e: ServiceConfigurationError) {
                logger.warn(e) { "Could not load the KtlintConnector" }
                throw e
            } finally {
                // Restore original classloader
                thread.contextClassLoader = prevLoader
            }
        }
    }

    private fun File.toUrlArray() = arrayOf(toURI().toURL())

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

    fun ktlintConnector(project: Project?): KtlintConnector =
        if (project == null) {
            ktlintConnector
        } else {
            updateProject(project)
        }

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
