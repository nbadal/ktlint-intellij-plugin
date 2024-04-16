import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.shadow)
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    compileOnly(project(":ktlint-lib:core")) // Required for IDE
    implementation(project(":ktlint-lib:core", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-0-50-0")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-0-50-0", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-0-1")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-0-1", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-1-1")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-1-1", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-2-0")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-2-0", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-2-1")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-2-1", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-2-2")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-2-2", "shadow"))

    implementation("com.rollbar:rollbar-java:1.10.0") {
        exclude(group = "org.slf4j") // Duplicated in IDE environment
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

        // Remove all classes which are not referenced. Note that classes that are reference inside the "plugin" module might need to be
        // added to ShadowJarMinimizeHelper to prevent that they are removed.
        minimize()
    }
}
