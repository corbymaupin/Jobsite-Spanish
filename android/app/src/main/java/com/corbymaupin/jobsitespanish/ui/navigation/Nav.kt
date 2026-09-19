// app/src/main/java/com/corbymaupin/jobsitespanish/ui/navigation/Nav.kt
package com.corbymaupin.jobsitespanish.ui.navigation

import androidx.navigation.NavController

sealed class Dest(val route: String, val label: String) {
    data object Home : Dest("home", "Home")
    data object Study : Dest("study", "Study")
    data object Listen : Dest("listen", "Listen")
    data object Browse : Dest("browse", "Browse")
    data object Stats : Dest("stats", "Stats")
    data object Feedback : Dest("feedback", "Feedback")

    companion object {
        /**
         * Bottom-bar order. Home is first and is also the NavHost start destination.
         *
         * Lazy on purpose: an eager list in a sealed-class companion captures null for any
         * subclass that was touched before Dest itself was initialized (for example
         * Dest.Home.route read first), which would crash the bottom bar.
         */
        val all: List<Dest> by lazy { listOf(Home, Study, Listen, Browse, Stats, Feedback) }
    }
}

/**
 * Top-level tab navigation used by the bottom bar and by in-screen buttons
 * (Home -> Study on Start lesson, Study -> Home on Back to Home).
 * Keeps one copy of each tab, saves/restores tab state, and Back always lands on Home.
 */
fun NavController.navigateToTab(dest: Dest) {
    val startId = graph.startDestinationId

    navigate(dest.route) {
        popUpTo(startId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

