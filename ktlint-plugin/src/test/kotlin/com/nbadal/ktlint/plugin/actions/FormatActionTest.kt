package com.nbadal.ktlint.plugin.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.testFramework.TestActionEvent
import com.nbadal.ktlint.plugin.KtlintFeature
import com.nbadal.ktlint.plugin.KtlintMode
import com.nbadal.ktlint.plugin.actions.FormatAction
import com.nbadal.ktlint.plugin.isEnabled
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import testhelper.KtlintRuleEngineTestCase
import testhelper.SimpleNotification

class FormatActionTest : KtlintRuleEngineTestCase() {
    override fun setUp() {
        super.setUp()
        every { project.isEnabled(KtlintFeature.SHOW_MENU_OPTION_FORMAT_WITH_KTLINT) } returns true
        with(ActionManager.getInstance()) {
            if (getAction("Ktlint.Format") == null) {
                registerAction("Ktlint.Format", FormatAction())
            }
        }
    }

    fun `test Given disabled feature then the format action is not visible`() {
        // Disable the required feature
        every { project.isEnabled(KtlintFeature.SHOW_MENU_OPTION_FORMAT_WITH_KTLINT) } returns false
        // Setup a file for which the menu option would be shown if the feature was enabled
        createKotlinFile("Foo.kt")
        configureFiles()

        val presentation = myFixture.testAction(FormatAction())

        assertThat(presentation.isVisible).isFalse
        assertThat(notifications).isEmpty()
    }

    fun `test Given no selected file then the format action is not visible`() {
        val presentation = myFixture.testAction(FormatAction())

        assertThat(presentation.isVisible).isFalse
        assertThat(notifications).isEmpty()
    }

    fun `test Given a kotlin file then the file is formatted`() {
        val kotlinFile = createKotlinFile("Foo.kt")
        configureFiles()

        val presentation = myFixture.testAction(FormatAction())

        assertThat(presentation.isEnabledAndVisible).isTrue
        assertThat(kotlinFile.isFormattedWithKtlint()).isTrue()
        assertThat(notifications).containsExactly(
            SimpleNotification(
                NotificationType.INFORMATION,
                "Format with Ktlint",
                "Formatting is completed. " +
                    "1 files have been formatted. " +
                    "Files might still contain ktlint violations which can not be autocorrected.",
            ),
        )
    }

    fun `test Given a kotlin script file then the file is formatted`() {
        val kotlinScriptFile = createKotlinFile("Bar.kts")
        configureFiles()

        val presentation = myFixture.testAction(FormatAction())

        assertThat(presentation.isEnabledAndVisible).isTrue
        assertThat(kotlinScriptFile.isFormattedWithKtlint()).isTrue()
        assertThat(notifications).containsExactly(
            SimpleNotification(
                NotificationType.INFORMATION,
                "Format with Ktlint",
                "Formatting is completed. " +
                    "1 files have been formatted. " +
                    "Files might still contain ktlint violations which can not be autocorrected.",
            ),
        )
    }

    fun `test Given a non-kotlin file then the file is not formatted`() {
        val file = createFile("some-file.txt", "some text")
        // Retrieve file timestamp after configuration as the file is being copied resulting in the timestamp to be changed
        val timeStamp = configureFiles().first().timeStamp

        val presentation = myFixture.testAction(FormatAction())

        assertThat(presentation.isEnabledAndVisible).isFalse
        // When the action is not applicable, the file is not changed
        assertThat(file.timeStamp).isEqualTo(timeStamp)
        assertThat(notifications).isEmpty()
    }

    fun `test Given a directory is selected then the format action is enabled and visible`() {
        // In this test we want to simulate that a directory is selected in the project tree. For this the virtual file array in the data
        // object of the events should only contain the virtual file of that directory. The myFixture of the base test class does not seem
        // to support this. So analog to the CodeInsightTestFixtureImpl class, a test event is created in which the virtual file array is
        // mocked.
        val formatAction = FormatAction()
        val dataContext =
            mockk<DataContext> {
                every { getData(CommonDataKeys.PROJECT) } returns project
                every { getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) } returns
                    arrayOf(
                        mockk {
                            every { isDirectory } returns true
                        },
                    )
            }
        val anActionEvent = TestActionEvent.createTestEvent(formatAction, dataContext, null)

        ActionUtil.performDumbAwareUpdate(formatAction, anActionEvent, false)

        assertThat(anActionEvent.presentation.isEnabledAndVisible).isTrue
    }

    fun `test Given a kotlin file which can not be parsed then a warning is shown`() {
        createFile("Foo.kt", "fun cannotBeParsed(")
        configureFiles()

        val presentation = myFixture.testAction(FormatAction())

        assertThat(presentation.isEnabledAndVisible).isTrue
        assertThat(notifications).containsExactly(
            SimpleNotification(
                NotificationType.WARNING,
                "Format with Ktlint",
                "Formatting is completed. 1 files have not been formatted due to an error.",
            ),
        )
    }

    fun `test Given that distract free mode is not enabled then the notification contains a suggestion to enable it`() {
        every { configMock.ktlintMode } returns KtlintMode.MANUAL
        createFile("Foo.kt", "fun foo() = 42")
        configureFiles()

        val presentation = myFixture.testAction(FormatAction())

        assertThat(presentation.isEnabledAndVisible).isTrue
        assertThat(notifications).containsExactly(
            SimpleNotification(
                NotificationType.INFORMATION,
                "Format with Ktlint",
                "Formatting is completed. " +
                    "1 files have been formatted. " +
                    "Files might still contain ktlint violations which can not be autocorrected. " +
                    "Get more value out of ktlint by enabling automatic formatting by using the 'distract free' mode.",
            ),
        )
    }

    fun `test Given a kotlin file and an invalid external ruleset then the file is formatted and an error is shown`() {
        every { configMock.externalJarPaths } returns listOf("/some/non-existing/ruleset.jar")
        val kotlinFile = createKotlinFile("Foo.kt")
        configureFiles()

        val presentation = myFixture.testAction(FormatAction())

        assertThat(presentation.isEnabledAndVisible).isTrue
        assertThat(kotlinFile.isFormattedWithKtlint()).isTrue()
        assertThat(notifications).containsExactly(
            SimpleNotification(
                NotificationType.ERROR,
                "Invalid external ruleset JAR",
                "An error occurred while reading external ruleset file '/some/non-existing/ruleset.jar'. " +
                    "No ktlint ruleset can be loaded from this file.",
            ),
            SimpleNotification(
                NotificationType.INFORMATION,
                "Format with Ktlint",
                "Formatting is completed. " +
                    "1 files have been formatted. " +
                    "Files might still contain ktlint violations which can not be autocorrected.",
            ),
        )
    }
}
