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
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:1.0.1")

    constraints {
        // ec4-core version 0.3.0 which is included in ktlint 1.0.1 fails on '.editorconfig' properties without value
        implementation("org.ec4j.core:ec4j-core:1.1.1") {
            because("Allows '.editorconfig' properties to be defined without any value")
        }
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
        "com.pinterest.ktlint.ruleset.standard",
        "com.pinterest.ktlint.ruleset.standard.V1_0_1",
    )

    minimize {
        exclude(dependency("com.pinterest.ktlint:ktlint-ruleset-standard:1.0.1"))
    }

    // Cannot use the minimize-block as that would build a fat jar. The GitHub runner has too little diskspace to build the project if a
    // fat jar is built for each version of the ktlint rulesets. Also, the non-ktlint dependencies will not be used in the final ktlint-lib
    // jar as the files of the latest ruleset will be used instead.
    exclude("dev/**")
    exclude("gnu/**")
    exclude("io/**")
    exclude("javaslang/**")
    exclude("kotlin/**")
    exclude("messages/**")
    exclude("misc/**")
    exclude("org/**")
}
