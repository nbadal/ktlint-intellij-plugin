# Contributing

Thanks for your interest in improving the Ktlint IntelliJ Plugin.

## Development setup

- Open the project in IntelliJ IDEA.
- Use the `Run Plugin.run.xml` run configuration to start a sandbox IDE.
- The plugin description and change notes are extracted from `ktlint-plugin/README.md` via Gradle.

## Building and installing a dev build

Once the plugin has been tested with the `Run Plugin.run.xml` run configuration, a development build can be installed locally.

1) (Optional) Update the plugin name in `ktlint-plugin/src/main/resources/META-INF/plugin.xml` and/or `pluginVersion` in `gradle.properties` so the dev build is recognizable.
2) Perform a clean build.
3) Run the plugin once so the IDEA sandbox is refreshed.
4) Create a zip from the sandbox:

```shell
./create-ktlint-plugin-zip.sh
```

5) Install the zip via
<kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Ruleset maintenance

The `ktlint-lib` module relocates ktlint rulesets so multiple versions can coexist.

### Ruleset modules (ktlint-lib)

Each ruleset module transforms the StandardRuleSetProvider of that version to a unique class name so that multiple versions of the Standard rulesets can be supported by the plugin. The plugin allows the user to configure one of the supported ktlint versions. In this way, the user can keep the configuration of the ktlint IntelliJ plugin in sync with other integrations like the ktlint Gradle plugin or kotlinter.

### How to add a new ruleset when ktlint publishes a new version

1) Upgrade the ktlint version in `gradle/libs.versions.toml`.

2) Create a new ruleset directory by duplicating one of the existing ruleset directories in `ktlint-lib`.
   - The ktlint version provided via this directory should be the highest ktlint version supported by the last published plugin version (the highest version in `KtlintRulesetVersion`).
   - Rename the directory to `ruleset-X-Y-Z`.

3) Update the new ruleset `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.pinterest.ktlint:ktlint-ruleset-standard:X.Y.Z")
}
```

```kotlin
relocate(
    "com.pinterest.ktlint.ruleset.standard",
    "com.pinterest.ktlint.ruleset.standard.VX_Y_Z",
)
```

```kotlin
minimize {
    exclude(dependency("com.pinterest.ktlint:ktlint-ruleset-standard:X.Y.Z"))
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

## Release checklist

1) Verification before starting release:
   - Ensure no `build.gradle.kts` depends on `mavenLocal()`. Those may only be used in snapshot versions on a local machine.
   - Ensure no `build.gradle.kts` depends on `maven("https://central.sonatype.com/repository/maven-snapshots/")`. Those may only be used when publishing to the beta or dev channel.
   - Check that the `ktlint` version in `gradle/libs.versions.toml` is updated to the latest version of ktlint.
   - Check that the ruleset for the latest ktlint version has been added to `ktlint-lib` (see the ruleset steps above).
2) Check that the plugin description in `ktlint-plugin/README.md` is up to date. This is used for the JetBrains Marketplace and the Marketplace Overview page.
3) Check that the change notes in `ktlint-plugin/README.md` are up to date. These are used for the JetBrains Marketplace and the Marketplace Overview page.
4) Do not change the heading `Unreleased` in `ktlint-plugin/CHANGELOG.md` as it will be updated automatically. The release entry will be appended automatically.
5) Set `pluginVersion` in `gradle.properties` to the new version number.
   - When the version is suffixed with `-beta`, it is released to the beta channel only.
   - When the version is suffixed with `-dev`, it is released to the dev channel only.
6) Push the change and merge to `main`.
7) Create a new release on the GitHub release page:
   - Choose a new tag for the release:
     - Default channel: `<major>.<minor>.<patch>`
     - Beta channel: `<major>.<minor>.<patch>-beta-<sequence-number>`
     - Dev channel: `<major>.<minor>.<patch>-dev-<sequence-number>`
   - Set the release title to the same value as the tag.
   - Generate the changelog (PR titles are usually sufficient). For breaking changes, add additional context. To indent this explanation correctly, append two spaces to the end of the previous line.
   - For beta and dev channels, mark the release as “pre-release”.
   - Do not upload the ZIP file containing the plugin; it will be published via the marketplace.
   - Publish the release.
8) Check that the “Release” workflow starts on the Actions page.
9) Check that the release is available on the Marketplace Versions page. For the default channel, it can take a couple of business days due to manual review.
10) Check that screenshots on the Marketplace Overview page are up to date.
11) Announce the release on the Ktlint Slack channel.
