import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.shadow)
}

allprojects {
    repositories {
        mavenCentral()
        // Comment-out next line before publish on the default channel. It is okay to keep it when publishing to beta or dev channels
        // maven("https://central.sonatype.com/repository/maven-snapshots/")
        // Comment out next line before publishing to any channel
        // mavenLocal()
    }
}
// repositories {
//    maven {
//        name = 'Central Portal Snapshots'
//        url = 'https://central.sonatype.com/repository/maven-snapshots/'
//
//        // Only search this repository for the specific dependency
//        content {
//            includeModule("<the snapshot's groupId>", "<the snapshot's artifactId>")
//        }
//    }
//    mavenCentral()
// }
dependencies {
    // Common dependencies are included from latest released Ktlint version
    api(libs.ktlintRuleEngine)
    api(libs.ktlintCliRulesetCore)
    api(libs.ktlintCliReporterCore)
    api(libs.ktlintCliReporterBaselineCore)

    // Add the latest version of ktlintRulesetStandard so that custom rulesets can be loaded. With this implementation dependency, a runtime
    // exception is thrown when loading a custom ruleset.
    implementation(libs.ktlintRulesetStandard)

    // For each rule set version, add the ruleset dependencies

    // Potentially add the SNAPSHOT version of the next release
    // compileOnly(project(":ktlint-lib:ruleset-X-Y-X-SNAPSHOT")) // Required for IDE
    // implementation(project(":ktlint-lib:ruleset-X-Y-Z", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-8-0")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-8-0", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-7-2")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-7-2", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-6-0")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-6-0", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-5-0")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-5-0", "shadow"))

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
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        apiVersion.set(
            org
                .jetbrains
                .kotlin
                .gradle
                .dsl
                .KotlinVersion
                .KOTLIN_2_0,
        )
    }
}

tasks {
    withType<ShadowJar> {
        // Relocate to prevent conflicts with same packages provided by Intellij IDEA as well. The embeddable Kotlin compiler in the Ktlint
        // jar differs from the compiler provided in the IDEA.
        // From "compatability verification" (plugin verifier) results:
        //     The plugin distribution bundles IDE packages
        //       'org.jetbrains.org.objectweb.asm.signature',
        //       'org.jetbrains.org.objectweb.asm.commons',
        //       'org.jetbrains.org.objectweb.asm',
        //       'org.jetbrains.org.objectweb.asm.util',
        //       'org.jetbrains.org.objectweb',
        //       'org.jetbrains.org.objectweb.asm.tree.analysis',
        //       'org.jetbrains.concurrency',
        //       'org.jetbrains.org.objectweb.asm.tree',
        //       'org.jetbrains.org'.
        //       Bundling IDE packages is considered bad practice and may lead to sophisticated compatibility problems. Consider excluding
        //       these IDE packages from the plugin distribution. If your plugin depends on classes of an IDE bundled plugin, explicitly
        //       specify dependency on that plugin instead of bundling it.
        // IMPORTANT: These need to be added to `RelocatingClassLoader` as well!
        // See https://gradleup.com/shadow/configuration/relocation/
        relocate("org.jetbrains.org", "shadow.org.jetbrains.org")
        relocate("org.jetbrains.concurrency", "shadow.org.jetbrains.concurrency")
        relocate("org.jetbrains.kotlin", "shadow.org.jetbrains.kotlin")

        // Ktlint-lib itself may not be minimized as this would result in exceptions when loading custom rulesets as the RulesetProviderV3
        // cannot be found
    }
}
