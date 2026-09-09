package com.corbymaupin.jobsitespanish.srs

import com.corbymaupin.jobsitespanish.data.CardProgress
import com.corbymaupin.jobsitespanish.data.DeckCard
import com.corbymaupin.jobsitespanish.data.StreakState
import com.corbymaupin.jobsitespanish.data.Term
import java.util.Calendar
import java.util.Locale

/**
 * Core Leitner scheduling ported from index.html BOX_INTERVALS / grade().
 */
object Leitner {
    val BOX_INTERVALS: Map<Int, Int> = mapOf(1 to 1, 2 to 2, 3 to 4, 4 to 8, 5 to 16)
    const val MAX_BOX = 5
    const val NEW_RATIO = 0.3
    const val GRADUATE_AT = 2

    fun todayKey(calendar: Calendar = Calendar.getInstance()): String {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    fun addDays(dateKey: String, days: Int): String {
        val p = dateKey.split("-")
        val cal = Calendar.getInstance()
        cal.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt(), 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, days)
        return todayKey(cal)
    }

    fun daysBetween(a: String, b: String): Int {
        val pa = a.split("-")
        val pb = b.split("-")
        val ca = Calendar.getInstance().apply {
            set(pa[0].toInt(), pa[1].toInt() - 1, pa[2].toInt(), 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val cb = Calendar.getInstance().apply {
            set(pb[0].toInt(), pb[1].toInt() - 1, pb[2].toInt(), 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val ms = cb.timeInMillis - ca.timeInMillis
        return Math.round(ms / 86400000.0).toInt()
    }

    fun nextReviewFor(box: Int, today: String = todayKey()): String {
        val interval = BOX_INTERVALS[box.coerceIn(1, MAX_BOX)] ?: 1
        return addDays(today, interval)
    }

    fun joinDeck(terms: List<Term>, progress: Map<String, CardProgress>): List<DeckCard> =
        terms.map { t ->
            val p = progress[t.id]
            DeckCard(
                term = t,
                box = p?.box ?: 1,
                introduced = p?.introduced == true,
                nextReview = p?.nextReview
            )
        }

    fun inTrade(card: DeckCard, trade: String): Boolean =
        trade == "All" || card.trade == trade

    fun dueCards(deck: List<DeckCard>, trade: String, today: String = todayKey()): List<DeckCard> =
        deck.filter { c ->
            c.introduced && c.nextReview != null && c.nextReview!! <= today && inTrade(c, trade)
        }

    fun uninitiatedCards(deck: List<DeckCard>, trade: String): List<DeckCard> =
        deck.filter { !it.introduced && inTrade(it, trade) }

    fun boxCounts(deck: List<DeckCard>): IntArray {
        val counts = IntArray(5)
        deck.forEach { c ->
            if (c.introduced) counts[(c.box - 1).coerceIn(0, 4)]++
        }
        return counts
    }

    fun introducedCount(deck: List<DeckCard>): Int = deck.count { it.introduced }

    fun masteredCount(deck: List<DeckCard>): Int =
        deck.count { it.introduced && it.box >= 4 }

    /** Right answer: bump box, schedule next review. */
    fun onCorrect(card: DeckCard, today: String = todayKey()): DeckCard {
        val newBox = (card.box + 1).coerceAtMost(MAX_BOX)
        return card.copy(box = newBox, nextReview = nextReviewFor(newBox, today), introduced = true)
    }

    /** Wrong answer: drop to box 1. */
    fun onMiss(card: DeckCard, today: String = todayKey()): DeckCard =
        card.copy(box = 1, nextReview = nextReviewFor(1, today), introduced = true)

    fun bumpStreak(streak: StreakState, today: String = todayKey()): StreakState {
        if (streak.last == today) return streak
        val days = if (streak.last.isNotEmpty() && daysBetween(streak.last, today) == 1) {
            streak.days + 1
        } else {
            1
        }
        return StreakState(last = today, days = days)
    }

    fun currentStreak(streak: StreakState, today: String = todayKey()): Int {
        if (streak.last.isEmpty()) return 0
        val gap = daysBetween(streak.last, today)
        return if (gap <= 1) streak.days else 0
    }

    fun toProgress(card: DeckCard): CardProgress =
        CardProgress(box = card.box, introduced = card.introduced, nextReview = card.nextReview)
}
