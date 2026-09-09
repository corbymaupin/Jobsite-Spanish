package com.corbymaupin.jobsitespanish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.corbymaupin.jobsitespanish.ui.AppViewModel
import com.corbymaupin.jobsitespanish.ui.navigation.Dest
import com.corbymaupin.jobsitespanish.ui.screens.BrowseScreen
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
                            Dest.all.forEach { dest ->
                                NavigationBarItem(
                                    selected = current == dest.route,
                                    onClick = {
                                        nav.navigate(dest.route) {
                                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(iconFor(dest), contentDescription = dest.label) },
                                    label = { Text(dest.label) },
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
                        startDestination = Dest.Study.route,
                        modifier = Modifier.padding(padding)
                    ) {
                        composable(Dest.Study.route) { StudyScreen(vm) }
                        composable(Dest.Listen.route) { ListenScreen(vm) }
                        composable(Dest.Browse.route) { BrowseScreen(vm) }
                        composable(Dest.Stats.route) { StatsScreen(vm) }
                    }
                }
            }
        }
    }

    private fun iconFor(dest: Dest): ImageVector = when (dest) {
        Dest.Study -> Icons.Filled.School
        Dest.Listen -> Icons.Filled.Headphones
        Dest.Browse -> Icons.AutoMirrored.Filled.List
        Dest.Stats -> Icons.Filled.BarChart
    }
}
