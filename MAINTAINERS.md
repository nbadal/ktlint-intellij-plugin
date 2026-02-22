# Maintainers

This document is for project maintainers only.

## Release checklist

1) Verification before starting release:
   - Ensure no `build.gradle.kts` depends on `mavenLocal()`. Those may only be used in snapshot versions on a local machine.
   - Ensure no `build.gradle.kts` depends on `maven("https://central.sonatype.com/repository/maven-snapshots/")`. Those may only be used when publishing to the beta or dev channel.
   - Check that the `ktlint` version in `gradle/libs.versions.toml` is updated to the latest version of ktlint.
   - Check that the ruleset for the latest ktlint version has been added to `ktlint-lib`.
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
   - For beta and dev channels, mark the release as "pre-release".
   - Do not upload the ZIP file containing the plugin; it will be published via the marketplace.
   - Publish the release.
8) Check that the "Release" workflow starts on the Actions page.
9) Check that the release is available on the Marketplace Versions page. For the default channel, it can take a couple of business days due to manual review.
10) Check that screenshots on the Marketplace Overview page are up to date.
11) Announce the release on the Ktlint Slack channel.
