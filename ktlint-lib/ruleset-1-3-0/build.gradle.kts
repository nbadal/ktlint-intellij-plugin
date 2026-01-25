plugins {
    id("ktlint-ruleset")
}

ktlintRuleset {
    version = "1.3.0"
    addEc4jCoreConstraint = true
}
