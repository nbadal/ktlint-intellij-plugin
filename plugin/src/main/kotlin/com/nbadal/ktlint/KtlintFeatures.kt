package com.nbadal.ktlint

import com.nbadal.ktlint.KtlintFeature.AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND
import com.nbadal.ktlint.KtlintFeature.AUTOMATICALLY_DISPLAY_PROBLEM_WITH_NUMBER_OF_VIOLATIONS_FOUND
import com.nbadal.ktlint.KtlintFeature.FORMAT_WITH_KTLINT_ON_SAVE
import com.nbadal.ktlint.KtlintFeature.POST_FORMAT_WITH_KTLINT
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
            AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND,
        ),
    ),
    DISTRACT_FREE(
        setOf(
            FORMAT_WITH_KTLINT_ON_SAVE,
            POST_FORMAT_WITH_KTLINT,
            // Although Ktlint is executed automatically on files being edited, this option is still useful for mass formatting on files
            // without having the need to edit each file individually.
            SHOW_MENU_OPTION_FORMAT_WITH_KTLINT,
        ),
    ),
    MANUAL(
        setOf(
            SHOW_MENU_OPTION_FORMAT_WITH_KTLINT,
            AUTOMATICALLY_DISPLAY_PROBLEM_WITH_NUMBER_OF_VIOLATIONS_FOUND,
        ),
    ),
    DISABLED(emptySet()),
    ;

    fun isEnabled(ktlintFeature: KtlintFeature) = ktlintFeatures.contains(ktlintFeature)
}

enum class KtlintFeature {
    TBD,

    // | Highlight problems which cannot be autocorrected [automatically]                     | *yes                             | *no         | no            |
    // | Highlight problems for all Ktlint violations (in open editor window) [automatically] | *no                              | *yes        | no            |
    // | Highlight problems for all Ktlint violations (in open editor window) [manually]      | *yes                             | *no         | yes           |
    // | Format with ktlint after normal format [automatically]                               | *yes                             | *no         | no            |
    POST_FORMAT_WITH_KTLINT,

    /**
     * On save of a kotlin file, apply Ktlint formatting to the file if the corresponding on-save-action is enabled.
     */
    FORMAT_WITH_KTLINT_ON_SAVE,

    // | Format with Ktlint [manually]                                                        | *no                              | *yes        | yes           |
    // | Format file (in open editor window) [manually]                                       | *no                              | *yes        | yes           |

    /**
     * Allows the developer to manually format the files or directories selected in the project explorer.
     */
    SHOW_MENU_OPTION_FORMAT_WITH_KTLINT,

    // | Suppress ktlint violation [manually]                                                 | *yes                             | *yes        | no            |

    // | Display problem with number of problems found by ktlint [automatically]              | *no                              | *yes        | no            |
    AUTOMATICALLY_DISPLAY_PROBLEM_WITH_NUMBER_OF_VIOLATIONS_FOUND,

    /**
     * Makes the developer aware that Ktlint plugin is not yet configured for the project. Banner is however only displayed when an
     * error is found.
     */
    AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND,
}