package com.nbadal.ktlint

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.nbadal.ktlint.KtlintNotifier.KtlintNotificationGroup.CONFIGURATION
import com.pinterest.ktlint.ruleset.standard.KtlintRulesetVersion
import java.nio.file.Path

const val KTLINT_PLUGINS_PROPERTIES_FILE_NAME = "ktlint-plugins.properties"
const val KTLINT_PLUGINS_VERSION_PROPERTY = "ktlint-version"

private val logger = KtlintLogger()

class KtlintPluginsPropertiesReader {
    private var properties: Map<String, String> = emptyMap()
    private var project: Project? = null
    private var projectBasepath: String? = null
    private var readFromKtlintPluginPropertiesFile = false
    private var showErrorOnUnsupportedKtlintVersion = true

    fun configure(project: Project?) {
        if (this.project != project || this.project?.basePath != projectBasepath) {
            this.project = project
            projectBasepath = project?.basePath
            showErrorOnUnsupportedKtlintVersion = true
            properties =
                ktlintPluginsPropertiesVirtualFile(project?.basePath)
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
            logger.debug { "File '$KTLINT_PLUGINS_PROPERTIES_FILE_NAME' not found in ${project?.basePath}" }
            return null
        }

        val ktlintVersionLabel = ktlintVersion()
        return if (ktlintVersionLabel.isNullOrBlank()) {
            logger.debug {
                "No value found for property '$KTLINT_PLUGINS_VERSION_PROPERTY' in file '$KTLINT_PLUGINS_PROPERTIES_FILE_NAME' " +
                    "in ${project?.basePath}"
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
                    if (showErrorOnUnsupportedKtlintVersion) {
                        // Prevent the error from being shown too many times
                        showErrorOnUnsupportedKtlintVersion = false

                        val ktlintPluginVersion =
                            PluginManager
                                .getInstance()
                                .findEnabledPlugin(PluginId.findId("com.nbadal.ktlint")!!)
                                ?.version

                        KtlintNotifier
                            .notifyError(
                                notificationGroup = CONFIGURATION,
                                project = project!!,
                                title = "Unsupported Ktlint version",
                                message =
                                    """
                                    Ktlint version <strong>$ktlintVersionLabel</strong> is not supported by current version 
                                    (<strong>$ktlintPluginVersion</strong>) of Ktlint Intelli Plugin.
                                    """.trimIndent(),
                            )
                    }
                }
        }
    }
}
