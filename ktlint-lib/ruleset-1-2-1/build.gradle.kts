plugins {
    id("ktlint-ruleset")
}

ktlintRuleset {
    version = "1.2.1"
    addEc4jCoreConstraint = true
}
