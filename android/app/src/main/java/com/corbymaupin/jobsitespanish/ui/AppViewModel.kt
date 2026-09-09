package com.corbymaupin.jobsitespanish.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.corbymaupin.jobsitespanish.data.DeckCard
import com.corbymaupin.jobsitespanish.data.ProgressStore
import com.corbymaupin.jobsitespanish.data.StreakState
import com.corbymaupin.jobsitespanish.data.TermsRepository
import com.corbymaupin.jobsitespanish.srs.Leitner
import com.corbymaupin.jobsitespanish.srs.SessionBuilder
import com.corbymaupin.jobsitespanish.tts.Speech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

data class StudyUiState(
    val phase: StudyPhase = StudyPhase.Home,
    val trade: String = "All",
    val current: DeckCard? = null,
    val revealed: Boolean = false,
    val promptEnFirst: Boolean = true,
    val isNew: Boolean = false,
    val right: Int = 0,
    val wrong: Int = 0,
    val remaining: Int = 0,
    val dueCount: Int = 0,
    val newPoolCount: Int = 0,
    val canResume: Boolean = false
)

enum class StudyPhase { Home, Session, Done }

data class StatsUiState(
    val total: Int = 0,
    val introduced: Int = 0,
    val mastered: Int = 0,
    val due: Int = 0,
    val streak: Int = 0,
    val boxCounts: List<Int> = listOf(0, 0, 0, 0, 0)
)

data class ListenUiState(
    val playing: Boolean = false,
    val trade: String = "All",
    val index: Int = 0,
    val total: Int = 0,
    val sideLabel: String = "Paused",
    val text: String = "Tap play to hear cards",
    val cardTrade: String = ""
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val termsRepo = TermsRepository(app)
    private val store = ProgressStore(app)
    private val sessionBuilder = SessionBuilder()
    val speech = Speech(app)

    private val deck = mutableListOf<DeckCard>()
    private var liveSession: SessionBuilder.LiveSession? = null

    private val _study = MutableStateFlow(StudyUiState())
    val study: StateFlow<StudyUiState> = _study.asStateFlow()

    private val _stats = MutableStateFlow(StatsUiState())
    val stats: StateFlow<StatsUiState> = _stats.asStateFlow()

    private val _listen = MutableStateFlow(ListenUiState())
    val listen: StateFlow<ListenUiState> = _listen.asStateFlow()

    private val _browseTrade = MutableStateFlow("All")
    val browseTrade: StateFlow<String> = _browseTrade.asStateFlow()

    private val _trades = MutableStateFlow<List<String>>(emptyList())
    val trades: StateFlow<List<String>> = _trades.asStateFlow()

    private val _browseCards = MutableStateFlow<List<DeckCard>>(emptyList())
    val browseCards: StateFlow<List<DeckCard>> = _browseCards.asStateFlow()

    private var listenQueue: List<DeckCard> = emptyList()
    private var listenIndex = 0
    private var listenStep = 0 // 0 = es, 1 = en
    private var listenPlaying = false

    private var streak = StreakState()

    init {
        viewModelScope.launch { refreshAll() }
    }

    private suspend fun refreshAll() {
        val terms = termsRepo.loadTerms()
        val progress = store.progressFlow.first()
        streak = store.streakFlow.first()
        deck.clear()
        deck.addAll(Leitner.joinDeck(terms, progress))
        _trades.value = listOf("All") + termsRepo.trades()
        refreshStats()
        refreshBrowse()
        refreshStudyHome()
        val snap = store.sessionFlow.first()
        _study.value = _study.value.copy(canResume = snap != null && snap.keys.isNotEmpty() && !snap.isCategoryMode)
    }

    private fun refreshStats() {
        val today = Leitner.todayKey()
        _stats.value = StatsUiState(
            total = deck.size,
            introduced = Leitner.introducedCount(deck),
            mastered = Leitner.masteredCount(deck),
            due = Leitner.dueCards(deck, "All", today).size,
            streak = Leitner.currentStreak(streak, today),
            boxCounts = Leitner.boxCounts(deck).toList()
        )
    }

    private fun refreshBrowse() {
        val t = _browseTrade.value
        _browseCards.value = deck.filter { Leitner.inTrade(it, t) }
    }

    private fun refreshStudyHome() {
        val trade = _study.value.trade
        val today = Leitner.todayKey()
        _study.value = _study.value.copy(
            dueCount = Leitner.dueCards(deck, trade, today).size,
            newPoolCount = Leitner.uninitiatedCards(deck, trade).size
        )
    }

    fun setStudyTrade(trade: String) {
        _study.value = _study.value.copy(trade = trade)
        refreshStudyHome()
    }

    fun setBrowseTrade(trade: String) {
        _browseTrade.value = trade
        refreshBrowse()
    }

    fun startSession(categoryMode: Boolean = false) {
        viewModelScope.launch {
            val trade = _study.value.trade
            val (session, fresh) = if (categoryMode && trade != "All") {
                val s = sessionBuilder.buildCategorySet(deck, trade) ?: return@launch
                s to emptyList()
            } else {
                sessionBuilder.buildWorkingSet(deck, trade)
            }
            if (session.queue.isEmpty()) {
                _study.value = _study.value.copy(phase = StudyPhase.Done, right = 0, wrong = 0)
                return@launch
            }
            // Persist introductions
            fresh.forEach { store.commitCard(it.id, Leitner.toProgress(it)) }
            liveSession = session
            if (!session.isCategoryMode) {
                store.saveSession(session.toSnapshot())
            }
            _study.value = _study.value.copy(
                phase = StudyPhase.Session,
                right = 0,
                wrong = 0,
                revealed = false
            )
            nextCard()
        }
    }

    fun resumeSession() {
        viewModelScope.launch {
            val snap = store.sessionFlow.first() ?: return@launch
            val session = sessionBuilder.resumeFromSnapshot(deck, snap) ?: return@launch
            liveSession = session
            _study.value = _study.value.copy(
                phase = StudyPhase.Session,
                trade = session.trade,
                revealed = false
            )
            nextCard()
        }
    }

    private fun nextCard() {
        val session = liveSession ?: return
        if (session.queue.isEmpty()) {
            finishSession()
            return
        }
        val card = session.queue.removeAt(0)
        val enFirst = Random.nextBoolean()
        _study.value = _study.value.copy(
            current = card,
            revealed = false,
            promptEnFirst = enFirst,
            isNew = session.isNewCard(card),
            remaining = session.queue.size + 1,
            right = session.right,
            wrong = session.wrong
        )
    }

    fun reveal() {
        val card = _study.value.current ?: return
        _study.value = _study.value.copy(revealed = true)
        // Speak answer in Spanish if answer is Spanish side
        val answerIsEs = _study.value.promptEnFirst
        if (answerIsEs) speech.speakSpanish(card.es) else speech.speakEnglish(card.en)
    }

    fun grade(right: Boolean) {
        val session = liveSession ?: return
        val current = _study.value.current ?: return
        if (!_study.value.revealed) return
        viewModelScope.launch {
            val updated = sessionBuilder.grade(session, current, right, deck)
            if (!session.isCategoryMode) {
                store.commitCard(updated.id, Leitner.toProgress(updated))
                // Persist any replacement introductions already reflected in deck
                session.graduations.keys.forEach { key ->
                    val c = deck.find { it.id == key }
                    if (c != null) store.commitCard(c.id, Leitner.toProgress(c))
                }
                store.saveSession(session.toSnapshot())
            }
            streak = Leitner.bumpStreak(streak)
            store.setStreak(streak)
            refreshStats()
            refreshBrowse()
            if (session.queue.isEmpty()) {
                finishSession()
            } else {
                nextCard()
            }
        }
    }

    fun endSession() {
        finishSession()
    }

    private fun finishSession() {
        viewModelScope.launch {
            val session = liveSession
            if (session != null && !session.isCategoryMode) {
                if (session.queue.isEmpty()) store.saveSession(null)
                else store.saveSession(session.toSnapshot())
            }
            _study.value = _study.value.copy(
                phase = StudyPhase.Done,
                right = session?.right ?: _study.value.right,
                wrong = session?.wrong ?: _study.value.wrong,
                current = null,
                revealed = false,
                canResume = session != null && session.queue.isNotEmpty() && !session.isCategoryMode
            )
            liveSession = if (session != null && session.queue.isNotEmpty() && !session.isCategoryMode) session else null
            refreshStudyHome()
            refreshStats()
        }
    }

    fun backToHome() {
        _study.value = _study.value.copy(phase = StudyPhase.Home, current = null, revealed = false)
        refreshStudyHome()
        viewModelScope.launch {
            val snap = store.sessionFlow.first()
            _study.value = _study.value.copy(
                canResume = snap != null && snap.keys.isNotEmpty() && !snap.isCategoryMode
            )
        }
    }

    // ---- Listen ----
    fun setListenTrade(trade: String) {
        _listen.value = _listen.value.copy(trade = trade)
        rebuildListenQueue()
    }

    private fun rebuildListenQueue() {
        val trade = _listen.value.trade
        // Prefer introduced; fall back to all if none
        var pool = deck.filter { Leitner.inTrade(it, trade) && it.introduced }
        if (pool.isEmpty()) pool = deck.filter { Leitner.inTrade(it, trade) }
        listenQueue = pool.shuffled()
        listenIndex = 0
        listenStep = 0
        paintListen()
    }

    private fun paintListen() {
        if (listenQueue.isEmpty()) {
            _listen.value = _listen.value.copy(
                text = "Study a few cards first",
                sideLabel = if (listenPlaying) "Nothing" else "Paused",
                total = 0,
                index = 0,
                cardTrade = ""
            )
            return
        }
        val c = listenQueue[listenIndex % listenQueue.size]
        val isEs = listenStep == 0
        _listen.value = _listen.value.copy(
            playing = listenPlaying,
            index = listenIndex + 1,
            total = listenQueue.size,
            sideLabel = if (!listenPlaying) "Paused" else if (isEs) "Spanish" else "English",
            text = if (isEs) c.es else c.en,
            cardTrade = c.trade
        )
    }

    fun toggleListen() {
        if (listenQueue.isEmpty()) rebuildListenQueue()
        listenPlaying = !listenPlaying
        _listen.value = _listen.value.copy(playing = listenPlaying)
        if (listenPlaying) speakListenStep() else speech.stop()
        paintListen()
    }

    private fun speakListenStep() {
        if (!listenPlaying || listenQueue.isEmpty()) return
        val c = listenQueue[listenIndex % listenQueue.size]
        if (listenStep == 0) speech.speakSpanish(c.es) else speech.speakEnglish(c.en)
    }

    fun listenAdvance() {
        if (listenQueue.isEmpty()) return
        listenStep++
        if (listenStep >= 2) {
            listenStep = 0
            listenIndex = (listenIndex + 1) % listenQueue.size
            if (listenIndex == 0) listenQueue = listenQueue.shuffled()
        }
        paintListen()
        if (listenPlaying) speakListenStep()
    }

    fun hearCurrentSpanish() {
        val c = _study.value.current ?: return
        speech.speakSpanish(c.es)
    }

    override fun onCleared() {
        speech.shutdown()
        super.onCleared()
    }
}
