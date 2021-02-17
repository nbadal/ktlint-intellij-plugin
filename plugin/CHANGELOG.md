<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# ktlint-intellij-plugin Changelog

## [Unreleased]
### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security
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
