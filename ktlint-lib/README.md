# ktlint-lib module

The plugin encapsulates the artifacts of ktlint so it does not conflict with the IDE. For example, the embedded Kotlin compiler in the KtlintRuleEngine conflicts with the Kotlin compiler provided by IntelliJ IDEA. The `ktlint-lib` module relocates those artifacts and provides multiple ruleset versions.

For ruleset maintenance and version updates, see `CONTRIBUTING.md`.
