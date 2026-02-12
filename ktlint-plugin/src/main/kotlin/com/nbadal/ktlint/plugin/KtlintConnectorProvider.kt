package com.nbadal.ktlint.plugin

import com.nbadal.ktlint.RelocatingClassLoader
import com.nbadal.ktlint.connector.KtlintConnector
import java.io.File
import java.util.ServiceConfigurationError
import java.util.ServiceLoader

private val logger = KtlintLogger()

object KtlintConnectorProvider {
    val instance =
        with(KtlintConnector::class.java) {
            // See: https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#classes-from-plugin-dependencies
            val thread = Thread.currentThread()
            val prevLoader = thread.getContextClassLoader()
            try {
                val loader = ProjectWrapper::class.java.classLoader
                thread.contextClassLoader = loader
                val url = File("jar:ktlint-lib.jar").toUrlArray()
                ServiceLoader
                    .load(this, RelocatingClassLoader(url, loader))
                    .toSet()
                    .singleOrNull()
                    ?.apply {
                        // External ruleset JAR files will be initiated from withing the context of the "ktlint-lib" module. But the actual
                        // loading of the ruleset JAR files still needs to be done in the context of the "ktlint-plugin" module. So,
                        // the urlClassLoaderFactory delegates the construction of a RelocatingClassLoader back to the context of the
                        // "ktlint-plugin" module, as this class should use the Intellij IDEA version of
                        // [org.jetbrains.org.objectweb.asm.commons.ClassRemapper], and not the version provided via KtLint.
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

    private fun File.toUrlArray() = arrayOf(toURI().toURL())
}
