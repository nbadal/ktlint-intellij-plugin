import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.util.Properties

plugins {
    // Java support
    id("java")
    // Kotlin support
    kotlin("jvm")
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "1.1.2"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "1.1.2"
    // BuildConfig - read more: https://github.com/gmazzo/gradle-buildconfig-plugin
    id("com.github.gmazzo.buildconfig") version "3.0.1"
}

// Import variables from gradle.properties file
val pluginGroup: String by project
// `pluginName_` variable ends with `_` because of the collision with Kotlin magic getter in the `intellij` closure.
// Read more about the issue: https://github.com/JetBrains/intellij-platform-plugin-template/issues/29
val pluginName_: String by project
val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project
val pluginVerifierIdeVersions: String by project

val platformType: String by project
val platformVersion: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = pluginVersion

// Configure project's dependencies
repositories {
    mavenCentral()
}

dependencies {
    // Shadow lib (see: ../lib/README.md)
    compileOnly(project(":lib")) // Required for IDE
    implementation(project(":lib", "shadow"))

    implementation("com.rollbar:rollbar-java:1.7.8") {
        exclude(group = "org.slf4j") // Duplicated in IDE environment
    }

    // Tests:
    testImplementation(project(":lib"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.2")
    testImplementation("io.mockk:mockk:1.12.0")
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(pluginName_)
    version.set(platformVersion)
    type.set(platformType)
    downloadSources.set(platformDownloadSources.toBoolean())
    updateSinceUntilBuild.set(true)

//  Plugin Dependencies:
//  https://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_dependencies.html
    plugins.set(listOf("Kotlin"))
}

// Configure BuildConfig generation
buildConfig {
    packageName("$pluginGroup.$pluginName_")

    val propsFile = File("secrets.properties")
    if (!propsFile.exists()) throw GradleException("secrets.properties not found.")
    val props = Properties()
    props.load(FileInputStream(propsFile))

    buildConfigField("String", "NAME", "\"ktlint-intellij-plugin\"")
    buildConfigField("String", "VERSION", "\"$pluginVersion\"")
    buildConfigField("String", "ROLLBAR_ACCESS_TOKEN", "\"${props.getProperty("ROLLBAR_ACCESS_TOKEN")}\"")
}

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version = pluginVersion
}

tasks {
    // Set the compatibility versions to 1.8
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Detekt> {
        jvmTarget = "1.8"
    }

    named<Test>("test") {
        useJUnitPlatform()
    }

    patchPluginXml {
        version.set(pluginVersion)
        sinceBuild.set(pluginSinceBuild)
        untilBuild.set(pluginUntilBuild)

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            File("./README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md file:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(
            provider {
                changelog.getLatest().toHTML()
            }
        )
    }

    runPluginVerifier {
        ideVersions.set(pluginVerifierIdeVersions.split(',').map(String::trim).filter(String::isNotEmpty))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        channels.set(listOf(pluginVersion.split('-').getOrElse(1) { "default" }.split('.').first()))
    }
}
