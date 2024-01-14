import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.ktlintRuleEngine)
    api(libs.ktlintCliRulesetCore)
    api(libs.ktlintCliReporterCore)
    api(libs.ktlintCliReporterBaselineCore)
    implementation(libs.ktlintRulesetStandard)
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

        // Relocate to prevent conflicts with same packages provided by Intellij IDEA as well.
        // - The embeddable Kotlin compiler in the Ktlint jar differs from the compiler provided in the IDEA.
        // - "org.jetbrains.org.objectweb.asm" causes compatability issues in IDEA resulting in a lot of exceptions
        relocate("org.jetbrains", "shadow.org.jetbrains")
    }
}
