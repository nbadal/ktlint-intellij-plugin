package com.nbadal.ktlint

import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.FILE_RELATED_ERROR
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.SUCCESS
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class KtlintFormatOutputTest {
    @Test
    fun `test Given formatted code then the applied result reports that the file has been changed`() {
        val ktlintFormatOutput = KtlintFormatOutput(KtlintResult(SUCCESS), formattedCode = "val foo = 1\n")

        val appliedKtlintResult = ktlintFormatOutput.appliedKtlintResult()

        assertThat(appliedKtlintResult.fileChangedByFormat).isTrue()
    }

    @Test
    fun `test Given no formatted code then the applied result reports that the file has not been changed`() {
        val ktlintFormatOutput = KtlintFormatOutput(KtlintResult(SUCCESS))

        val appliedKtlintResult = ktlintFormatOutput.appliedKtlintResult()

        assertThat(appliedKtlintResult.fileChangedByFormat).isFalse()
    }

    @Test
    fun `test Given a failed ktlint run then formatted code is rejected`() {
        val failedKtlintResult = KtlintResult(FILE_RELATED_ERROR)

        val construction = { KtlintFormatOutput(failedKtlintResult, formattedCode = "val foo = 1\n") }

        assertThatThrownBy { construction() }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
