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


## Building with local ktlint SNAPSHOT

In case you need to build the plugin based on a SNAPSHOT version of ktlint on your local machine, then follow procedure below:

* In the "ktlint" project execute `./gradlew publishMavenPublicationToMavenLocal` to publish the SNAPSHOT artifacts to your local maven repository.
* In the "ktlint-intellij-plugin" project:
  * Change the ktlint version in `libs.version.toml`  
  * Include `mavenLocal()` repository in *all* `build.gradle.kts` files as follows:
    ```kotlin
     repositories {
         mavenCentral()
         mavenLocal()
     }
    ```
  * Build the `lib` module
  * Build the `plugin` module
  * Run the plugin
