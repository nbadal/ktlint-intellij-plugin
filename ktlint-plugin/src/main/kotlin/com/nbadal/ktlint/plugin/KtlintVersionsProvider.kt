package com.nbadal.ktlint.plugin

import com.nbadal.ktlint.connector.KtlintVersionsConnector
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

private val logger = KtlintLogger()

class KtlintVersionsProvider {
    companion object {
        val instance by lazy {
            with(KtlintVersionsConnector::class.java) {
                // See: https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#classes-from-plugin-dependencies
                val thread = Thread.currentThread()
                val prevLoader = thread.getContextClassLoader()
                try {
                    // val loader = ClassLoader.getSystemClassLoader() // FAILS
                    val loader = KtlintVersionsProvider::class.java.classLoader
                    thread.contextClassLoader = loader
                    val url = File("jar:ktlint-lib.jar").toUrlArray()
                    ServiceLoader
                        // The RelocatingClassLoader is not needed here. Or, at least it is not invoked in the same way as when loading an
                        // external ruleset (e.g. compose ruleset)
                        // .load(this, RelocatingClassLoader(url, loader))
                        .load(this, URLClassLoader(url, loader))
                        .toSet()
                        .singleOrNull()
                        ?: throw IllegalStateException("Cannot load KtlintConnector from 'ktlint-lib.jar'")
                } catch (e: ServiceConfigurationError) {
                    logger.error(e) { "Failed to load KtlintVersionsProvider" }
                    throw e
                } finally {
                    // Restore original classloader
                    thread.contextClassLoader = prevLoader
                }
            }
        }

        private fun File.toUrlArray() = arrayOf(toURI().toURL())
    }
}
