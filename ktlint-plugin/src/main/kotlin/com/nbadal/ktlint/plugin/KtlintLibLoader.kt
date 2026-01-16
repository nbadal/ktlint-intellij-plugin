package com.nbadal.ktlint.plugin

// private fun <T> Class<T>.loadProvidersFromJars(url: URL?): Set<T> {
//    // See: https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html#classes-from-plugin-dependencies
//    val thread = Thread.currentThread()
//    val prevLoader = thread.getContextClassLoader()
//    try {
//        val loader = FormatAction::class.java.classLoader
//        thread.contextClassLoader = loader
//        return try {
//            ServiceLoader.load(this, URLClassLoader(url.toArray(), loader)).toSet()
//        } catch (e: ServiceConfigurationError) {
//            emptySet()
//        }
//    } finally {
//        // Restore original classloader
//        thread.contextClassLoader = prevLoader
//    }
// }
