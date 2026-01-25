plugins {
    id("ktlint-ruleset")
}

ktlintRuleset {
    version = "1.2.0"
    addEc4jCoreConstraint = true
}
