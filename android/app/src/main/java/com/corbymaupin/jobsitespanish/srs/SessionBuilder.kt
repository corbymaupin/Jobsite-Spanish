package com.corbymaupin.jobsitespanish.srs

import com.corbymaupin.jobsitespanish.data.DeckCard
import com.corbymaupin.jobsitespanish.data.SessionSnapshot
import kotlin.random.Random

/**
 * Session working-set builder: ~30/70 new/review, graduate after 2 correct.
 * Ported from index.html buildWorkingSet / pullReplacement / grade().
 */
class SessionBuilder(private val random: Random = Random.Default) {

    data class LiveSession(
        var trade: String,
        var queue: MutableList<DeckCard>,
        var graduations: MutableMap<String, Int>,
        var isCategoryMode: Boolean = false,
        var right: Int = 0,
        var wrong: Int = 0,
        val retried: MutableSet<String> = mutableSetOf()
    ) {
        fun isNewCard(card: DeckCard): Boolean = graduations.containsKey(card.id)

        fun toSnapshot(): SessionSnapshot = SessionSnapshot(
            trade = trade,
            keys = queue.map { it.id },
            graduations = graduations.toMap(),
            isCategoryMode = isCategoryMode
        )
    }

    fun buildCategorySet(deck: List<DeckCard>, category: String): LiveSession? {
        val cards = deck.filter { it.trade == category }.shuffled(random)
        if (cards.isEmpty()) return null
        return LiveSession(
            trade = category,
            queue = cards.toMutableList(),
            graduations = mutableMapOf(),
            isCategoryMode = true
        )
    }

    fun buildWorkingSet(
        deck: MutableList<DeckCard>,
        trade: String,
        today: String = Leitner.todayKey()
    ): Pair<LiveSession, List<DeckCard>> {
        val due = Leitner.dueCards(deck, trade, today).shuffled(random).toMutableList()
        val pool = Leitner.uninitiatedCards(deck, trade).shuffled(random).toMutableList()

        val newCount = if (due.isEmpty()) {
            minOf(pool.size, 10)
        } else {
            val total = Math.ceil(due.size / (1.0 - Leitner.NEW_RATIO)).toInt()
            minOf(pool.size, total - due.size)
        }

        val fresh = pool.take(newCount).map { c ->
            c.copy(introduced = true, nextReview = today)
        }
        val graduations = fresh.associate { it.id to 0 }.toMutableMap()

        fresh.forEach { f ->
            val idx = deck.indexOfFirst { it.id == f.id }
            if (idx >= 0) deck[idx] = f
        }

        val cards = (due + fresh).shuffled(random).toMutableList()
        val session = LiveSession(
            trade = trade,
            queue = cards,
            graduations = graduations,
            isCategoryMode = false
        )
        return session to fresh
    }

    fun resumeFromSnapshot(
        deck: List<DeckCard>,
        snapshot: SessionSnapshot
    ): LiveSession? {
        val byId = deck.associateBy { it.id }
        val queue = snapshot.keys.mapNotNull { byId[it] }.toMutableList()
        if (queue.isEmpty() && snapshot.keys.isNotEmpty()) return null
        if (queue.isEmpty() && !snapshot.isCategoryMode) return null
        return LiveSession(
            trade = snapshot.trade,
            queue = queue,
            graduations = snapshot.graduations.toMutableMap(),
            isCategoryMode = snapshot.isCategoryMode
        )
    }

    fun pullReplacement(
        deck: MutableList<DeckCard>,
        trade: String,
        excludeKeys: Set<String>,
        today: String = Leitner.todayKey()
    ): DeckCard? {
        val pool = Leitner.uninitiatedCards(deck, trade).filter { it.id !in excludeKeys }
        if (pool.isEmpty()) return null
        val card = pool[random.nextInt(pool.size)].copy(introduced = true, nextReview = today)
        val idx = deck.indexOfFirst { it.id == card.id }
        if (idx >= 0) deck[idx] = card
        return card
    }

    fun grade(
        session: LiveSession,
        current: DeckCard,
        right: Boolean,
        deck: MutableList<DeckCard>,
        today: String = Leitner.todayKey()
    ): DeckCard {
        val wasNew = session.isNewCard(current)
        val updated: DeckCard

        if (right) {
            session.right++
            updated = if (!session.isCategoryMode) Leitner.onCorrect(current, today) else current
            if (wasNew) {
                val g = (session.graduations[current.id] ?: 0) + 1
                if (g >= Leitner.GRADUATE_AT) {
                    session.graduations.remove(current.id)
                    if (!session.isCategoryMode) {
                        val exclude = session.queue.map { it.id }.toSet() + current.id
                        val replacement = pullReplacement(deck, session.trade, exclude, today)
                        if (replacement != null) {
                            session.graduations[replacement.id] = 0
                            session.queue.add(replacement)
                        }
                    }
                } else {
                    session.graduations[current.id] = g
                    session.queue.add(updated)
                }
            }
            // Correct review: leave queue (dropped from working set)
        } else {
            session.wrong++
            updated = if (!session.isCategoryMode) Leitner.onMiss(current, today) else current
            if (current.id !in session.retried || wasNew) {
                session.retried.add(current.id)
                session.queue.add(updated)
            }
        }

        if (!session.isCategoryMode) {
            val idx = deck.indexOfFirst { it.id == updated.id }
            if (idx >= 0) deck[idx] = updated
        }
        return updated
    }
}
