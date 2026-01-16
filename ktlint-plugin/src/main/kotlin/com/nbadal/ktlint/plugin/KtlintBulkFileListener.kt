package com.nbadal.ktlint.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent

private val logger = KtlintLogger()

class KtlintBulkFileListener : BulkFileListener {
    private val projectLocator = ProjectLocator.getInstance()

    private val vFilePathProjectMap = HashMap<String, Project>()

    override fun before(events: List<VFileEvent>) {
        events.forEach { event ->
            if (event.mightAffectKtlintPluginConfiguration()) {
                // The project cannot be guessed for some of the events during the "after" processing, so cache it.
                event.cacheProject()
            }
        }
    }

    private fun VFileEvent.mightAffectKtlintPluginConfiguration(): Boolean {
        val projectBasePath = guessProject()?.basePath ?: return false
        return when (this) {
            is VFileMoveEvent -> {
                oldPath.isKtlintConfigurationPath(projectBasePath) || newPath.isKtlintConfigurationPath(projectBasePath)
            }

            is VFilePropertyChangeEvent -> {
                propertyName == "name" &&
                    (oldPath.isKtlintConfigurationPath(projectBasePath) || newPath.isKtlintConfigurationPath(projectBasePath))
            }

            else -> {
                path.isKtlintConfigurationPath(projectBasePath)
            }
        }
    }

    private fun String.isKtlintConfigurationPath(projectBasePath: String) =
        removePrefix(projectBasePath) in RELATIVE_PATHS_POTENTIALLY_AFFECTING_KTLINT_CONFIGURATION

    private fun VFileEvent.cacheProject() {
        guessProject()
            ?.run { vFilePathProjectMap[path] = this }
            ?: logger.debug { "Cannot cache project for $this as project cannot be guessed" }
    }

    override fun after(events: List<VFileEvent>) {
        events
            .forEach { event ->
                if (event.mightAffectKtlintPluginConfiguration()) {
                    // Simply always reset the ktlint configuration in case a file that is used to configure ktlint is affected. In some
                    // cases the ktlint configuration will be reset, while this might not be necessary. For example the `.editorconfig` file
                    // is changed for a non-ktlint property. Detailed checking is quite complicated, and could lead to problems when
                    // relevant cases are missed. Eagerly resetting on the other hand is not too expensive.
                    event
                        .guessProject()
                        ?.run {
                            KtlintRuleEngineWrapper
                                .instance
                                .reset(this)
                        }
                }
            }
    }

    private fun VFileEvent.guessProject(): Project? =
        vFilePathProjectMap[this.path]
            // In case a new file is created, the projectLocator needs to be called in the after event as the file did not yet exist in the
            // before event, and therefore the cache is not yet filled.
            ?: file?.run { projectLocator.guessProjectForFile(this) }

    private companion object {
        val RELATIVE_PATHS_POTENTIALLY_AFFECTING_KTLINT_CONFIGURATION =
            listOf(
                "/.editorconfig",
                "/.idea/ktlint-plugin.xml",
                "/ktlint-plugins.properties",
            )
    }
}
