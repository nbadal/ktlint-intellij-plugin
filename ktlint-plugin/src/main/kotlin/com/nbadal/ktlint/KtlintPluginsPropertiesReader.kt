package com.nbadal.ktlint

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.pinterest.ktlint.ruleset.standard.KtlintRulesetVersion
import java.nio.file.Path

const val KTLINT_PLUGINS_PROPERTIES_FILE_NAME = "ktlint-plugins.properties"
const val KTLINT_PLUGINS_VERSION_PROPERTY = "ktlint-version"

private val logger = KtlintLogger("com.nbdal.ktlint.KtlintPluginsPropertiesReader")

class KtlintPluginsPropertiesReader {
    private var properties: Map<String, String> = emptyMap()
    private var projectBasePath: String? = null
    private var readFromKtlintPluginPropertiesFile = false

    fun reset() {
        projectBasePath = null
        properties = emptyMap()
        readFromKtlintPluginPropertiesFile = false
    }

    fun configure(projectBasePath: String?) {
        if (this.projectBasePath != projectBasePath) {
            this.projectBasePath = projectBasePath
            properties =
                ktlintPluginsPropertiesVirtualFile(projectBasePath)
                    ?.readProperties()
                    ?: emptyMap()
        }
    }

    private fun ktlintPluginsPropertiesVirtualFile(projectBasePath: String?) =
        VirtualFileManager
            .getInstance()
            .findFileByNioPath(Path.of("$projectBasePath/${KTLINT_PLUGINS_PROPERTIES_FILE_NAME}"))
            .also {
                if (it == null) {
                    logger.debug("File '$KTLINT_PLUGINS_PROPERTIES_FILE_NAME' not found in $projectBasePath")
                    readFromKtlintPluginPropertiesFile = false
                } else {
                    readFromKtlintPluginPropertiesFile = true
                }
            }

    private fun VirtualFile.readProperties() =
        inputStream
            .bufferedReader()
            .readLines()
            .filter { it.contains("=") }
            .associate {
                val key = it.substringBefore("=").trim()
                val value = it.substringAfter('=').trim()
                key to value
            }

    fun ktlintVersion() = properties[KTLINT_PLUGINS_VERSION_PROPERTY]

    fun ktlintRulesetVersion(): KtlintRulesetVersion? {
        if (!readFromKtlintPluginPropertiesFile) {
            logger.debug { "File '$KTLINT_PLUGINS_PROPERTIES_FILE_NAME' not found in $projectBasePath" }
            return null
        }

        val ktlintVersionLabel = ktlintVersion()
        return if (ktlintVersionLabel.isNullOrBlank()) {
            logger.debug {
                "No value found for property '$KTLINT_PLUGINS_VERSION_PROPERTY' in file '$KTLINT_PLUGINS_PROPERTIES_FILE_NAME' " +
                    "in $projectBasePath"
            }
            null
        } else {
            KtlintRulesetVersion
                .findByLabelOrNull(ktlintVersionLabel)
                ?.also {
                    logger.debug {
                        "Found Ktlint version '$ktlintVersionLabel' defined in property '$KTLINT_PLUGINS_VERSION_PROPERTY' in file " +
                            "'$KTLINT_PLUGINS_PROPERTIES_FILE_NAME'"
                    }
                }
                ?: null.also {
                    logger.debug {
                        "Ktlint version '$ktlintVersionLabel' defined in property '$KTLINT_PLUGINS_VERSION_PROPERTY' in file " +
                            "'$KTLINT_PLUGINS_PROPERTIES_FILE_NAME' is not supported by this version of the ktlint-intellij-plugin."
                    }
                }
        }
    }
}
