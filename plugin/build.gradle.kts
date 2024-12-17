import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

val finalPluginVersion =
    if (pluginVersion.takeIfVersionForChannel("default") != null) {
        pluginVersion
    } else {
        // When building the beta or dev version then expand the version with a build timestamp
        "$pluginVersion.${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))}"
    }

group = pluginGroup
version = finalPluginVersion

repositories {
    mavenCentral()
    // Comment out next line releasing a new version to any channel
    // mavenLocal()
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    compileOnly(project(":ktlint-lib")) // Required for IDE
    implementation(project(":ktlint-lib", "shadow"))

    implementation("com.rollbar:rollbar-java:1.10.3") {
        exclude(group = "org.slf4j") // Duplicated in IDE environment
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation("io.mockk:mockk:1.13.13")
}

kotlin {
    jvmToolchain(17)
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
    buildConfigField("String", "VERSION", "\"${finalPluginVersion}\"")
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
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
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

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
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
        version = finalPluginVersion

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
                finalPluginVersion.takeIfVersionForChannel("default"),
                // Publish official and beta releases to beta channel
                finalPluginVersion.takeIfVersionForChannel("beta"),
                // Publish each version to the dev channel
                "dev",
            ).distinct()
                .also { channels ->
                    project.logger.lifecycle("PluginVersion `$finalPluginVersion` publishes the plugin to channels: $channels")
                }
    }
}

fun String.takeIfVersionForChannel(channel: String): String? =
    split('-')
        // Extract the channel from the version number. If no version is embedded in the version, assume the given channel was included in
        // the version.
        .getOrElse(1) { channel }
        // The version can contain a subversion which is specified after a ".", and has to be ignored
        .split('.')
        .firstOrNull { it == channel }
