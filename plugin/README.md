<img src='/META-INF/pluginIcon.svg?raw=true' alt="plugin icon" width='128' />  

# ktlint-intellij-plugin

[![Build](https://github.com/nbadal/ktlint-intellij-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/nbadal/ktlint-intellij-plugin/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/15057.svg)](https://plugins.jetbrains.com/plugin/15057)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/15057.svg)](https://plugins.jetbrains.com/plugin/15057)
[![GitHub license](https://img.shields.io/github/license/nbadal/ktlint-intellij-plugin.svg)](https://github.com/nbadal/ktlint-intellij-plugin/blob/master/LICENSE.md)

<!-- Plugin description -->
Formats code with [KtLint](https://pinterest.github.io/ktlint/) after IntelliJ IDEA formatting or on save of file.

Ktlint is an anti bikeshedding linter/formatter for Kotlin code based on the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html), [Androids Kotlin Style guide](https://developer.android.com/kotlin/style-guide), and other best practices.

Violations will be autocorrected when possible based on your configuration in the `.editorconfig` file (see [KtLint configuration](https://pinterest.github.io/ktlint/latest/rules/configuration-ktlint/)). Lint violations that can not be fixed by KtLint are highlighted as error for manual resolving.

This plugin was formerly known as the "Ktlint (unofficial)" plugin, developed by [Nick Badal](https://github.com/nbadal). In collaboration with KtLint maintainer [Paul Dingemans](https://github.com/paul-dingemans), the plugin has now been reworked and rebranded as (official) "KtLint" Plugin. The plugin now puts more focus on the automatic formatting of Kotlin code. This results in more consistent code, better performance in IntelliJ IDEA, and developers spending less time on fixing errors which can be autocorrected. Finally, the plugin will be updated in same release cycle as KtLint itself.

Enable and configure in `Preferences` > `Tools` > `KtLint Format`.
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "ktlint"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually with published version of plugin:

  Download the [latest release](https://github.com/nbadal/ktlint-intellij-plugin/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually with development (e.g. unpublished) version of plugin:
  - Run the Plugin so that the IDEA sandbox is refreshed
  - Create zip file with development version of plugin from IDEA sandbox
    ```shell
    (cd plugin/build/idea-sandbox/plugins && zip  -r ../../ktlint-plugin-dev.zip ktlint)
    ```
  - Install the zip file manually using
    <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
