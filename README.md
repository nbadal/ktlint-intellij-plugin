# Plugin set

Contrary to the [default plugin setup](https://github.com/JetBrains/intellij-platform-plugin-template) this plugin is set up as a multi-module project. This is required as the Ktlint artifact for the KtlintRuleEngine encloses the embeddable Kotlin compiler which conflicts with the IDE compiler.

The "ktlint-lib" project relocates the conflicting classes, and provides the different versions of the rulesets. The "plugin" project uses the "lib" to include the (modified) Ktlint artifacts. 

## Installation

Once the plugin has been tested with the `Run Plugin.run.xml` run configuration, the development version of the plugin can also be installed on the local machine of yourself and other beta testers.

- Modify the name of the plugin `plugin/src/main/resources/META-INF/plugin.xml` and/or the `pluginVersion` in the (root) `gradle.properties` so that the specific version can be recognized easily:
   ```xml
   <name>Ktlint (dev-version YYYY-MM-DD)</name>
   ```
- Perform a clean build
- Run the Plugin so that the IDEA sandbox is refreshed
- Create zip file with development version of plugin from IDEA sandbox
  ```shell
  ./create-ktlint-plugin-zip.sh
  ```
- Install the zip file manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Adding a ktlint ruleset

The `ktlint-lib` module contains all version of the ktlint rules which are supported by the plugin. 

To support a new version (`X.Y.Z`) of ktlint, the following needs to be done:
* Duplicate the latest `ruleset-A-B-C` module in `ktlint-lib`. This module only contains a `build.gradle.kts` file. This file needs to be changed as follows:
  * In the `dependencies` block refer to the new ktlint version `X.Y.Z` (use a snapshot version when applicable)
    ```kotlin
    dependencies {
      implementation("com.pinterest.ktlint:ktlint-ruleset-standard:X.Y.Z-SNAPSHOT")
    }
    ```
  * In the `relocate` block change the coordinates of the `StandardRulesetProvider` as follows (note that only the minor version `Y` needs to be left padded with a 0 to support up to 99 minor versions):
    ```kotlin
        relocate(
            "com.pinterest.ktlint.ruleset.standard",
            "com.pinterest.ktlint.ruleset.standard.VX_YY_Z",
        )
    ```
* In class `KtlintRulesetVersion` add a new enum entry below enum entry `DEFAULT`:
  ```kotlin
    DEFAULT("default (recommended)", null),
    VX_Y_Z("X.Y.Z", StandardRuleSetProviderVX_0Y_Z()), 
  ```
  Note: the required import `com.pinterest.ktlint.ruleset.standard.VX_0Y_Z.StandardRuleSetProvider as StandardRuleSetProviderVX_0Y_Z` will not be valid until all steps have been completed and the build has succeeded.
* In the `dependencies` block of `ktlint-lib/build.gradle.kts` add following:
  ```kotlin
  compileOnly(project(":ktlint-lib:ruleset-X-Y-Z")) // Required for IDE
  implementation(project(":ktlint-lib:ruleset-X-Y-Z", "shadow"))
  ```
* In field `rulesetVersion` in file `KtlintConfigForm` add the new option `X.Y.Z` just below value `default (recommended)`. Note that this value should be identical to the value of the `label` used in the enum entry in `KtlintRulesetVersion`.
* In the `include` block in the root `build.gradle.kts` add following:
  ```kotlin
    "ktlint-lib:ruleset-X-Y-Z",
  ```

Note: the total size of the plugin grows with approximately 1 MB per ruleset version which is added. 

## Building with ktlint SNAPSHOT version

Snapshots of ktlint are published on Sonatype https://oss.sonatype.org/content/repositories/snapshots/com/pinterest/ktlint/

Add following section to the `build.gradle.kts` of the `ktlint-lib` module:
```kotlin
allprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}
```

In `gradle/libs.version.toml` change the `ktlint` setting to the snapshot-version.

In case you want to build with a local version of ktlint which is not yet published to Sonatype, then add following section to the `build.gradle.kts` of the `ktlint-lib` module instead:
```kotlin
allprojects {
    repositories {
        mavenCentral()
        // Comment out next line before publish on default channel. It is okay to keep it when publishing to beta or dev channels
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        // Comment out next line before publishing to any channel
        mavenLocal()
    }
}
```
Note: In the "ktlint" project execute `./gradlew publishMavenPublicationToMavenLocal` to publish the SNAPSHOT artifacts to your local maven repository!
