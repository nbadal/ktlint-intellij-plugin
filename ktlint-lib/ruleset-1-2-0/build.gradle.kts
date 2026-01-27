plugins {
    id("ktlint-ruleset")
}

ktlintRuleset {
    version = "1.2.0"
    includeKotlinxExcludes = false
    addEc4jCoreConstraint = true
}
