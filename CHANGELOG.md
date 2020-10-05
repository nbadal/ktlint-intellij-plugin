<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# ktlint-intellij-plugin Changelog

## [Unreleased]
### Added
- Allow selection of multiple ruleset JARs
- Add github button to settings page

### Changed
- Enable ktlint by default on install

### Deprecated

### Removed

### Fixed

### Security
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

### Deprecated

### Removed

### Fixed

### Security
## [0.1.0]
### Added
- Added action to format file from quick-fix menu of ktlint error annotations
- Added action to disable ktlint from annotations

### Changed

### Deprecated

### Removed

### Fixed

### Security

## [0.0.2]
### Added
- MVP: plugin that runs ktlint scanner and provides annotations. Lots more to do!
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
