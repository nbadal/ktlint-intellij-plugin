plugins {
    id("ktlint-ruleset")
}

ktlintRuleset {
    version = "1.2.1"
    includeKotlinxExcludes = false
    addEc4jCoreConstraint = true
}
