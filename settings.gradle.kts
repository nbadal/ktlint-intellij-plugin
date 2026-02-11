pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ktlint-intellij-plugin"

include(
    "ktlint-lib",
    "ktlint-lib:ruleset-1-0-1",
    "ktlint-lib:ruleset-1-1-1",
    "ktlint-lib:ruleset-1-2-0",
    "ktlint-lib:ruleset-1-2-1",
    "ktlint-lib:ruleset-1-3-0",
    "ktlint-lib:ruleset-1-3-1",
    "ktlint-lib:ruleset-1-4-1",
    "ktlint-lib:ruleset-1-5-0",
    "ktlint-lib:ruleset-1-6-0",
    "ktlint-lib:ruleset-1-7-2",
    "ktlint-lib:ruleset-1-8-0",
    "ktlint-plugin",
)
