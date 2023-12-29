import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
    mavenLocal()
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

        // Relocate PSI classes to avoid conflicts and linkage errors. PSI class provided by the internal compiler of the Intellij IDEA are
        // not always identical/compatible with the PSI classes provided by the embeddable kotlin compiler used by ktlint.
        relocate("org.jetbrains.kotlin.psi", "shadow.org.jetbrains.kotlin.psi")
    }
}
