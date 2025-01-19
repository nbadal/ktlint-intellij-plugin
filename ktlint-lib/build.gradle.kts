import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.shadow)
}

allprojects {
    repositories {
        mavenCentral()
        // Comment out next line before publishing to any channel
        // mavenLocal()
        // Comment out next line before publish on default channel. It is okay to keep it when publishing to beta or dev channels
        // maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    // Add dependencies from latest released Ktlint version. Note that the latest release version is not build as a subproject like the
    // older released version. Reason for this is that minimizing the Shadowjar of the subprojects leads to removal of the RulesetProviderV3
    // class which leads to exceptions when loading a custom ruleset jar.
    api(libs.ktlintRuleEngine)
    api(libs.ktlintCliRulesetCore)
    api(libs.ktlintCliReporterCore)
    api(libs.ktlintCliReporterBaselineCore)

    // Include the ktlint ruleset baked into the latest ktlint release in the `ktlint-lib` directly
    implementation(libs.ktlintRulesetStandard)

    // Be aware that the latest ktlint ruleset is not listed below because it already included by line above!

    compileOnly(project(":ktlint-lib:ruleset-1-4-1")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-4-1", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-3-1")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-3-1", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-3-0")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-3-0", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-2-1")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-2-1", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-2-0")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-2-0", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-1-1")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-1-1", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-0-1")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-0-1", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-0-50-0")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-0-50-0", "shadow"))

    implementation("com.rollbar:rollbar-java:1.10.3") {
        exclude(group = "org.slf4j") // Duplicated in IDE environment
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
        // Expose all ruleset implementations:
        mergeServiceFiles()

        // Ktlint contains the embeddable Kotlin compiler. The Kotlin compiler brings in some packages from "org.jetbrains" and
        // "org.intellij" which might conflict with the same packages provided by the Intellij IDEA Runtime environment. To avoid possible
        // conflicts, the packages are shadowed so that ktlint can still use the embedded compiler, independently of the compiler which is
        // provided in the Intellij IDEA runtime.
        // IMPORTANT: Third party suppliers of rule set need to add those relocations as well!
        relocate("org.jetbrains", "shadow.org.jetbrains")
        relocate("org.intellij", "shadow.org.intellij")

        // Ktlint-lib itself may not be minimized as this would result in exceptions when loading custom rulesets as the RulesetProviderV3
        // can not be found
    }
}
