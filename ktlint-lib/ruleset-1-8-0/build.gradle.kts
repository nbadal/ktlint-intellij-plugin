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
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:1.8.0")
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

tasks.shadowJar {
    relocate(
        "com.pinterest.ktlint.ruleset.standard",
        "com.pinterest.ktlint.ruleset.standard.V1_8_0",
    )

    minimize {
        exclude(dependency("com.pinterest.ktlint:ktlint-ruleset-standard:1.8.0"))
    }

    // Cannot use the minimize-block as that would build a fat jar. The GitHub runner has too little diskspace to build the project if a
    // fat jar is built for each version of the ktlint rulesets. Also, the non-ktlint dependencies will not be used in the final ktlint-lib
    // jar as the files of the latest ruleset will be used instead.
    exclude("dev/**")
    exclude("gnu/**")
    exclude("io/**")
    exclude("javaslang/**")
    exclude("kotlin/**")
    exclude("kotlinx/**")
    exclude("messages/**")
    exclude("misc/**")
    exclude("org/**")
}
