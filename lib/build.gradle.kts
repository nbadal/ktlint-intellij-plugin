import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.pinterest.ktlint:ktlint-core:0.48.2") {
        exclude("org.slf4j")
    }
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:1.0.0") {
        exclude("org.slf4j")
    }
    implementation("com.pinterest.ktlint:ktlint-ruleset-experimental:0.48.2") {
        exclude("org.slf4j")
    }
}

tasks {
    // Set the compatibility versions to 11
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    withType<ShadowJar> {
        val api = project.configurations.api.get()
        val impl = project.configurations.implementation.get()

        configurations = listOf(api, impl).map { it.apply { isCanBeResolved = true } }

        // Expose all ruleset implementations:
        mergeServiceFiles()

        relocate("org.jetbrains.kotlin.psi.KtPsiFactory", "shadow.org.jetbrains.kotlin.psi.KtPsiFactory")
        relocate("org.jetbrains.kotlin.psi.psiUtil", "shadow.org.jetbrains.kotlin.psi.psiUtil")
    }
}
