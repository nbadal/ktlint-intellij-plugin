plugins {
    id("ktlint-ruleset")
}

ktlintRuleset {
    version = "1.0.1"
    addEc4jCoreConstraint = true
}
