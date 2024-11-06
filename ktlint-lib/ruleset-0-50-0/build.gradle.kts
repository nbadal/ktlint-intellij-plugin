import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:1.4.1") {
        // Exclude the slf4j 2.0.3 version provided via Ktlint as it does not use the module classloader in the ServiceLoader
        exclude("org.slf4j")
            .because(
                "Transitive ktlint logging dependency (2.0.3) does not use the module classloader in ServiceLoader. Replace with newer SLF4J version",
            )
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
}

tasks {
    withType<ShadowJar> {
        relocate(
            "com.pinterest.ktlint.logger",
            "com.pinterest.ktlint-0-50-0.logger",
        )
        relocate(
            "com.pinterest.ktlint.ruleset.standard",
            "com.pinterest.ktlint.ruleset.standard.V0_50_0",
        )

        minimize {
            exclude(dependency("com.pinterest.ktlint:ktlint-ruleset-standard:1.4.1"))
        }
    }
}
