plugins {
    kotlin("jvm") version "1.8.22" apply false

    // detekt linter - read more: https://github.com/detekt/detekt
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    // ktlint linter - read more: https://github.com/jeremymailen/kotlinter-gradle
    id("org.jmailen.kotlinter") version "3.13.0"
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.22.0")
}

apply(plugin = "io.gitlab.arturbosch.detekt")

detekt {
    config = files("./detekt-config.yml")
    buildUponDefaultConfig = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(false)
        xml.required.set(false)
        txt.required.set(false)
    }
}