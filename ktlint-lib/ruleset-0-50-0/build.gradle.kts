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
    constraints {
        runtimeOnly(libs.slf4j.api) {
            because(
                "Transitive ktlint logging dependency (2.0.3) does not use the module classloader in ServiceLoader. Replace with newer SLF4J version",
            )
        }
    }
    implementation("com.pinterest.ktlint:ktlint-logger:0.50.0") {
        // Exclude the slf4j 2.0.3 version provided via Ktlint as it does not use the module classloader in the ServiceLoader
        exclude("org.slf4j")
            .because(
                "Transitive ktlint logging dependency (2.0.3) does not use the module classloader in ServiceLoader. Replace with newer SLF4J version",
            )
    }
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:0.50.0") {
        // Exclude the slf4j 2.0.3 version provided via Ktlint as it does not use the module classloader in the ServiceLoader
        exclude("org.slf4j")
            .because(
                "Transitive ktlint logging dependency (2.0.3) does not use the module classloader in ServiceLoader. Replace with newer SLF4J version",
            )
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
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

        minimize {
            exclude(dependency("com.pinterest.ktlint:ktlint-ruleset-standard:0.50.0"))
        }
    }
}
