import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream

fun properties(key: String) = providers.gradleProperty(key)

fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
    alias(libs.plugins.buildconfig) // BuildConfig - read more: https://github.com/gmazzo/gradle-buildconfig-plugin
}

// Import variables from gradle.properties file as plugin is now configured as submodule
// Original setup in intellij plugin template
//   group = properties("pluginGroup").get()
//   version = properties("pluginVersion").get()
val pluginGroup: String by project
// `pluginName_` variable ends with `_` because of the collision with Kotlin magic getter in the `intellij` closure.
// Read more about the issue: https://github.com/JetBrains/intellij-platform-plugin-template/issues/29
val pluginName_: String by project
val pluginVersion: String by project
val pluginRepositoryUrl: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project
val pluginVerifierIdeVersions: String by project
val platformPlugins: String by project
val platformType: String by project
val platformVersion: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = pluginVersion

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
//    implementation(libs.annotations)

    // Shadow lib (see: ../ktlint-lib/README.md)
    compileOnly(project(":ktlint-lib:core")) // Required for IDE
    implementation(project(":ktlint-lib:core", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-0-50-0")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-0-50-0", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-0-1")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-0-1", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-1-1")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-1-1", "shadow"))

    compileOnly(project(":ktlint-lib:ruleset-1-2-1")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-1-2-1", "shadow"))

    implementation("com.rollbar:rollbar-java:1.10.0") {
        exclude(group = "org.slf4j") // Duplicated in IDE environment
    }

    // Tests:
    testImplementation(project(":ktlint-lib:core"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("io.mockk:mockk:1.13.10")
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    @Suppress("UnstableApiUsage")
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(11)
        vendor = JvmVendorSpec.JETBRAINS
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    // Replace due to multi-module setup
    //   pluginName = properties("pluginName")
    //   version = properties("platformVersion")
    //   type = properties("platformType")
    pluginName = pluginName_
    version = platformVersion
    type = platformType

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins =
        // Replace due to multi-module setup
        //   properties("platformPlugins")
        //      .map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
        // with
        platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty)
}

// Configure BuildConfig generation
buildConfig {
    packageName("$pluginGroup.${pluginName_}")

    val secretsPropertiesFile = File("secrets.properties")
    if (!secretsPropertiesFile.exists()) throw GradleException("secrets.properties not found.")
    val props = Properties()
    props.load(FileInputStream(secretsPropertiesFile))

    buildConfigField("String", "NAME", "\"ktlint-intellij-plugin\"")
    buildConfigField("String", "VERSION", "\"${pluginVersion}\"")
    buildConfigField("String", "ROLLBAR_ACCESS_TOKEN", "\"${props.getProperty("ROLLBAR_ACCESS_TOKEN")}\"")
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    // Replace due to multi-module setup
    // repositoryUrl = properties("pluginRepositoryUrl")
    repositoryUrl = pluginRepositoryUrl
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath = provider { file(".qodana").canonicalPath }
    reportPath = provider { file("build/reports/inspections").canonicalPath }
    saveReport = true
    showReport = environment("QODANA_SHOW_REPORT").map { it.toBoolean() }.getOrElse(false)
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
koverReport {
    defaults {
        xml {
            onCheck = true
        }
    }
}

tasks {
    // Can not use wrapper clojure here due to multi-module configuration
    // Original
    //   wrapper {
    //      gradleVersion = properties("gradleVersion").get()
    //   }
    // Replace with JavaCompile and KotlintCompile tasks

    // Set the compatibility versions to 11
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    named<Test>("test") {
        useJUnitPlatform()
    }

    patchPluginXml {
        // Read properties from variables due to multi-module setup
        // Original
        //   version = properties("pluginVersion")
        //   sinceBuild = properties("pluginSinceBuild")
        //   untilBuild = properties("pluginUntilBuild")
        version = pluginVersion
        sinceBuild = pluginSinceBuild
        untilBuild = pluginUntilBuild

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription =
            providers
                .fileContents(layout.projectDirectory.file("README.md"))
                .asText
                .map {
                    val start = "<!-- Plugin description -->"
                    val end = "<!-- Plugin description end -->"

                    with(it.lines()) {
                        if (!containsAll(listOf(start, end))) {
                            throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                        }
                        subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
                    }
                }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes =
            // Replace due to multi-module setup
            //   properties("pluginVersion")
            //      .map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token = environment("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel.
        // The channel is set by setting the pluginVersion in the root `gradle.properties`
        channels =
            listOfNotNull(
                // Extract channel from `pluginVersion`. When version does not contain information that restricts the version to a specific
                // channel, then it is to be published to the `default` channel.
                pluginVersion
                    .split('-')
                    .getOrElse(1) { "default" }
                    .split('.')
                    .first(),
                // Publish each version to the beta channel.
                "beta",
            ).distinct()
                .also { channels ->
                    project.logger.lifecycle("PluginVersion `$pluginVersion` publishes the plugin to channels: $channels")
                }
    }
}
