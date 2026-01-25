plugins {
    id("ktlint-ruleset")
}

ktlintRuleset {
    version = "1.3.1"
    addEc4jCoreConstraint = true
}
