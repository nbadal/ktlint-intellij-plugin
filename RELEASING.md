# How to make new project release

### Publishing a new release

Note: These steps should be done directly in the `nbadal/ktlint-intellij-plugin` repository, not in your personal fork.

Releasing a new version:

1. Create new branch, for example `0.20.0-beta-7-prep`.
2. Check whether plugin description in readme file of plugin-folder is up-to-date as this is used as description of the plugin on the Jetbrains Marketplace inside IntelliJ IDEA and for [Marketplace Overview](https://plugins.jetbrains.com/plugin/15057-ktlint?noRedirect=true). 
3. TODO: Manually run GitHub Action workflow https://github.com/pinterest/ktlint/actions/workflows/generate-changelog.yml to generate the new changelog based on the PR's since last release
   * Check whether each PR is listed in the correct category. If not, add the proper label to the PR and repeat. See https://github.com/pinterest/ktlint/blob/master/.github/workflows/generate-changelog.yml#L35 for which labels to use.
   * Copy the generated changelog from build step `Echo generated changelog` to the `CHANGELOG.md` file
   * The generated changelog only contains the PR titles. For most changes this should be sufficient. For breaking API changes, it is better to add additional information. To indent this explanation correctly, append `  ` (two spaces) to the end of the previous line.
4. Update `CHANGELOG.md` to rename the `Unreleased` section to the new release name, following the `## [x.x.x] - YYYY-MM-DD` format.
5. Add the new release to the bottom of the `CHANGELOG.md` file. 
6. Set field `pluginVersion` in `gradle.properties` to the new version number. Note that when the version if suffixed with `-beta` it will be released on the beta channel of the plugin only. Only users that have configured the additional repository `https://plugins.jetbrains.com/plugins/list?pluginId=com.nbadal.ktlint&channel=beta` will see the new version after it is released.
7. Push your changes to the branch, and merge it to `master`.
8. Update your local `ktlint-intellij-plugin` `master` branch; verify you see the `gradle.properties` and `CHANGELOG.md` changes locally.
9. Do *not* tag the release manually, but go to the latest [Draft Release](https://github.com/nbadal/ktlint-intellij-plugin/releases). Check that release notes are up-to-date. It is not needed to add the ZIP file containing the plugin, as it will be published via the marketplace. 
10. Check that the release is available on [Marketplace Versions](https://plugins.jetbrains.com/plugin/15057-ktlint/versions?noRedirect=true). Note that it might take a full business day before the plugin is actually released due to a manual review by Jetbrains.
11. Check that screenshots on the [Marketplace Overview](https://plugins.jetbrains.com/plugin/15057-ktlint?noRedirect=true) are up-to-date.
12. Announce release on Ktlint Slack channel
13. Update `gradle.properties` with the new `beta` version, and add the section below to the top of `CHANGELOG.md` and commit. (This can be done directly in the main repo or in your fork.)
```markdown
## [Unreleased]

### Added

### Removed

### Fixed

### Changed
```
Note: the heading "[Unreleased]" may not be changed as this will result in incorrect release notes being extracted from the 'changelog.md' file. 