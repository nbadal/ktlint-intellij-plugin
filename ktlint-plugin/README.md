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

Ktlint is configured in the `.editorconfig` file (see [KtLint configuration](https://pinterest.github.io/ktlint/latest/rules/configuration-ktlint/)). Next to this, the plugin itself has a few configuration options. The plugin can run in distinct modes. 

In the recommended 'distract free' mode, lint violations are automatically corrected when possible. Combined with formatting on save, ktlint enhances your developer experience greatly. Violations that can not be autocorrected are displayed as error.

In the 'manual' mode, ktlint format is not run automatically. Format can still be invoked manually for the entire file (via the refactor menu). If autocorrect for an individual violation is supported by the rule, the autocorrect option is also shown as Quick Fix. All violations found by ktlint are displayed as warning.

This plugin was formerly known as the "Ktlint (unofficial)" plugin, developed by [Nick Badal](https://github.com/nbadal). In collaboration with KtLint maintainer [Paul Dingemans](https://github.com/paul-dingemans), the plugin has now been reworked and rebranded as (official) "KtLint" Plugin. The plugin now puts more focus on the automatic formatting of Kotlin code. This results in more consistent code, better performance in IntelliJ IDEA, and developers spending less time on fixing errors which can be autocorrected. Finally, the plugin will be updated in same release cycle as KtLint itself.

The plugin currently runs with ktlint version `1.5.0` by default (see ktlint preferences to alter the ruleset version). K2 mode is supported starting from Intellij IDEA version 2024.2.1.

Enable and configure in `Preferences` > `Tools` > `KtLint`.
<!-- Plugin description end -->

## Feature set

When the plugin is not yet configured for a project, the basic functionalities (lint and format) can be triggered manually. Also, the user is invited via  banner and notification to configure the plugin.

The "Distract free" mode of the plugin supports users that have already configured ktlint to their liking. Violations that can be autocorrected by Ktlint will not be shown as they will be fixed automatically when invoking the IDEA formatting, or on save of a file.

The "Manual" mode of the plugin supports users that want to decide per lint violation how it should be resolved. It displays all ktlint violations, and never runs ktlint format automatically. Ktlint format can still be triggered manually.

| Feature                                                                              | Distract free mode (recommended) | Manual mode | Disabled mode |
|--------------------------------------------------------------------------------------|----------------------------------|-------------|---------------|
| Highlight problems which cannot be autocorrected [automatically]                     | *yes                             | *no         | no            |
| Highlight problems for all Ktlint violations (in open editor window) [automatically] | *no                              | *yes        | no            |
| Highlight problems for all Ktlint violations (in open editor window) [manually]      | *yes                             | *no         | yes           |
| Format with ktlint after normal format [automatically]                               | *yes                             | *no         | no            |
| Format with ktlint on save [automatically]                                           | *yes                             | *no         | no            |
| Format with Ktlint [manually]                                                        | *no                              | *yes        | yes           |
| Format file (in open editor window) [manually]                                       | *no                              | *yes        | yes           |
| Format selected file(s) with ktlint [manually]                                       | *yes                             | *yes        | yes           |
| Suppress ktlint violation [manually]                                                 | *yes                             | *yes        | no            |
| Display problem with number of problems found by ktlint [automatically]              | *no                              | *yes        | no            |
| Display banner with number of problems found by ktlint [automatically]               | *no                              | *no         | yes           |

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "ktlint"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually with published version of plugin:

  Download the [latest release](https://github.com/nbadal/ktlint-intellij-plugin/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
