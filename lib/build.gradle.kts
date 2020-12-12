import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    api("com.pinterest.ktlint:ktlint-core:0.40.0")
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:0.40.0")
    implementation("com.pinterest.ktlint:ktlint-ruleset-experimental:0.40.0")
}

tasks {
    // Set the compatibility versions to 1.8
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<ShadowJar> {
        val api = project.configurations.api.get()
        val impl = project.configurations.implementation.get()

        configurations = listOf(api, impl)
        configurations.forEach { it.isCanBeResolved = true }

        relocate("org.jetbrains", "shadow.org.jetbrains")
    }
}
