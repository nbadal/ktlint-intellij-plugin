package com.nbadal.ktlint.plugin

enum class KtlintMode(
    private val ktlintFeatures: Set<KtlintFeature>,
) {
    /**
     * Ktlint plugin settings have not yet been saved for this project.
     */
    NOT_INITIALIZED(
        setOf(
            KtlintFeature.SHOW_MENU_OPTION_FORMAT_WITH_KTLINT,
            KtlintFeature.SHOW_PROBLEM_WITH_NUMBER_OF_KTLINT_VIOLATIONS_THAT_CAN_BE_AUTOCORRECTED,
            KtlintFeature.SHOW_INTENTION_TO_DISPLAY_ALL_VIOLATIONS,
            KtlintFeature.SHOW_INTENTION_SETTINGS_DIALOG,
            KtlintFeature.AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND,
        ),
    ),

    /**
     *  Ktlint is fully enabled for the project. Location code will be formatted automatically. Violations which can be autocorrected,
     * are not displayed.
     */
    DISTRACT_FREE(
        setOf(
            KtlintFeature.DISPLAY_VIOLATION_WHICH_CAN_NOT_BE_AUTOCORRECTED_AS_ERROR,
            KtlintFeature.FORMAT_WITH_KTLINT_ON_SAVE,
            KtlintFeature.POST_FORMAT_WITH_KTLINT,
            KtlintFeature.SHOW_INTENTION_TO_DISPLAY_ALL_VIOLATIONS,
            KtlintFeature.SHOW_INTENTION_TO_SUPPRESS_VIOLATION,
            // Although Ktlint is executed automatically on files being edited, this option is still useful for mass formatting on files
            // without having the need to edit each file individually.
            KtlintFeature.SHOW_MENU_OPTION_FORMAT_WITH_KTLINT,
        ),
    ),

    /**
     * Ktlint is enabled for the project. All lint violations will be shown as warning as the user is allowed to ignore violations which
     * cannot be autocorrected. User has to trigger format with ktlint manually.
     */
    MANUAL(
        setOf(
            KtlintFeature.DISPLAY_ALL_VIOLATIONS,
            KtlintFeature.SHOW_MENU_OPTION_FORMAT_WITH_KTLINT,
            KtlintFeature.SHOW_PROBLEM_WITH_NUMBER_OF_KTLINT_VIOLATIONS_THAT_CAN_BE_AUTOCORRECTED,
            KtlintFeature.SHOW_INTENTION_TO_DISPLAY_ALL_VIOLATIONS,
            KtlintFeature.SHOW_INTENTION_TO_SUPPRESS_VIOLATION,
        ),
    ),

    /**
     * Ktlint is fully disabled for the project. Neither lint nor format will run.
     */
    DISABLED(emptySet()),
    ;

    fun isEnabled(ktlintFeature: KtlintFeature) = ktlintFeatures.contains(ktlintFeature)
}

enum class KtlintFeature {
    /**
     * Display a problem for each violation that can not be autocorrected.
     */
    DISPLAY_VIOLATION_WHICH_CAN_NOT_BE_AUTOCORRECTED_AS_ERROR,

    /**
     * Displays a problem for each ktlint violation.
     */
    DISPLAY_ALL_VIOLATIONS,

    /**
     * Runs ktlint formatting after normal IDEA formatting.
     */
    POST_FORMAT_WITH_KTLINT,

    /**
     * On save of a kotlin file, apply Ktlint formatting to the file if the corresponding on-save-action is enabled.
     */
    FORMAT_WITH_KTLINT_ON_SAVE,

    /**
     * Shows a file analysis problem when Ktlint has found violations that can be autocorrected, but which are not fixed automatically as
     * ktlint format is not run automatically.
     */
    SHOW_PROBLEM_WITH_NUMBER_OF_KTLINT_VIOLATIONS_THAT_CAN_BE_AUTOCORRECTED,

    /**
     * Allows the developer to manually format the files or directories selected in the project explorer.
     */
    SHOW_MENU_OPTION_FORMAT_WITH_KTLINT,

    /**
     * Shows the intention to open the settings dialog.
     */
    SHOW_INTENTION_SETTINGS_DIALOG,

    /**
     * Shows the intention to suppress an individual violation.
     */
    SHOW_INTENTION_TO_SUPPRESS_VIOLATION,

    /**
     * Shows the intention to display all violations in the current file.
     */
    SHOW_INTENTION_TO_DISPLAY_ALL_VIOLATIONS,

    /**
     * Makes the developer aware that Ktlint plugin is not yet configured for the project. Banner is however only displayed when an
     * error is found.
     */
    AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND,
}
