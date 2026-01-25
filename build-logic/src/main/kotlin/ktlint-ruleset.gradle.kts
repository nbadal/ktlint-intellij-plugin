import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

abstract class KtlintRulesetExtension {
    abstract val version: Property<String>
    abstract val includeKotlinxExcludes: Property<Boolean>
    abstract val addEc4jCoreConstraint: Property<Boolean>
}

plugins {
    id("java") // Java support
    id("org.jetbrains.kotlin.jvm") // Kotlin support
    id("com.gradleup.shadow")
}

repositories {
    // Prevent that snapshot artifacts can be used for ktlint versions that have been released officially
    mavenCentral()
}

val rulesetExtension =
    extensions.create(
        "ktlintRuleset",
        KtlintRulesetExtension::class,
    ).apply {
        includeKotlinxExcludes.convention(true)
        addEc4jCoreConstraint.convention(false)
    }

kotlin {
    jvmToolchain(17)
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

afterEvaluate {
    val ktlintVersion =
        rulesetExtension.version.orNull
            ?: error("ktlintRuleset.version is required")
    val includeKotlinxExcludes = rulesetExtension.includeKotlinxExcludes.getOrElse(true)
    val addEc4jCoreConstraint = rulesetExtension.addEc4jCoreConstraint.getOrElse(false)
    val relocateSuffix = "V" + ktlintVersion.replace('.', '_')

    dependencies {
        implementation("com.pinterest.ktlint:ktlint-ruleset-standard:$ktlintVersion")

        if (addEc4jCoreConstraint) {
            constraints {
                implementation("org.ec4j.core:ec4j-core:1.2.0") {
                    because("Allows '.editorconfig' properties to be defined without any value")
                }
            }
        }
    }

    tasks.withType<ShadowJar>().configureEach {
        relocate(
            "com.pinterest.ktlint.ruleset.standard",
            "com.pinterest.ktlint.ruleset.standard.$relocateSuffix",
        )

        minimize {
            exclude(dependency("com.pinterest.ktlint:ktlint-ruleset-standard:$ktlintVersion"))
        }

        // Cannot use the minimize-block as that would build a fat jar. The GitHub runner has too little diskspace to build the project if a
        // fat jar is built for each version of the ktlint rulesets. Also, the non-ktlint dependencies will not be used in the final ktlint-lib
        // jar as the files of the latest ruleset will be used instead.
        exclude("dev/**")
        exclude("gnu/**")
        exclude("io/**")
        exclude("javaslang/**")
        exclude("kotlin/**")
        if (includeKotlinxExcludes) {
            exclude("kotlinx/**")
        }
        exclude("messages/**")
        exclude("misc/**")
        exclude("org/**")
    }
}
