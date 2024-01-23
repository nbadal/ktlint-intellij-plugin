<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# ktlint-intellij-plugin Changelog

KtLint maintainer [Paul Dingemans](https://github.com/paul-dingemans) has joined the project.

## [Unreleased]

### What's Changed

* Reload KtLintRuleEngine when changing the rule sets by @paul-dingemans in https://github.com/nbadal/ktlint-intellij-plugin/pull/429
* Highlight a ktlint violation as warning except in distract-free mode, and it can not be autocorrected by @paul-dingemans in https://github.com/nbadal/ktlint-intellij-plugin/pull/437
* Relocate all packages from "org.jetbrains" by @paul-dingemans in https://github.com/nbadal/ktlint-intellij-plugin/pull/445
* Relocate packages more fine grained by @paul-dingemans in https://github.com/nbadal/ktlint-intellij-plugin/pull/454
* Remove plugin verifier script as it is integrated in plugin build by @paul-dingemans in https://github.com/nbadal/ktlint-intellij-plugin/pull/455

### Dependency updates

* Bump actions/cache from 3 to 4 by @dependabot in https://github.com/nbadal/ktlint-intellij-plugin/pull/450
* Bump com.github.gmazzo.buildconfig from 5.1.0 to 5.3.5 by @dependabot in https://github.com/nbadal/ktlint-intellij-plugin/pull/427
* Bump io.mockk:mockk from 1.13.8 to 1.13.9 by @dependabot in https://github.com/nbadal/ktlint-intellij-plugin/pull/439
* Bump JetBrains/qodana-action from 2023.3.0 to 2023.3.1 by @dependabot in https://github.com/nbadal/ktlint-intellij-plugin/pull/453
* Bump ktlint from 1.1.0 to 1.1.1 by @dependabot in https://github.com/nbadal/ktlint-intellij-plugin/pull/442
* Bump org.gradle.toolchains.foojay-resolver-convention from 0.7.0 to 0.8.0 by @dependabot in https://github.com/nbadal/ktlint-intellij-plugin/pull/448
* Bump org.jetbrains.intellij from 1.16.1 to 1.17.0 by @dependabot in https://github.com/nbadal/ktlint-intellij-plugin/pull/452


## [0.20.0] - 2023-12-26

### Changed

- Upgrade KtLint to 1.1.0
- Rework UI and UX
- Plugin can either be run in 'distract free' mode or 'manual' mode.
- In 'distract free' mode, ktlint format is run automatically after Intellij IDEA format, and on save. Violations which can be autocorrected are grouped into 1 single warning showing the number of violations that will be autocorrected. Each violation that can not be autocorrected is shown as error.
- In 'manual' mode, ktlint format is never run automatically. All violations found are shown as error.
- Suppress KtLint violations using `@Suppress` annotation instead of `// ktlint-disable`. This functionality is not yet entire stable. So please raise issues when the `@Suppress` annotation is not inserted properly.
- Error handling is improved.
- Support for IDEA version 233.*

### Removed

- Removed flags and features no longer supported by KtLint 1.x

## [0.13.0] - 2023-08-23

### Changed

- Updated to IntelliJ platform 213-232

## [0.12.0] - 2023-02-03

### Changed

- Upgrade to ktlint 0.48.2
- Updated to IntelliJ platform 213-223

## [0.10.0]

### Changed

- Upgrade to ktlint 0.46.1

## [0.9.1]

### Added

- Support for IDE version 222.*

## [0.9.0]

### Changed

- Upgrade to ktlint 0.45.1

### Fixed

- Show error when ruleset exception is thrown (#188)
- Fix experimental rules not being included (#120)

## [0.8.3]

### Added

- Support for IDE version 221.*

### Changed

- Upgrade to ktlint 0.44.0

## [0.8.2]

### Added

- Support for IDE version 213.*

### Changed

- Upgrade to ktlint 0.43.2

## [0.8.1]

- Upgrade to ktlint 0.42.1

## [0.8.0]

### Changed

- Support for 212.*
- Upgrade to ktlint 0.42.0

### Fixed

- Java 11 dependencies

## [0.7.6]

### Fixed

- Custom rulesets using/comparing PSI classes will now work as intended

## [0.7.5]

### Added

- Manual action for triggering a format via Refactor menu
- Allow for formatting multiple files/folders at once via action

### Fixed

- "Undefined" action name in undo stack
- Annotator now correctly runs inside a read action
- End-of-file line disable comment miscalculation crash
- Crash when uncommitted PSI changes were blocking lint formatting

## [0.7.4]

### Changed

- Updated ktlint to 0.41.0
- Increased platform support to include 2021.1

## [0.7.3]

### Added

- Annotation type now has a "None" option for annotation-less linting

### Fixed

- Missing and duplicate tooltips in settings dialog

## [0.7.2]

### Fixed

- Issue where "Lint After Reformat" setting wasn't loaded from XML
- Shadowing issue that prevented ruleset jars from resolving properly

## [0.7.1]

### Fixed

- Fixed logic for detecting null virtual file / file not in project condition
- Use full path for files, only use project-relative path for baselines. 
  (This should address a bug with .editorconfig not being found in root dir)

## [0.7.0]

### Added

- Support for baseline XML file

## [0.6.1]

### Changed

- Updated ktlint to 0.40.0
- Update kotlin to 1.4.21
- Update internal plugins

## [0.6.0]

### Added

- Added support for 203.* IDEs

### Changed

- Updated internal plugin dependencies

### Fixed

- Fixed crash when ktlint reported a parsing exception.

## [0.5.0]

### Added

- Add post-format linter, with ability to disable via setting
- Add action to errors to allow disabling per-line via comment

### Changed

- Format action no longer appears for non-autocorrectable rules, since it doesn't address them.
- Include user-provided details in bug reports

### Fixed

- Issue where Kotlin fragments (inspector, templates, etc) were being linted

## [0.4.0]

### Added

- Add ability to disable a rule globally from an error annotation
- Add autocompletion to disabled rules field

## [0.3.0]

### Added

- Allow selection of multiple ruleset JARs
- Add github button to settings page
- Support error report submission

### Changed

- Enable ktlint by default on install
- Highlight the individual element specified by ktlint error, instead of the full line

## [0.2.0]

### Added

- Plugin setting for disabled rules
- Add support for .editorconfig
- Add setting for .editorconfig path override
- Add setting for Android mode
- Add setting for an external rule set JAR

### Changed

- Update ktlint to 0.39.0
- Opt-out of kotlint stdlib inclusion
- Load rule sets via classpath, instead of explicitly
- Dependency updates

## [0.1.0]

### Added

- Added action to format file from quick-fix menu of ktlint error annotations
- Added action to disable ktlint from annotations

## [0.0.2]

### Added

- MVP: plugin that runs ktlint scanner and provides annotations. Lots more to do!
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

[Unreleased]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.20.0...HEAD
[0.20.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.13.0...v0.20.0
[0.13.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.12.0...v0.13.0
[0.12.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.11.0...v0.12.0
[0.11.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.10.0...v0.11.0
[0.10.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.9.1...v0.10.0
[0.9.1]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.9.0...v0.9.1
[0.9.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.8.3...v0.9.0
[0.8.3]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.8.2...v0.8.3
[0.8.2]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.8.1...v0.8.2
[0.8.1]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.8.0...v0.8.1
[0.8.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.7.6...v0.8.0
[0.7.6]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.7.5...v0.7.6
[0.7.5]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.7.4...v0.7.5
[0.7.4]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.7.3...v0.7.4
[0.7.3]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.7.2...v0.7.3
[0.7.2]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.7.1...v0.7.2
[0.7.1]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.7.0...v0.7.1
[0.7.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.6.1...v0.7.0
[0.6.1]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.6.0...v0.6.1
[0.6.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/nbadal/ktlint-intellij-plugin/compare/v0.0.2...v0.1.0
[0.0.2]: https://github.com/nbadal/ktlint-intellij-plugin/compare/
