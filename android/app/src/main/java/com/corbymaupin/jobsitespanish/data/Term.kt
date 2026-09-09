package com.corbymaupin.jobsitespanish.data

/**
 * Static vocabulary card from assets/terms.json.
 * [id] matches web cardKey: en + "|" + es
 */
data class Term(
    val id: String,
    val en: String,
    val es: String,
    val trade: String,
    val region: String = ""
)

/**
 * Term joined with persisted Leitner progress (mirrors web deck card).
 */
data class DeckCard(
    val term: Term,
    val box: Int = 1,
    val introduced: Boolean = false,
    val nextReview: String? = null // YYYY-MM-DD
) {
    val id: String get() = term.id
    val en: String get() = term.en
    val es: String get() = term.es
    val trade: String get() = term.trade
    val region: String get() = term.region
}

data class CardProgress(
    val box: Int = 1,
    val introduced: Boolean = false,
    val nextReview: String? = null
)

data class StreakState(
    val last: String = "",
    val days: Int = 0
)

/**
 * Persisted working set for resume (state.session on web).
 */
data class SessionSnapshot(
    val trade: String = "All",
    val keys: List<String> = emptyList(),
    val graduations: Map<String, Int> = emptyMap(),
    val isCategoryMode: Boolean = false
)
