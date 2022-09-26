<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# ktlint-intellij-plugin Changelog

## [Unreleased]
### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security

## [0.11.0]
### Changed
- Upgrade to ktlint 0.47.1

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
