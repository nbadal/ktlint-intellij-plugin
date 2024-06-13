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
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:1.2.1")
}

kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<ShadowJar> {
        relocate(
            "com.pinterest.ktlint.ruleset.standard",
            "com.pinterest.ktlint.ruleset.standard.V1_02_1",
        )

        minimize {
            exclude(dependency("com.pinterest.ktlint:ktlint-ruleset-standard:1.2.1"))
        }
    }
}
