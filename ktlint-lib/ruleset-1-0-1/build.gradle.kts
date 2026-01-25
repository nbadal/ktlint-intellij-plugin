plugins {
    id("ktlint-ruleset")
}

ktlintRuleset {
    version = "1.0.1"
    includeKotlinxExcludes = false
    addEc4jCoreConstraint = true
}
