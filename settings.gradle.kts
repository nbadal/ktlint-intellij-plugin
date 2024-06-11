plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "ktlint-intellij-plugin"

include(
    "ktlint-lib",
    "ktlint-lib:ruleset-0-50-0",
    "ktlint-lib:ruleset-1-0-1",
    "ktlint-lib:ruleset-1-1-1",
    "ktlint-lib:ruleset-1-2-0",
    "ktlint-lib:ruleset-1-2-1",
    // The latest released version is *not* build as a separate sub project, but is an integral part of the lib
    "plugin",
)
