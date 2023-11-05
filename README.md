# Plugin set

Contrary to the [default plugin setup](https://github.com/JetBrains/intellij-platform-plugin-template) this plugin is set up as a multi-module project. This is required as the Ktlint artifact for the KtlintRuleEngine encloses the embeddable Kotlin compiler which conflicts with the IDE compiler.

The "lib" project relocates the conflicting classes. The "plugin" project uses the "lib" to include the (modified) Ktlint artifacts. 

## Installation

Once the plugin has been tested with the `Run Plugin.run.xml` run configuration, the development version of the plugin can also be installed on the local machine of yourself and other beta testers.

- Modify the name of the plugin `plugin/src/main/resources/META-INF/plugin.xml` and/or the `pluginVersion` in the (root) `gradle.properties` so that the specific version can be recognized easily:
   ```xml
   <name>Ktlint (dev-version YYYY-MM-DD)</name>
   ```
- Run the Plugin so that the IDEA sandbox is refreshed
- Create zip file with development version of plugin from IDEA sandbox
  ```shell
  (cd plugin/build/idea-sandbox/plugins && zip  -r ../../ktlint-plugin-dev.zip ktlint)
  ```
- Install the zip file manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
