<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# ktlint-intellij-plugin Changelog

## [Unreleased]
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
