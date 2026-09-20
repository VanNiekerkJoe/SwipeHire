package com.swipehire.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.swipehire.app.data.currentFirebaseUserId
import com.swipehire.app.ui.components.SwipeHireBottomBar
import com.swipehire.app.ui.components.SwipeHireTab
import com.swipehire.app.ui.screens.ChatDetailScreen
import com.swipehire.app.ui.screens.AlertsScreen
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
import com.swipehire.app.viewmodel.MatchesViewModel
import com.swipehire.app.viewmodel.AlertsViewModel

private object Routes {
    const val LOGIN = "login"
    const val ROLE_SELECT = "role_select"
    const val DISCOVER = "discover"
    const val MATCHES = "matches"
    const val ALERTS = "alerts"
    const val CHAT = "chat/{matchId}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val NEARBY_JOBS = "nearby_jobs"
    const val JOB_LOCATION = "job_location/{jobId}"
    const val CREATE_JOB = "create_job"
}

private val mainTabRoutes = setOf(Routes.DISCOVER, Routes.MATCHES, Routes.ALERTS, Routes.PROFILE)

@Composable
fun SwipeHireApp(settingsViewModel: SettingsViewModel = viewModel()) {
    val navController = rememberNavController()
    val settingsState by settingsViewModel.state.collectAsState()
    val matchesViewModel: MatchesViewModel = viewModel()
    val matches by matchesViewModel.matches.collectAsState()
    val alertsViewModel: AlertsViewModel = viewModel()
    val alerts by alertsViewModel.alerts.collectAsState()

    LaunchedEffect(settingsState.onboarded) {
        matchesViewModel.refresh()
        alertsViewModel.refresh()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in mainTabRoutes

    // Main tabs are peers, not nested screens. Always remove the previous tab
    // before opening another one. Saving/restoring the old stack could restore
    // Settings on top of Profile when Settings was opened from Profile.
    val navigateToMainTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.DISCOVER)
            launchSingleTop = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val currentTab = when (currentRoute) {
                    Routes.DISCOVER -> SwipeHireTab.DISCOVER
                    Routes.MATCHES -> SwipeHireTab.MATCHES
                    Routes.ALERTS -> SwipeHireTab.ALERTS
                    Routes.PROFILE -> SwipeHireTab.PROFILE
                    else -> SwipeHireTab.DISCOVER
                }
                SwipeHireBottomBar(
                    currentTab = currentTab,
                    hasUnreadMatches = matches.any { it.unread },
                    hasUnreadAlerts = alerts.any { alert ->
                        !alert.isRead && settingsState.pushNotifications &&
                            ((alert.type == com.swipehire.app.data.AlertType.MATCH && settingsState.matchAlerts) ||
                                (alert.type == com.swipehire.app.data.AlertType.MESSAGE && settingsState.messageAlerts))
                    },
                    onTabSelected = { tab ->
                        navigateToMainTab(tab.route)
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (settingsState.onboarded && currentFirebaseUserId() != null) Routes.DISCOVER else Routes.LOGIN,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onAuthenticated = {
                        settingsViewModel.resolveSignedInAccount { role ->
                            navController.navigate(if (role == null) Routes.ROLE_SELECT else Routes.DISCOVER) { popUpTo(0) }
                        }
                    },
                    onUseBiometric = {
                        settingsViewModel.resolveSignedInAccount { role ->
                            navController.navigate(if (role == null) Routes.ROLE_SELECT else Routes.DISCOVER) { popUpTo(0) }
                        }
                    }
                )
            }
            composable(Routes.ROLE_SELECT) {
                RoleSelectScreen(onRoleChosen = { type ->
                    settingsViewModel.chooseAccountType(type) { saved ->
                        if (saved) navController.navigate(Routes.DISCOVER) {
                            popUpTo(0)
                        }
                    }
                })
            }
            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    accountType = settingsState.accountType,
                    onOpenNearbyJobs = { navController.navigate(Routes.NEARBY_JOBS) },
                    onViewJobLocation = { jobId -> navController.navigate("job_location/$jobId") },
                    onOpenChat = { matchId -> navController.navigate("chat/$matchId") }
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
                MatchesScreen(onOpenChat = { matchId -> navController.navigate("chat/$matchId") }, viewModel = matchesViewModel)
            }
            composable(Routes.ALERTS) {
                AlertsScreen(
                    notificationsEnabled = settingsState.pushNotifications,
                    matchAlertsEnabled = settingsState.matchAlerts,
                    messageAlertsEnabled = settingsState.messageAlerts,
                    onOpenChat = { matchId -> navController.navigate("chat/$matchId") },
                    onOpenMatches = { navigateToMainTab(Routes.MATCHES) },
                    viewModel = alertsViewModel
                )
            }
            composable(Routes.CHAT) { backStack ->
                val matchId = backStack.arguments?.getString("matchId") ?: ""
                ChatDetailScreen(matchId = matchId, onBack = { navController.popBackStack() })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    accountType = settingsState.accountType,
                    onOpenCreateJob = { navController.navigate(Routes.CREATE_JOB) },
                    onOpenSettings = { navigateToMainTab(Routes.SETTINGS) },
                    onLoggedOut = {
                        settingsViewModel.logOut()
                        navController.navigate(Routes.LOGIN) { popUpTo(0) }
                    }
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
                    onBack = { navigateToMainTab(Routes.PROFILE) },
                    onLoggedOut = {
                        settingsViewModel.logOut()
                        navController.navigate(Routes.LOGIN) { popUpTo(0) }
                    }
                )
            }
        }
    }
}
