# Contributing

Thanks for your interest in improving the Ktlint IntelliJ Plugin.

## Development setup

- Open the project in IntelliJ IDEA.
- Use the `Run Plugin.run.xml` run configuration to start a sandbox IDE for day-to-day development.
- The plugin description and change notes are extracted from `ktlint-plugin/README.md` via Gradle.

Note: If you need to test the plugin in a regular IDE (non-sandbox), you can build and install a ZIP from the sandbox using `create-ktlint-plugin-zip.sh`.

## Ruleset maintenance

The `ktlint-lib` module relocates ktlint rulesets so multiple versions can coexist.

### Ruleset modules (ktlint-lib)

Each ruleset module transforms the StandardRuleSetProvider of that version to a unique class name so that multiple versions of the Standard rulesets can be supported by the plugin. The plugin allows the user to configure one of the supported ktlint versions. In this way, the user can keep the configuration of the ktlint IntelliJ plugin in sync with other integrations like the ktlint Gradle plugin or kotlinter.

### How to add a new ruleset when ktlint publishes a new version

1) Upgrade the ktlint version in `gradle/libs.versions.toml`.

2) Create a new ruleset directory by duplicating one of the existing ruleset directories in `ktlint-lib`.
   - The ktlint version provided via this directory should be the highest ktlint version supported by the last published plugin version (the highest version in `KtlintRulesetVersion`).
   - Rename the directory to `ruleset-X-Y-Z`.

3) Update the new ruleset `build.gradle.kts` to use the convention plugin:

```kotlin
plugins {
    id("ktlint-ruleset")
}

ktlintRuleset {
    version = "X.Y.Z"
    // Only set when needed for older ktlint versions (1.0.1 - 1.3.1)
    addEc4jCoreConstraint = true
    // Only set to false for older versions (1.0.1 - 1.3.1)
    includeKotlinxExcludes = false
}
```

4) Add the new module to `ktlint-lib/build.gradle.kts`:

```kotlin
compileOnly(project(":ktlint-lib:ruleset-X-Y-Z")) // Required for IDE
implementation(project(":ktlint-lib:ruleset-X-Y-Z", "shadow"))
```

5) Update `ktlint-lib/src/main/kotlin/com/pinterest/ktlint/ruleset/standard/KtlintRulesetVersion.kt`:

- Add the import:

```kotlin
import com.pinterest.ktlint.ruleset.standard.VX_Y_Z.StandardRuleSetProvider as StandardRuleSetProviderVX_Y_Z
```

- Add the enum entry (ordered by most recent after DEFAULT):

```kotlin
VX_Y_Z(StandardRuleSetProviderVX_Y_Z()),
```

6) Add the module to `settings.gradle.kts`:

```kotlin
"ktlint-lib:ruleset-X-Y-Z",
```

7) Clean/build the project and verify both the new and previous ruleset versions via the plugin settings.

Note: The total size of the plugin grows by approximately 1 MB per ruleset version.

## Building with ktlint SNAPSHOT versions

If you need a snapshot release of ktlint, add the snapshot repository to `ktlint-lib/build.gradle.kts`:

```kotlin
allprojects {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
```

Then update `gradle/libs.versions.toml` to the snapshot version.

## Building with a local ktlint version

If you need a local unpublished ktlint build, use:

```kotlin
allprojects {
    repositories {
        mavenCentral()
        // Comment out the next line before publishing to default channel.
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        // Comment out the next line before publishing to any channel.
        mavenLocal()
    }
}
```

Publish ktlint to the local Maven repo from the ktlint project:

```shell
./gradlew publishMavenPublicationToMavenLocal
```
