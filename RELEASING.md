# How to make new project release

### Publishing a new release

Releasing a new version:

1. Verification before starting release:
    * Check that none of the `build.gradle.kts` depends on `mavenLocal()`. Those may only be used in snapshot versions of the plugin build on the local machine.
    * Check that none of the `build.gradle.kts` depends on `maven("https://oss.sonatype.org/content/repositories/snapshots")`. Those may only be used when publishing to the beta or dev channel.
    * Check that the ruleset for the latest ktlint version has been added to `ktlint-lib`. See `README.md` for more information.
2. Check whether plugin description in readme file of plugin-folder is up-to-date as this is used as description of the plugin on the Jetbrains Marketplace inside IntelliJ IDEA and for [Marketplace Overview](https://plugins.jetbrains.com/plugin/15057-ktlint?noRedirect=true).
3. Do not change the heading `Unreleased` in `CHANGELOG.md` as it will be updated automatically. Also, the release will be added automatically to the bottom of the `CHANGELOG.md` file.
4. Set field `pluginVersion` in `gradle.properties` to the new version number. Note that when the version if suffixed with `-beta` it will be released on the beta channel of the plugin only. Only users that have configured the additional repository `https://plugins.jetbrains.com/plugins/list?pluginId=com.nbadal.ktlint&channel=beta` will see the new version after it is released. Same for `-dev` to publish to `dev` channel
5. Push change and merge to main
6. Create a new release on the [release page](https://github.com/nbadal/ktlint-intellij-plugin/releases)
   * Choose a new tag for the release which is to be created during release:
     * For default channel, use format "<major>.<minor>.<patch>"
     * For beta channel, use format "<major>.<minor>.<patch>-beta-<sequence-number>"
     * For dev channel, use format "<major>.<minor>.<patch>-dev-<sequence-number>"
   * Set release title to the same value as the tag above
   * Generate changelog. The generated changelog only contains the PR titles. For most changes this should be sufficient. For breaking API changes, it is better to add additional information. To indent this explanation correctly, append `  ` (two spaces) to the end of the previous line.
   * For beta and dev channels mark the release as "pre-release"
   * Do not upload the ZIP file containing the plugin, as it will be published via the marketplace.
   * Click button "Publish release"
7. Check that a "Release" workflow is started on the [actions page](https://github.com/nbadal/ktlint-intellij-plugin/actions/workflows/release.yml) 
8. Check that the release is available on [Marketplace Versions](https://plugins.jetbrains.com/plugin/15057-ktlint/versions?noRedirect=true). For the "default" channel it might take a couple of business day before the plugin is actually released due to a manual review by Jetbrains.
9. Check that screenshots on the [Marketplace Overview](https://plugins.jetbrains.com/plugin/15057-ktlint?noRedirect=true) are up-to-date.
10. Announce release on Ktlint Slack channel
