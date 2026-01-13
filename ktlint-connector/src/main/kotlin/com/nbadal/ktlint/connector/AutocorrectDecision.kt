package com.nbadal.ktlint.connector

enum class AutocorrectDecision {
    /**
     * Autocorrect lint violation if supported by the [Rule].
     */
    ALLOW_AUTOCORRECT,

    /**
     * Do not autocorrect lint violation even when this is supported by the [Rule].
     */
    NO_AUTOCORRECT,
}
