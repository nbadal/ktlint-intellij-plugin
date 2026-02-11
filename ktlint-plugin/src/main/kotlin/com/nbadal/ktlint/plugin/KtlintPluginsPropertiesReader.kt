package com.nbadal.ktlint.plugin

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.nbadal.ktlint.lib.KtlintConnector.Companion.supportedKtlintVersions
import com.nbadal.ktlint.lib.KtlintVersion
import java.nio.file.Path

const val KTLINT_PLUGINS_PROPERTIES_FILE_NAME = "ktlint-plugins.properties"
const val KTLINT_PLUGINS_VERSION_PROPERTY = "ktlint-version"

private val logger = KtlintLogger()

class KtlintPluginsPropertiesReader {
    private lateinit var project: Project
    private var properties: Map<String, String> = emptyMap()
    private var projectBasepath: String? = null
    private var readFromKtlintPluginPropertiesFile = false
    private var showErrorOnUnsupportedKtlintVersion = true

    private var _ktlintVersion: KtlintVersion? = null

    val ktlintVersion: KtlintVersion?
        get() = _ktlintVersion

    fun setProject(project: Project) {
        if (!::project.isInitialized || this.project != project) {
            this.project = project
        }
        if (this.project.basePath != projectBasepath) {
            projectBasepath = this.project.basePath
            readKtlintPluginPropertiesFile()
            _ktlintVersion = computeKtlintVersion()
        }
    }

    private fun readKtlintPluginPropertiesFile() {
        showErrorOnUnsupportedKtlintVersion = true
        properties =
            ktlintPluginsPropertiesVirtualFile(project.basePath)
                ?.readProperties()
                ?: emptyMap()
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

    private fun computeKtlintVersion(): KtlintVersion? {
        if (!readFromKtlintPluginPropertiesFile) {
            logger.debug { "File '$KTLINT_PLUGINS_PROPERTIES_FILE_NAME' not found in ${project.basePath}" }
            return null
        }

        val ktlintVersion = properties[KTLINT_PLUGINS_VERSION_PROPERTY]?.let { KtlintVersion(it) }
        return if (ktlintVersion == null) {
            logger.debug {
                "No value found for property '$KTLINT_PLUGINS_VERSION_PROPERTY' in file '$KTLINT_PLUGINS_PROPERTIES_FILE_NAME' " +
                    "in ${project.basePath}"
            }
            null
        } else {
            supportedKtlintVersions
                .firstOrNull { it.label == ktlintVersion.label }
                ?.also {
                    logger.debug {
                        "Found Ktlint version '${ktlintVersion.label}' defined in property '$KTLINT_PLUGINS_VERSION_PROPERTY' in file " +
                            "'$KTLINT_PLUGINS_PROPERTIES_FILE_NAME'"
                    }
                }
                ?: null.also {
                    logger.debug {
                        "Ktlint version '${ktlintVersion.label}' defined in property '$KTLINT_PLUGINS_VERSION_PROPERTY' in file " +
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
                                notificationGroup = KtlintNotifier.KtlintNotificationGroup.CONFIGURATION,
                                project = project,
                                title = "Unsupported Ktlint version",
                                message =
                                    """
                                    Ktlint version <strong>${ktlintVersion.label}</strong> is not supported by current version 
                                    (<strong>$ktlintPluginVersion</strong>) of Ktlint Intelli Plugin.
                                    """.trimIndent(),
                            )
                    }
                }
        }
    }
}
