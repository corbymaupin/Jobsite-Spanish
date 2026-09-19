// app/src/main/java/com/corbymaupin/jobsitespanish/ui/AppViewModel.kt
package com.corbymaupin.jobsitespanish.ui

import android.app.Application
import android.util.Log
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
import kotlin.coroutines.cancellation.CancellationException

data class StudyUiState(
    val phase: StudyPhase = StudyPhase.Home,
    val trade: String = "All",
    val current: DeckCard? = null,
    val revealed: Boolean = false,
    /**
     * Kept for compatibility. Mirrors the learning-direction preference
     * (true only for LEARN_ENGLISH). Never randomized.
     */
    val promptEnFirst: Boolean = LearningDirection.DEFAULT.promptIsEnglish,
    val isNew: Boolean = false,

    val right: Int = 0,
    val wrong: Int = 0,
    val remaining: Int = 0,
    val dueCount: Int = 0,
    val newPoolCount: Int = 0,
    val canResume: Boolean = false,
    /** False until terms + progress have loaded once (success or failure). Gates Start buttons. */
    val ready: Boolean = false
)

/**
 * Home = no active lesson (the visible lobby is HomeScreen), Session = card flow, Done = results.
 */
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

    private val _learningDirection = MutableStateFlow(LearningDirection.DEFAULT)

    /** App-wide learning direction from the Home toggle. */
    val learningDirection: StateFlow<LearningDirection> = _learningDirection.asStateFlow()

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

    /** Set when the user taps the toggle, so a slow first DataStore read can't undo the tap. */
    private var directionPickedByUser = false

    /** Blocks a double-tap from building two sessions (and introducing two batches of new cards). */
    private var sessionLaunchInFlight = false

    /** Blocks a double-tap from grading the same card twice. */
    private var gradeInFlight = false

    init {
        viewModelScope.launch {
            loadLearningDirection()
            try {
                refreshAll()
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                // Keep UI up even if assets/DataStore fail on first open
                Log.e(TAG, "refreshAll failed", t)
                _study.value = _study.value.copy(ready = true)
            }
        }
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
        _study.value = _study.value.copy(
            canResume = snap != null && snap.keys.isNotEmpty() && !snap.isCategoryMode,
            ready = true
        )
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

    // ---- Learning direction ----

    /** Home toggle. Updates the flow immediately, then persists. */
    fun setLearningDirection(d: LearningDirection) {
        directionPickedByUser = true
        applyDirection(d)
        viewModelScope.launch {
            try {
                store.setLearningDirection(d.storageValue)
            } catch (c: CancellationException) {

                throw c
            } catch (t: Throwable) {
                Log.w(TAG, "Saving learning direction failed", t)
            }
        }
    }

    private suspend fun loadLearningDirection() {
        val saved = try {
            LearningDirection.fromStorage(store.learningDirectionFlow.first())
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Log.w(TAG, "Loading learning direction failed; using default", t)
            LearningDirection.DEFAULT
        }
        if (!directionPickedByUser) applyDirection(saved)
    }

    private fun applyDirection(d: LearningDirection) {
        val changed = _learningDirection.value != d
        _learningDirection.value = d
        val s = _study.value
        // A card already revealed in the old direction goes back to its (new) front.
        val restartCard = changed && s.phase == StudyPhase.Session && s.revealed
        if (restartCard) speech.stop()
        _study.value = s.copy(
            promptEnFirst = d.promptIsEnglish,
            revealed = if (restartCard) false else s.revealed
        )
    }


    // ---- Study session ----

    fun startSession(categoryMode: Boolean = false) {
        if (sessionLaunchInFlight) return
        sessionLaunchInFlight = true
        val trade = _study.value.trade
        // Show the lesson surface right away so Home -> Study never flashes a stale screen.
        _study.value = _study.value.copy(
            phase = StudyPhase.Session,
            current = null,
            revealed = false,
            isNew = false,
            right = 0,
            wrong = 0,
            remaining = 0
        )
        viewModelScope.launch {
            try {
                val (session, fresh) = if (categoryMode && trade != "All") {
                    val s = sessionBuilder.buildCategorySet(deck, trade) ?: run {
                        _study.value = _study.value.copy(phase = StudyPhase.Home)
                        return@launch
                    }
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
            } finally {
                sessionLaunchInFlight = false
            }
        }
    }

    fun resumeSession() {
        if (sessionLaunchInFlight) return
        sessionLaunchInFlight = true
        _study.value = _study.value.copy(
            phase = StudyPhase.Session,
            current = null,
            revealed = false,
            isNew = false,
            remaining = 0
        )
        viewModelScope.launch {
            try {
                val snap = store.sessionFlow.first()
                val session = snap?.let { sessionBuilder.resumeFromSnapshot(deck, it) }

                if (session == null) {
                    // Nothing resumable after all: fall back to the idle state.
                    _study.value = _study.value.copy(phase = StudyPhase.Home, canResume = false)
                    return@launch
                }
                liveSession = session
                _study.value = _study.value.copy(
                    phase = StudyPhase.Session,
                    trade = session.trade,
                    revealed = false
                )
                nextCard()
            } finally {
                sessionLaunchInFlight = false
            }
        }
    }

    private fun nextCard() {
        val session = liveSession ?: return
        if (session.queue.isEmpty()) {
            finishSession()
            return
        }
        val card = session.queue.removeAt(0)
        _study.value = _study.value.copy(
            current = card,
            revealed = false,
            // Direction is a user preference, not a per-card coin flip.
            promptEnFirst = learningDirection.value.promptIsEnglish,
            isNew = session.isNewCard(card),
            remaining = session.queue.size + 1,

            right = session.right,
            wrong = session.wrong
        )
    }

    /** Flip the current card and speak the ANSWER in its language. No-op if already revealed. */
    fun reveal() {
        val s = _study.value
        val card = s.current ?: return
        if (s.revealed) return
        _study.value = s.copy(revealed = true)
        speakAnswer(card)
    }

    /** "Hear English" / "Hear Spanish": repeat the answer side of the current card. */
    fun hearAnswer() {
        val card = _study.value.current ?: return
        speakAnswer(card)
    }

    /** Always speaks the Spanish side of the current card. */
    fun hearCurrentSpanish() {
        val c = _study.value.current ?: return
        speech.speakSpanish(c.es)
    }

    private fun speakAnswer(card: DeckCard) {
        if (learningDirection.value.answerLanguage == "Spanish") {
            speech.speakSpanish(card.es)
        } else {
            speech.speakEnglish(card.en)
        }

    }

    fun grade(right: Boolean) {
        val session = liveSession ?: return
        val current = _study.value.current ?: return
        if (!_study.value.revealed || gradeInFlight) return
        gradeInFlight = true
        viewModelScope.launch {
            try {
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
                refreshStudyHome()
                // The lesson was ended or replaced while saving: don't advance it.
                if (liveSession !== session || _study.value.phase != StudyPhase.Session) return@launch
                if (session.queue.isEmpty()) {
                    finishSession()
                } else {
                    nextCard()
                }
            } finally {

                gradeInFlight = false
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

    /** Return the Study tab to its idle state. The visible lobby is HomeScreen. */
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

    override fun onCleared() {
        speech.shutdown()
        super.onCleared()
    }

    companion object {
        private const val TAG = "AppViewModel"
    }
}

