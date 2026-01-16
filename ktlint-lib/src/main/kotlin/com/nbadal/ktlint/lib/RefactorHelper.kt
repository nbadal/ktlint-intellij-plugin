package com.nbadal.ktlint.lib

import com.nbadal.ktlint.connector.LintError
import com.nbadal.ktlint.connector.RuleId

@Deprecated("remove after refactor")
fun RuleId.toKtlintCoreRuleId() =
    com.pinterest.ktlint.rule.engine.core.api
        .RuleId(value)

@Deprecated("remove after refactor")
fun LintError.toKtlintCoreLintError() =
    com.pinterest.ktlint.rule.engine.api.LintError(
        line = line,
        col = col,
        ruleId = ruleId.toKtlintCoreRuleId(),
        detail = detail,
        canBeAutoCorrected = canBeAutoCorrected,
    )
