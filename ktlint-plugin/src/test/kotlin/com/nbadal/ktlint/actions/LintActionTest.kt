package com.nbadal.ktlint.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.nbadal.ktlint.KtlintFeature.DISPLAY_ALL_VIOLATIONS
import com.nbadal.ktlint.isEnabled
import com.nbadal.ktlint.ktlintAnnotatorUserData
import com.nbadal.ktlint.removeKtlintAnnotatorUserData
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import testhelper.KtlintRuleEngineTestCase

class LintActionTest : KtlintRuleEngineTestCase() {
    override fun setUp() {
        super.setUp()
        // By default, the action should be visible
        every { project.isEnabled(DISPLAY_ALL_VIOLATIONS) } returns false
        with(ActionManager.getInstance()) {
            if (getAction("Ktlint.Lint") == null) {
                registerAction("Ktlint.Lint", LintAction())
            }
        }
    }

    fun `test LintAction is not visible when feature display all violations is enabled`() {
        every { project.isEnabled(DISPLAY_ALL_VIOLATIONS) } returns true
        createKotlinFile("Foo.kt")
        configureFiles()

        val presentation = myFixture.testAction(LintAction())

        assertThat(presentation.isVisible).isFalse
    }

    fun `test LintAction is not visible for a non-kotlin file`() {
        createFile("some-file.txt", "some text")
        configureFiles()

        val presentation = myFixture.testAction(LintAction())

        assertThat(presentation.isVisible).isFalse
    }

    fun `test LintAction is visible for a kotlin file`() {
        createKotlinFile("Foo.kt")
        configureFiles()

        val presentation = myFixture.testAction(LintAction())

        assertThat(presentation.isEnabledAndVisible).isTrue
    }

    fun `test LintAction on a kotlin file sets the user data`() {
        val kotlinFile = createKotlinFile("Foo.kt")
        val document = openFilesAsUnsavedDocuments(kotlinFile).first()
        document.removeKtlintAnnotatorUserData()

        myFixture.testAction(LintAction())

        assertThat(document.ktlintAnnotatorUserData?.displayAllKtlintViolations).isTrue
    }

    fun `test LintAction on a kotlin script file sets the user data`() {
        val kotlinScriptFile = createKotlinFile("Bar.kts")
        val document = openFilesAsUnsavedDocuments(kotlinScriptFile).first()
        document.removeKtlintAnnotatorUserData()

        myFixture.testAction(LintAction())

        assertThat(document.ktlintAnnotatorUserData?.displayAllKtlintViolations).isTrue
    }
}
