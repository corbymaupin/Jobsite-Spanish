// app/src/main/java/com/corbymaupin/jobsitespanish/MainActivity.kt
package com.corbymaupin.jobsitespanish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.corbymaupin.jobsitespanish.ui.AppViewModel

import com.corbymaupin.jobsitespanish.ui.navigation.Dest
import com.corbymaupin.jobsitespanish.ui.navigation.navigateToTab
import com.corbymaupin.jobsitespanish.ui.screens.BrowseScreen
import com.corbymaupin.jobsitespanish.ui.screens.FeedbackScreen
import com.corbymaupin.jobsitespanish.ui.screens.HomeScreen
import com.corbymaupin.jobsitespanish.ui.screens.ListenScreen
import com.corbymaupin.jobsitespanish.ui.screens.StatsScreen
import com.corbymaupin.jobsitespanish.ui.screens.StudyScreen
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteAccent
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteAppTheme
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteBg
import com.corbymaupin.jobsitespanish.ui.theme.JobsiteMuted

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JobsiteAppTheme {
                val nav = rememberNavController()
                val vm: AppViewModel = viewModel()
                val backStack by nav.currentBackStackEntryAsState()
                val current = backStack?.destination?.route

                Scaffold(
                    containerColor = JobsiteBg,
                    bottomBar = {
                        NavigationBar(containerColor = JobsiteBg) {
                            // Six tabs, all visible: Home, Study, Listen, Browse, Stats, Feedback.
                            Dest.all.forEach { dest ->
                                NavigationBarItem(
                                    selected = current == dest.route,

                                    onClick = { nav.navigateToTab(dest) },
                                    icon = { Icon(iconFor(dest), contentDescription = dest.label) },
                                    label = {
                                        // Compact single-line labels so "Feedback" fits a six-item bar.
                                        Text(
                                            dest.label,
                                            fontSize = 11.sp,
                                            letterSpacing = 0.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = JobsiteAccent,
                                        selectedTextColor = JobsiteAccent,
                                        indicatorColor = JobsiteAccent.copy(alpha = 0.18f),
                                        unselectedIconColor = JobsiteMuted,
                                        unselectedTextColor = JobsiteMuted
                                    )
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = nav,
                        startDestination = Dest.Home.route,
                        modifier = Modifier.padding(padding)
                    ) {
                        composable(Dest.Home.route) {
                            HomeScreen(vm = vm, onOpenStudy = { nav.navigateToTab(Dest.Study) })
                        }

                        composable(Dest.Study.route) {
                            StudyScreen(vm = vm, onGoHome = { nav.navigateToTab(Dest.Home) })
                        }
                        composable(Dest.Listen.route) { ListenScreen(vm) }
                        composable(Dest.Browse.route) { BrowseScreen(vm) }
                        composable(Dest.Stats.route) { StatsScreen(vm) }
                        composable(Dest.Feedback.route) { FeedbackScreen() }
                    }
                }
            }
        }
    }

    private fun iconFor(dest: Dest): ImageVector = when (dest) {
        Dest.Home -> Icons.Filled.Home
        Dest.Study -> Icons.Filled.School
        Dest.Listen -> Icons.Filled.Headphones
        Dest.Browse -> Icons.AutoMirrored.Filled.List
        Dest.Stats -> Icons.Filled.BarChart
        Dest.Feedback -> Icons.Filled.Feedback
    }
}
