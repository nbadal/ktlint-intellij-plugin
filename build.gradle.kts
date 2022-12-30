plugins {
    kotlin("jvm") version "1.7.22" apply false

    // detekt linter - read more: https://github.com/detekt/detekt
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    // ktlint linter - read more: https://github.com/jeremymailen/kotlinter-gradle
    id("org.jmailen.kotlinter") version "3.13.0"
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.22.0")
}

detekt {
    config = files("./detekt-config.yml")
    buildUponDefaultConfig = true

    reports {
        html.enabled = false
        xml.enabled = false
        txt.enabled = false
    }
}
