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
        // ec4-core version 0.3.0 which is included in ktlint 0.50.0 fails on '.editorconfig' properties without value
        implementation("org.ec4j.core:ec4j-core:1.1.0") {
            because("Allows '.editorconfig' properties to be defined without any value")
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

kotlin {
    jvmToolchain(17)
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
}

tasks.shadowJar {
    relocate(
        "com.pinterest.ktlint.logger",
        "com.pinterest.ktlint-0-50-0.logger",
    )
    relocate(
        "com.pinterest.ktlint.ruleset.standard",
        "com.pinterest.ktlint.ruleset.standard.V0_50_0",
    )

    // Can not use the minimize block as that would build a fat jar. The GitHub runner has too little diskspace to build the project if a
    // fat jar is build for each version of the ktlint rulesets. Also, the non-ktlint dependencies will not be used in the final ktlint-lib
    // jar as the files of the latest ruleset will be used instead,
    exclude("com/sun/**")
    exclude("gnu/**")
    exclude("javaslang/**")
    exclude("kotlin/**")
    exclude("messages/**")
    exclude("misc/**")
    exclude("mu/**")
    exclude("org/**")
}
