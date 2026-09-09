package com.corbymaupin.jobsitespanish.ui.navigation

sealed class Dest(val route: String, val label: String) {
    data object Study : Dest("study", "Study")
    data object Listen : Dest("listen", "Listen")
    data object Browse : Dest("browse", "Browse")
    data object Stats : Dest("stats", "Stats")

    companion object {
        val all = listOf(Study, Listen, Browse, Stats)
    }
}
