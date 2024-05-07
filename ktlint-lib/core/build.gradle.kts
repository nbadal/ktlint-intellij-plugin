import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.shadow)
}

dependencies {
    api(libs.ktlintRuleEngine)
    api(libs.ktlintCliRulesetCore)
    api(libs.ktlintCliReporterCore)
    api(libs.ktlintCliReporterBaselineCore)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<ShadowJar> {
        val api = project.configurations.api.get()
        val impl = project.configurations.implementation.get()

        configurations = listOf(api, impl).map { it.apply { isCanBeResolved = true } }

        // Expose all ruleset implementations:
        mergeServiceFiles()

        // Relocate to prevent conflicts with same packages provided by Intellij IDEA as well. The embeddable Kotlin compiler in the Ktlint
        // jar differs from the compiler provided in the IDEA.
        // IMPORTANT: Third party suppliers of rule set need to add those relocations as well!
        relocate("org.jetbrains.kotlin.psi.KtPsiFactory", "shadow.org.jetbrains.kotlin.psi.KtPsiFactory")
        relocate("org.jetbrains.kotlin.psi.psiUtil", "shadow.org.jetbrains.kotlin.psi.psiUtil")

        // From "compatability verification" (plugin verifier) results:
        //     The plugin distribution bundles IDE packages
        //       'org.jetbrains.org.objectweb.asm.signature',
        //       'org.jetbrains.org.objectweb.asm.commons',
        //       'org.jetbrains.org.objectweb.asm',
        //       'org.jetbrains.org.objectweb.asm.util',
        //       'org.jetbrains.org.objectweb',
        //       'org.jetbrains.org.objectweb.asm.tree.analysis',
        //       'org.jetbrains.concurrency',
        //       'org.jetbrains.org.objectweb.asm.tree',
        //       'org.jetbrains.org'.
        //       Bundling IDE packages is considered bad practice and may lead to sophisticated compatibility problems. Consider excluding
        //       these IDE packages from the plugin distribution. If your plugin depends on classes of an IDE bundled plugin, explicitly
        //       specify dependency on that plugin instead of bundling it.
        // IMPORTANT: Third party suppliers of rule set need to add those relocations as well!
        relocate("org.jetbrains.org", "shadow.org.jetbrains.org")
        relocate("org.jetbrains.concurrency", "shadow.org.jetbrains.concurrency")
    }
}
