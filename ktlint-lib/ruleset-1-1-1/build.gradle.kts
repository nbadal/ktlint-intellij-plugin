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
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:1.1.1")
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
            "com.pinterest.ktlint.ruleset.standard",
            "com.pinterest.ktlint.ruleset.standard.V1_01_1",
        )
    }
}
