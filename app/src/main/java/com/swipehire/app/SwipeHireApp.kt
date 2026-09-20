package com.swipehire.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.swipehire.app.data.MockData
import com.swipehire.app.ui.components.SwipeHireBottomBar
import com.swipehire.app.ui.components.SwipeHireTab
import com.swipehire.app.ui.screens.ChatDetailScreen
import com.swipehire.app.ui.screens.CreateJobScreen
import com.swipehire.app.ui.screens.DiscoverScreen
import com.swipehire.app.ui.screens.JobLocationScreen
import com.swipehire.app.ui.screens.LoginScreen
import com.swipehire.app.ui.screens.MatchesScreen
import com.swipehire.app.ui.screens.NearbyJobsScreen
import com.swipehire.app.ui.screens.ProfileScreen
import com.swipehire.app.ui.screens.RoleSelectScreen
import com.swipehire.app.ui.screens.SettingsScreen
import com.swipehire.app.viewmodel.SettingsViewModel

private object Routes {
    const val LOGIN = "login"
    const val ROLE_SELECT = "role_select"
    const val DISCOVER = "discover"
    const val MATCHES = "matches"
    const val CHAT = "chat/{matchId}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val NEARBY_JOBS = "nearby_jobs"
    const val JOB_LOCATION = "job_location/{jobId}"
    const val CREATE_JOB = "create_job"
}

private val mainTabRoutes = setOf(Routes.DISCOVER, Routes.MATCHES, Routes.PROFILE, Routes.SETTINGS)

@Composable
fun SwipeHireApp(settingsViewModel: SettingsViewModel = viewModel()) {
    val navController = rememberNavController()
    val settingsState by settingsViewModel.state.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in mainTabRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val currentTab = when (currentRoute) {
                    Routes.DISCOVER -> SwipeHireTab.DISCOVER
                    Routes.MATCHES -> SwipeHireTab.MATCHES
                    Routes.PROFILE -> SwipeHireTab.PROFILE
                    else -> SwipeHireTab.SETTINGS
                }
                SwipeHireBottomBar(
                    currentTab = currentTab,
                    hasUnreadMatches = MockData.matches.any { it.unread },
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (settingsState.onboarded) Routes.DISCOVER else Routes.LOGIN,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onContinueWithGoogle = { navController.navigate(Routes.ROLE_SELECT) },
                    onUseBiometric = { navController.navigate(Routes.ROLE_SELECT) }
                )
            }
            composable(Routes.ROLE_SELECT) {
                RoleSelectScreen(onRoleChosen = { type ->
                    settingsViewModel.setAccountType(type)
                    settingsViewModel.setOnboarded(true)
                    navController.navigate(Routes.DISCOVER) {
                        popUpTo(0)
                    }
                })
            }
            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    accountType = settingsState.accountType,
                    onOpenNearbyJobs = { navController.navigate(Routes.NEARBY_JOBS) },
                    onViewJobLocation = { jobId -> navController.navigate("job_location/$jobId") }
                )
            }
            composable(Routes.NEARBY_JOBS) {
                NearbyJobsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenJob = { jobId -> navController.navigate("job_location/$jobId") }
                )
            }
            composable(Routes.JOB_LOCATION) { backStack ->
                val jobId = backStack.arguments?.getString("jobId") ?: ""
                JobLocationScreen(jobId = jobId, onBack = { navController.popBackStack() })
            }
            composable(Routes.MATCHES) {
                MatchesScreen(onOpenChat = { matchId -> navController.navigate("chat/$matchId") })
            }
            composable(Routes.CHAT) { backStack ->
                val matchId = backStack.arguments?.getString("matchId") ?: ""
                ChatDetailScreen(matchId = matchId, onBack = { navController.popBackStack() })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    accountType = settingsState.accountType,
                    onAccountTypeChange = { type -> settingsViewModel.setAccountType(type) },
                    onOpenCreateJob = { navController.navigate(Routes.CREATE_JOB) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.CREATE_JOB) {
                CreateJobScreen(
                    onBack = { navController.popBackStack() },
                    onPublished = {
                        navController.navigate(Routes.DISCOVER) {
                            popUpTo(Routes.CREATE_JOB) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onLoggedOut = {
                        settingsViewModel.setOnboarded(false)
                        navController.navigate(Routes.LOGIN) { popUpTo(0) }
                    }
                )
            }
        }
    }
}
