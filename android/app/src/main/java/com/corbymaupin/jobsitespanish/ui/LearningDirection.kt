// app/src/main/java/com/corbymaupin/jobsitespanish/ui/LearningDirection.kt
package com.corbymaupin.jobsitespanish.ui

import com.corbymaupin.jobsitespanish.data.DeckCard

/**
 * Which language the learner is practicing. This is a user preference chosen on Home and
 * persisted by ProgressStore under the DataStore key "learning_direction". It is never a
 * per-card coin flip.
 *
 * - [LEARN_SPANISH] (default): prompt = Spanish (term.es), answer = English (term.en).
 * - [LEARN_ENGLISH]: prompt = English (term.en), answer = Spanish (term.es).
 */
enum class LearningDirection(
    /** Value written to DataStore. Never change these strings; they live on users' devices. */
    val storageValue: String,
    /** Label on the Home toggle. */
    val label: String
) {
    LEARN_SPANISH(storageValue = "learn_spanish", label = "Learn Spanish"),
    LEARN_ENGLISH(storageValue = "learn_english", label = "Learn English");

    /** True when the card front shows English. Mirrors the legacy StudyUiState.promptEnFirst. */
    val promptIsEnglish: Boolean get() = this == LEARN_ENGLISH

    /** Language shown on the card front. */
    val promptLanguage: String get() = if (promptIsEnglish) "English" else "Spanish"

    /** Language of the answer: what reveal speaks and what the hear button repeats. */
    val answerLanguage: String get() = if (promptIsEnglish) "Spanish" else "English"

    /** Study screen button label: "Hear English" or "Hear Spanish". */

    val hearAnswerLabel: String get() = "Hear $answerLanguage"

    fun promptOf(card: DeckCard): String = if (promptIsEnglish) card.en else card.es

    fun answerOf(card: DeckCard): String = if (promptIsEnglish) card.es else card.en

    companion object {
        val DEFAULT: LearningDirection = LEARN_SPANISH

        /** Missing or unknown values fall back to [DEFAULT], so a bad value can't break Home. */
        fun fromStorage(raw: String?): LearningDirection =
            entries.firstOrNull { it.storageValue == raw } ?: DEFAULT
    }
}

