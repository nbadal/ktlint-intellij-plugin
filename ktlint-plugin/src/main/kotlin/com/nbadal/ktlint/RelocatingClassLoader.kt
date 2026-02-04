package com.nbadal.ktlint

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.commons.ClassRemapper
import org.jetbrains.org.objectweb.asm.commons.Remapper
import java.net.URL
import java.net.URLClassLoader

// Rewrites bytecode references to relocated packages when loading classes from custom ruleset JARs.
// Custom rulesets reference non-relocated packages (e.g. `org.jetbrains.kotlin`), but ktlint-lib relocates
// these to `shadow.*` via ShadowJar. This uses ASM's ClassRemapper to transform references at load time.
// See the "Transforming classes" chapter of the ASM User Guide: https://asm.ow2.io/asm4-guide.pdf
// See also: https://asm.ow2.io/javadoc/org/objectweb/asm/commons/ClassRemapper.html
class RelocatingClassLoader(
    urls: Array<out URL>,
    parent: ClassLoader,
) : URLClassLoader(urls, parent) {
    override fun findClass(name: String): Class<*> {
        val inputStream = getResourceAsStream(name.replace('.', '/') + ".class") ?: throw ClassNotFoundException(name)

        return inputStream.use { stream ->
            val transformedBytecode = transformBytecode(stream.readBytes())
            defineClass(name, transformedBytecode, 0, transformedBytecode.size)
        }
    }

    private fun transformBytecode(bytecode: ByteArray): ByteArray {
        val reader = ClassReader(bytecode)
        val writer = ClassWriter(reader, 0)
        reader.accept(ClassRemapper(writer, remapper), 0)
        return writer.toByteArray()
    }

    private companion object {
        val remapper =
            object : Remapper() {
                override fun map(internalName: String): String =
                    if (isRelocated(internalName)) {
                        // IMPORTANT: The prefix here must match the `ShadowJar` relocation target in ktlint-lib/build.gradle.kts
                        "shadow/$internalName"
                    } else {
                        internalName
                    }

                private fun isRelocated(internalName: String) = relocatedPrefixes.any { internalName.startsWith(it) }

                // IMPORTANT: This must be kept in sync with the `ShadowJar` task in ktlint-lib/build.gradle.kts
                private val relocatedPrefixes =
                    listOf(
                        "org/jetbrains/kotlin",
                        "org/jetbrains/org",
                        "org/jetbrains/concurrency",
                    )
            }
    }
}
