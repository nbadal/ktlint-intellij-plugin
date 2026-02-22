package com.nbadal.ktlint.actions

import testhelper.KtlintRuleEngineTestCase

class FormatActionTest : KtlintRuleEngineTestCase() {
    override fun getTestDataPath(): String = "src/test/testData/formatAction"

    private fun actionTest(name: String) {
        myFixture.configureByFile(testFileName(name, postfix = "_before.kt"))
        myFixture.performEditorAction("Ktlint.Format")
        myFixture.checkResultByFile(testFileName(name, postfix = "_after.kt"))
    }

    fun testFormatFileWithLatestKtlintVersion() {
        actionTest(name)
    }
}

fun testFileName(
    name: String,
    postfix: String,
): String = name.substringAfter("test") + postfix
