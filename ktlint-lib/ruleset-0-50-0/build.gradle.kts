import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.shadow)
}

// Prevent that snapshot artifacts can be used for ktlint versions that have been released officially
repositories {
    mavenCentral()
}

dependencies {
    // Until version 0.50.0, the "mu.Kotlin" logger was used. In 1.x version this has been replaced with
    // "io.github.oshai.kotlinlogging.KLogger".
    implementation("com.pinterest.ktlint:ktlint-logger:0.50.0")
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:0.50.0")
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

        relocate(
            "com.pinterest.ktlint.logger",
            "com.pinterest.ktlint-0-50-0.logger",
        )
        relocate(
            "com.pinterest.ktlint.ruleset.standard",
            "com.pinterest.ktlint.ruleset.standard.V0_50_0",
        )
    }
}
