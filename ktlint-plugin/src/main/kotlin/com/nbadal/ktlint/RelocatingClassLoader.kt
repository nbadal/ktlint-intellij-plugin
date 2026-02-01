package com.nbadal.ktlint

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.commons.ClassRemapper
import org.jetbrains.org.objectweb.asm.commons.Remapper
import java.net.URL
import java.net.URLClassLoader

class RelocatingClassLoader(
    urls: Array<out URL>,
    parent: ClassLoader,
) : URLClassLoader(urls, parent) {
    private val remapper = PackageRemapper()

    override fun findClass(name: String): Class<*> {
        val resourcePath = name.replace('.', '/') + ".class"
        val inputStream = getResourceAsStream(resourcePath) ?: throw ClassNotFoundException(name)

        return inputStream.use { stream ->
            val originalBytecode = stream.readBytes()
            val transformedBytecode = transformBytecode(originalBytecode)
            defineClass(name, transformedBytecode, 0, transformedBytecode.size)
        }
    }

    private fun transformBytecode(bytecode: ByteArray): ByteArray {
        val reader = ClassReader(bytecode)
        val writer = ClassWriter(reader, 0)
        val classRemapper = ClassRemapper(writer, remapper)
        reader.accept(classRemapper, 0)
        return writer.toByteArray()
    }

    private class PackageRemapper : Remapper() {
        private val relocatedPrefixes =
            listOf(
                "org/jetbrains/kotlin",
                "org/jetbrains/org",
                "org/jetbrains/concurrency",
            )

        override fun map(internalName: String): String =
            if (relocatedPrefixes.any { internalName.startsWith(it) }) {
                "shadow/$internalName"
            } else {
                internalName
            }
    }
}
