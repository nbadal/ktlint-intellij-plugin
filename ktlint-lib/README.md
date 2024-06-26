# Why is this a module?

The `ktlint-lib` contains several libraries to isolate functionalities from Ktlint in such a way that it does not conflict with the plugin.

## Core

The Ktlint RuleEngine core module requires certain elements of the kotlin compiler. As of that it includes a dependency on the embedded kotlin compiler library. This clashes and conflicts with classes we use in the JetBrains Kotlin plugin.

## Ruleset

Each ruleset library transforms the StandardRuleSetProvider of that version to a unique class name so that multiple versions of the Standard rule sets can be supported by the plugin. The plugin allows the user to configure one of the supported ktlint versions. In this way, the user can keep the configuration of the ktlint intellij plugin in sync with other plugins like the ktlint gradle plugin or kotlinter.

# ktlint-lib

The ktlint-lib module itself contains the `KtlintRulesetVersion` class which contains the references to the distinct versions of the rulesets. Also, it contains the `ShadowJarMinimizeHelper` class containing references to classes which are also used in the `plugin` module to prevent that those classes are removed by the minimize process of the Shadow Jar. 