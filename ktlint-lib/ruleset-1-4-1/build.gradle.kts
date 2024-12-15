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
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:1.4.1")
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
            "com.pinterest.ktlint.ruleset.standard",
            "com.pinterest.ktlint.ruleset.standard.V1_4_1",
        )

        minimize {
            exclude(dependency("com.pinterest.ktlint:ktlint-ruleset-standard:1.4.1"))
        }
    }
}
