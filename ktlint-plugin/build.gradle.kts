import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
    alias(libs.plugins.buildconfig) // BuildConfig - read more: https://github.com/gmazzo/gradle-buildconfig-plugin
}

// `pluginName_` variable ends with `_` because of the collision with Kotlin magic getter in the `intellij` closure.
// Read more about the issue: https://github.com/JetBrains/intellij-platform-plugin-template/issues/29
@Suppress("PropertyName")
val pluginName_: String by project

group = providers.gradleProperty("pluginGroup").get()
// The publishPluginVersion contains the build timestamp in case the version is targeting a non-default channel
val publishPluginVersion =
    providers
        .gradleProperty("pluginVersion")
        .get()
        .let { pluginVersion ->
            if (pluginVersion.takeIfVersionForChannel("default") != null) {
                pluginVersion
            } else {
                // When building the beta or dev version then expand the version with a build timestamp
                "$pluginVersion.${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))}"
            }
        }
version = publishPluginVersion

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

// Configure project's dependencies
repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }

    // Comment out next line releasing a new version to any channel
    // mavenLocal()
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        instrumentationTools()
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }

    compileOnly(project(":ktlint-lib")) // Required for IDE
    implementation(project(":ktlint-lib", "shadow"))

    implementation("com.rollbar:rollbar-java:1.10.3") {
        exclude(group = "org.slf4j") // Duplicated in IDE environment
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testImplementation("org.junit.platform:junit-platform-launcher:1.12.1")
    testImplementation("io.mockk:mockk:1.13.17")
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    projectName.set("ktlint")

    pluginConfiguration {
        // Use publishPluginVersion which contains the build timestamp for release to non-default channel
        // Original:
        //    version = providers.gradleProperty("pluginVersion")
        version = publishPluginVersion

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description =
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
            // Use publishPluginVersion which contains the build timestamp for release to non-default channel
            // Original:
            //     providers.gradleProperty("pluginVersion").map { pluginVersion ->
            publishPluginVersion.let { pluginVersion ->
                with(changelog) {
                    renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                            .withHeader(false)
                            .withEmptySections(false),
                        Changelog.OutputType.HTML,
                    )
                }
            }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        // The channel is set by setting the pluginVersion in the root `gradle.properties`
        // Original:
        //     channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
        // Use publishPluginVersion which contains the build timestamp for release to non-default channel instead of
        // the regular pluginVersion
        // Original:
        //    version = providers.gradleProperty("pluginVersion")
        channels =
            listOfNotNull(
                // Extract channel from `pluginVersion`. When version does not contain information that restricts the version to a specific
                // channel, then it is to be published to the `default` channel.
                publishPluginVersion.takeIfVersionForChannel("default"),
                // Publish official and beta releases to beta channel
                publishPluginVersion.takeIfVersionForChannel("beta"),
                // Publish each version to the dev channel
                "dev",
            ).distinct()
                .also { channels ->
                    project.logger.lifecycle("PluginVersion `$version` publishes the plugin to channels: $channels")
                }
    }

    pluginVerification {
        ides {
            recommended()
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

// Configure BuildConfig generation
buildConfig {
    packageName("$group.${pluginName_}")

    val secretsPropertiesFile = File("secrets.properties")
    if (!secretsPropertiesFile.exists()) throw GradleException("secrets.properties not found.")
    val props = Properties()
    props.load(FileInputStream(secretsPropertiesFile))

    buildConfigField("String", "NAME", "\"ktlint-intellij-plugin\"")
    buildConfigField("String", "VERSION", "\"$publishPluginVersion\"")
    buildConfigField("String", "ROLLBAR_ACCESS_TOKEN", "\"${props.getProperty("ROLLBAR_ACCESS_TOKEN")}\"")
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
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
    // Replace with JavaCompile and KotlinCompile tasks

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders +=
                    CommandLineArgumentProvider {
                        listOf(
                            "-Drobot-server.port=8082",
                            "-Dide.mac.message.dialogs.as.sheets=false",
                            "-Djb.privacy.policy.text=<!--999.999-->",
                            "-Djb.consents.confirmation.enabled=false",
                        )
                    }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}
