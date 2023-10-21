<img src='/plugin/src/main/resources/META-INF/pluginIcon.svg?raw=true' alt="plugin icon" width='128' />  

# ktlint-intellij-plugin

[![Build](https://github.com/nbadal/ktlint-intellij-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/nbadal/ktlint-intellij-plugin/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/15057.svg)](https://plugins.jetbrains.com/plugin/15057)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/15057.svg)](https://plugins.jetbrains.com/plugin/15057)
[![GitHub license](https://img.shields.io/github/license/nbadal/ktlint-intellij-plugin.svg)](https://github.com/nbadal/ktlint-intellij-plugin/blob/master/LICENSE.md)

<!-- Plugin description -->
[KtLint](https://pinterest.github.io/ktlint/) is an anti bikeshedding linter/formatter for Kotlin code.

This plugin applies KtLint formatting after IntelliJ IDEA formatting. Formatting is also applied on save of file. Lint violations that can not be fixed by KtLint are highlighted as error for manual resolving. KtLint should be configured via ".editorconfig". See [KtLint configuration](https://pinterest.github.io/ktlint/latest/rules/configuration-ktlint/).

Enable and configure in `Preferences` > `Tools` > `KtLint Format`.

This plugin is based on the "Ktlint (unofficial)" plugin, developed by Nick Badal. The "KtLint Format" plugin is maintained by the KtLint team. The official plugin puts more focus on the automatic format of Kotlin code by offering fewer features. This results in more consistent code, better performance in IntelliJ IDEA, and less time on fixing errors which can be autocorrected. Finally, the plugin will be updated in same release cycle as KtLint itself.
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "ktlint"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/nbadal/ktlint-intellij-plugin/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
