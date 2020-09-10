package com.github.nbadal.ktlintintellijplugin.services

import com.intellij.openapi.project.Project
import com.github.nbadal.ktlintintellijplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
