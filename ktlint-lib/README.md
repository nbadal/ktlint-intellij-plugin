# Why is this a module?

The plugin encapsulates the artifacts of Ktlint in such a way that it does not conflict with the plugin. For example the embedded Kotlin compiler in the KtlintRuleEngine conflicts with the Kotlin compiler provided by Intellij IDEA. The `build.gradle.kts` contains the dependencies on the required Ktlint artifacts including the latest version of the rules.  Next to this, the lib contains submodules for older versions of the Ktlint rulesets.

## Ruleset modules

Each ruleset module transforms the StandardRuleSetProvider of that version to a unique class name so that multiple versions of the Standard rule sets can be supported by the plugin. The plugin allows the user to configure one of the supported ktlint versions. In this way, the user can keep the configuration of the ktlint intellij plugin in sync with other plugins like the ktlint gradle plugin or kotlinter.

# ktlint-lib module

The ktlint-lib module itself contains the `KtlintRulesetVersion` class which contains the references to the distinct versions of the rulesets. Also, it contains the `ShadowJarMinimizeHelper` class containing references to classes which are also used in the `plugin` module to prevent that those classes are removed by the minimize process of the Shadow Jar. 

## How to add a new ruleset when ktlint publishes a new version?

* Upgrade ktlint version in `ktlint-intellij-plugin/gradle/libs.versions.toml`
* Create a new ruleset directory by duplicating one of the existing ruleset directories in `ktlint-intellij-plugin/ktlint-lib`.
  * The ktlint version provided via this directory will be the highest ktlint version which was support by the last published plugin version. E.g. the highest ktlint version in enum `KtlintRulesetVersion`. For this readme, assume that it equals the value `X.Y.Z`. 
  * Change name of the directory to `ruleset-X-Y-Z`
    * Change the `build.gradle.kts` in this directory as follows:
      * Update the version number in the dependencies block:
        ```kotlin
        dependencies {
            implementation("com.pinterest.ktlint:ktlint-ruleset-standard:X.Y.Z")
        }
        ```
      * Update the ShadowJar relocate:
        ```kotlin
        relocate(
            "com.pinterest.ktlint.ruleset.standard",
            "com.pinterest.ktlint.ruleset.standard.VX_Y_Z",
        )
        ```
      * Update the ShadowJar minimize block:
        ```kotlin
        minimize {
            exclude(dependency("com.pinterest.ktlint:ktlint-ruleset-standard:X.Y.Z"))
        }
        ```
  * Add the new module to the `ktlint-intellij-plugin/ktlint-lib/build.gradle.kts` like:
    ```kotlin
    compileOnly(project(":ktlint-lib:ruleset-X-Z")) // Required for IDE
    implementation(project(":ktlint-lib:ruleset-X-Y-Z", "shadow"))
    ```
* Update the Ktlint version provided via directory `ktlint-intellij-plugin/ktlint-lib/src`. In file `ktlint-intellij-plugin/ktlint-lib/src/main/kotlin/com/pinterest/ktlint/ruleset/standard/KtlintRulesetVersion.kt` change following:
  * Update the ktlint version that will be provided via the `StandardRuleSetProvider` to the new ktlint version `A.B.C`
    ```kotlin
    VA_B_C("A.B.C", StandardRuleSetProvider()),
    ```
  * Add import statement for the newly created ruleset directory of version `X.Y.Z`
    ```kotlin
    import com.pinterest.ktlint.ruleset.standard.VX_Y_Z.StandardRuleSetProvider as StandardRuleSetProviderVX_Y_Z
    ```
    Note: this import statement will show as error as it does not yet exist until the compilation is completed when all changes are made!
  * Add the reference to the newly created ruleset like:
    ```kotlin
    VX_Y_Z("X.Y.Z", StandardRuleSetProviderVX_Y_Z()),
    ```
* Add the newly create ruleset module to the `include` block in file `ktlint-intellij-plugin/settings.gradle.kts`:
  ```kotlin
    "ktlint-lib:ruleset-X-Y-Z",
  ```
* Clean/build the project
* Test both the new and the previous ruleset version via the plugin
