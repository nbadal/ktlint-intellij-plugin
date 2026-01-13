plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
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

dependencies {
    // No dependency allowed on ktlint-lib or ktlint-plugin
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
}
