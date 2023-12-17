package com.nbadal.ktlint

import com.nbadal.ktlint.KtlintFeature.AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND
import com.nbadal.ktlint.KtlintFeature.DISPLAY_ALL_VIOLATIONS
import com.nbadal.ktlint.KtlintFeature.DISPLAY_PROBLEM_WITH_NUMBER_OF_VIOLATIONS_FOUND
import com.nbadal.ktlint.KtlintFeature.DISPLAY_VIOLATIONS_WHICH_CAN_NOT_BE_AUTOCORRECTED
import com.nbadal.ktlint.KtlintFeature.FORMAT_WITH_KTLINT_ON_SAVE
import com.nbadal.ktlint.KtlintFeature.POST_FORMAT_WITH_KTLINT
import com.nbadal.ktlint.KtlintFeature.SHOW_INTENTION_FORCE_FORMAT_WITH_KTLINT
import com.nbadal.ktlint.KtlintFeature.SHOW_INTENTION_TO_DISPLAY_ALL_VIOLATIONS
import com.nbadal.ktlint.KtlintFeature.SHOW_INTENTION_TO_SUPPRESS_VIOLATION
import com.nbadal.ktlint.KtlintFeature.SHOW_MENU_OPTION_FORMAT_WITH_KTLINT

/**
 * A feature is a single basic functionality provided by ktlint. Features are grouped into profiles. A user can activate only a single
 * profile.
 */
enum class KtlintFeatureProfile(
    private val ktlintFeatures: Set<KtlintFeature>,
) {
    NOT_YET_CONFIGURED(
        setOf(
            SHOW_MENU_OPTION_FORMAT_WITH_KTLINT,
            SHOW_INTENTION_FORCE_FORMAT_WITH_KTLINT,
            SHOW_INTENTION_TO_DISPLAY_ALL_VIOLATIONS,
            AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND,
        ),
    ),
    DISTRACT_FREE(
        setOf(
            FORMAT_WITH_KTLINT_ON_SAVE,
            POST_FORMAT_WITH_KTLINT,
            SHOW_INTENTION_TO_DISPLAY_ALL_VIOLATIONS,
            SHOW_INTENTION_TO_SUPPRESS_VIOLATION,
            // Although Ktlint is executed automatically on files being edited, this option is still useful for mass formatting on files
            // without having the need to edit each file individually.
            SHOW_MENU_OPTION_FORMAT_WITH_KTLINT,
            DISPLAY_VIOLATIONS_WHICH_CAN_NOT_BE_AUTOCORRECTED,
            DISPLAY_PROBLEM_WITH_NUMBER_OF_VIOLATIONS_FOUND,
        ),
    ),
    MANUAL(
        setOf(
            DISPLAY_ALL_VIOLATIONS,
            SHOW_MENU_OPTION_FORMAT_WITH_KTLINT,
            SHOW_INTENTION_FORCE_FORMAT_WITH_KTLINT,
            SHOW_INTENTION_TO_DISPLAY_ALL_VIOLATIONS,
            SHOW_INTENTION_TO_SUPPRESS_VIOLATION,
            DISPLAY_PROBLEM_WITH_NUMBER_OF_VIOLATIONS_FOUND,
        ),
    ),
    DISABLED(emptySet()),
    ;

    fun isEnabled(ktlintFeature: KtlintFeature) = ktlintFeatures.contains(ktlintFeature)
}

enum class KtlintFeature {
    /**
     * Display a problem for each violation that can not be autocorrected.
     */
    DISPLAY_VIOLATIONS_WHICH_CAN_NOT_BE_AUTOCORRECTED,

    /**
     * Displays a problem for each ktlint violation.
     */
    DISPLAY_ALL_VIOLATIONS,

    // | Highlight problems for all Ktlint violations (in open editor window) [manually]      | *yes                             | *no         | yes           |
    SHOW_MENU_OPTION_TO_DISPLAY_ALL_VIOLATIONS,

    // | Format with ktlint after normal format [automatically]                               | *yes                             | *no         | no            |
    POST_FORMAT_WITH_KTLINT,

    /**
     * On save of a kotlin file, apply Ktlint formatting to the file if the corresponding on-save-action is enabled.
     */
    FORMAT_WITH_KTLINT_ON_SAVE,

    /**
     * Shows an intention to force formatting of the file with ktlint manually.
     */
    SHOW_INTENTION_FORCE_FORMAT_WITH_KTLINT,

    // | Format file (in open editor window) [manually]                                       | *no                              | *yes        | yes           |

    /**
     * Allows the developer to manually format the files or directories selected in the project explorer.
     */
    SHOW_MENU_OPTION_FORMAT_WITH_KTLINT,

    // | Suppress ktlint violation [manually]                                                 | *yes                             | *yes        | no            |
    SHOW_INTENTION_TO_SUPPRESS_VIOLATION,

    /**
     * Shows the intention to display all violations in the current file.
     */
    SHOW_INTENTION_TO_DISPLAY_ALL_VIOLATIONS,

    /**
     * Displays one single problem containing a summary of the number of violations that are ignored (e.g. not displayed individually).
     */
    DISPLAY_PROBLEM_WITH_NUMBER_OF_VIOLATIONS_FOUND,

    /**
     * Makes the developer aware that Ktlint plugin is not yet configured for the project. Banner is however only displayed when an
     * error is found.
     */
    AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND,
}
