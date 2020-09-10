package com.nbadal.ktlint.services

import com.intellij.openapi.project.Project
import com.nbadal.ktlint.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
