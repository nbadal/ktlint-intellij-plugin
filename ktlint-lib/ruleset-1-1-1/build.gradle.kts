plugins {
    id("ktlint-ruleset")
}

ktlintRuleset {
    version = "1.1.1"
    addEc4jCoreConstraint = true
}
