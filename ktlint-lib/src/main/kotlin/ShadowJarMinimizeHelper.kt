@file:Suppress("unused")

import com.pinterest.ktlint.cli.reporter.baseline.BaselineErrorHandling
import com.pinterest.ktlint.cli.reporter.baseline.BaselineLoaderException
import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintParseException
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.KtlintSuppressionAtOffset
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.api.insertSuppression
import com.pinterest.ktlint.rule.engine.core.api.RuleId

// When the Shadow jar is created, it is also minimized. This means that all unreferenced classes are removed. In this class we define
// reference to objects that are used by the plugin so that they will not be removed when minimizing the jar.
private class ShadowJarMinimizeHelper {
    val lintError: LintError? = null
    val xxx = loadBaseline("xxx", BaselineErrorHandling.EXCEPTION)
    val baselineLoaderException: BaselineLoaderException? = null
    val ktlintCliError: KtlintCliError? = null
    val xx = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE
    val ktLintRuleEngine: KtLintRuleEngine? = null
    val ktLintParseException: KtLintParseException? = null
    val ktlintSuppressionAtOffset: KtlintSuppressionAtOffset? = null
    val code = ktLintRuleEngine?.insertSuppression(Code.fromSnippet("", true), KtlintSuppressionAtOffset(1, 1, RuleId("xxx")))
}
