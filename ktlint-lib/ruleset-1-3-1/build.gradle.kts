plugins {
    id("ktlint-ruleset")
}

ktlintRuleset {
    version = "1.3.1"
    includeKotlinxExcludes = false
    addEc4jCoreConstraint = true
}
