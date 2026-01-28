<img src="ktlint-plugin/src/main/resources/META-INF/pluginIcon.svg" alt="plugin icon" width="128" />

# Ktlint IntelliJ Plugin

[![Build](https://github.com/nbadal/ktlint-intellij-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/nbadal/ktlint-intellij-plugin/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/15057.svg)](https://plugins.jetbrains.com/plugin/15057)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/15057.svg)](https://plugins.jetbrains.com/plugin/15057)
[![GitHub license](https://img.shields.io/github/license/nbadal/ktlint-intellij-plugin.svg)](LICENSE)
[![Slack](https://img.shields.io/badge/slack-@kotlinlang/ktlint-yellow.svg?logo=slack)](https://kotlinlang.slack.com/messages/CKS3XG0LS)

Formats code with [ktlint](https://pinterest.github.io/ktlint/) after IntelliJ IDEA formatting or on save of file.

Ktlint is an anti-bikeshedding linter/formatter for Kotlin code based on the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html), [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide), and other best practices.

## Installation

- Install via <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> and search for "ktlint".
- For beta versions, add the plugin repository:
  <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Manage plugin repositories</kbd> and add
  `https://plugins.jetbrains.com/plugins/list?channel=beta&pluginId=com.nbadal.ktlint`.
- Manual install: download the plugin from the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/15057-ktlint).

## Usage

- Configure in <kbd>Preferences</kbd> > <kbd>Tools</kbd> > <kbd>KtLint</kbd>.
- Ktlint is configured via `.editorconfig` (see [Ktlint configuration](https://pinterest.github.io/ktlint/latest/rules/configuration-ktlint/)).

## Compatibility

- IntelliJ IDEA Platform compatibility is defined by `pluginSinceBuild` / `pluginUntilBuild` in [`gradle.properties`](gradle.properties).
- Default ktlint version is defined in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Project structure

This repo is a multi-module project:
- `ktlint-plugin` - IntelliJ plugin code.
- `ktlint-lib` - Relocates ktlint artifacts and ships multiple ruleset versions.

## Development and maintenance

See `CONTRIBUTING.md` for contribution, local development, and ruleset/version maintenance. Release and publishing steps are in `MAINTAINERS.md`.

---
Plugin based on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).
