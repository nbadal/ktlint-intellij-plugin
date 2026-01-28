plugins {
    id("ktlint-ruleset")
}

ktlintRuleset {
    version = "1.3.0"
    includeKotlinxExcludes = false
    addEc4jCoreConstraint = true
}
