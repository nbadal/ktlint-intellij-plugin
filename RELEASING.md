# How to make new project release

### Publishing a new release

Note: These steps should be done directly in the `nbadal/ktlint-intellij-plugin` repository, not in your personal fork.

Releasing a new version:

1. Check that ruleset of new ktlint version is added to ktlint-intellij-plugin. Only the latest patch version of a minor release needs to be supported. If a new major/minor version of ktlint is released then do following:
   * Copy latest ruleset module in `ktlint-lib` module.
   * Change the ruleset-version in this new module to the same version as ktlint
   * Add the new ruleset module to the `build.gradle.kts` in the `plugin` module
   * Add a new enum entry in `KtlintRulesetVersion` in the ruleset dropdown in `KtlintConfigForm.form`. Keep the label in the ruleset dropdown identical to the label in `KtlintRulesetVersion`.
   * Test that when selecting the `default (recommended)` version in the dropdown of the ktlint settings that actually the new ruleset is selected.
   * Test that when explicitly selecting the new version in the dropdown of the ktlint settings that actually the new ruleset is selected.
2. Create new branch, for example `0.20.0-beta-7-prep`.
3. Check whether plugin description in readme file of plugin-folder is up-to-date as this is used as description of the plugin on the Jetbrains Marketplace inside IntelliJ IDEA and for [Marketplace Overview](https://plugins.jetbrains.com/plugin/15057-ktlint?noRedirect=true). 
4. Generate changelog 
   * The generated changelog only contains the PR titles. For most changes this should be sufficient. For breaking API changes, it is better to add additional information. To indent this explanation correctly, append `  ` (two spaces) to the end of the previous line.
5. Do not change the heading `Unreleased` in `CHANGELOG.md` as it will be updated automatically. Also, the release will be added automatically to the bottom of the `CHANGELOG.md` file. 
6. Set field `pluginVersion` in `gradle.properties` to the new version number. Note that when the version if suffixed with `-beta` it will be released on the beta channel of the plugin only. Only users that have configured the additional repository `https://plugins.jetbrains.com/plugins/list?pluginId=com.nbadal.ktlint&channel=beta` will see the new version after it is released.
7. Push your changes to the branch, and merge it to `master`.
8. Do *not* tag the release manually, but go to the latest [Draft Release](https://github.com/nbadal/ktlint-intellij-plugin/releases). Check that release notes are up-to-date. It is not needed to add the ZIP file containing the plugin, as it will be published via the marketplace. 
9. Check that the release is available on [Marketplace Versions](https://plugins.jetbrains.com/plugin/15057-ktlint/versions?noRedirect=true). Note that it might take a full business day before the plugin is actually released due to a manual review by Jetbrains.
10. Check that screenshots on the [Marketplace Overview](https://plugins.jetbrains.com/plugin/15057-ktlint?noRedirect=true) are up-to-date.
11. Announce release on Ktlint Slack channel
12. Update `gradle.properties` with the new `beta` version, and add the section below to the top of `CHANGELOG.md` and commit. (This can be done directly in the main repo or in your fork.)
```markdown
## [Unreleased]
```
Note: the heading "[Unreleased]" may not be changed as this will result in incorrect release notes being extracted from the 'changelog.md' file. 
