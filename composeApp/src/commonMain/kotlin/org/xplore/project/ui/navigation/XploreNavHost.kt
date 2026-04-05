package org.xplore.project.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.koin.compose.koinInject
import org.xplore.project.data.local.TokenManager
import org.xplore.project.ui.auth.LoginScreen
import org.xplore.project.ui.auth.RegisterScreen
import org.xplore.project.ui.home.HomeScreen
import org.xplore.project.ui.poi.PoiDetailScreen
import androidx.compose.runtime.LaunchedEffect
import org.xplore.project.ui.community.detail.GroupDetailScreen
import org.xplore.project.ui.auth.WelcomeScreen
import org.xplore.project.ui.community.detail.CreateCompetitionScreen
import org.xplore.project.ui.community.detail.PoiSelectionMapScreen
import org.xplore.project.ui.community.detail.CompetitionMapScreen
import org.xplore.project.ui.community.detail.CreateCompetitionViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.xplore.project.ui.home.HomeViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import xploreapp.composeapp.generated.resources.Res
import xploreapp.composeapp.generated.resources.session_expired_title
import xploreapp.composeapp.generated.resources.session_expired_message
import xploreapp.composeapp.generated.resources.close
import xploreapp.composeapp.generated.resources.server_offline_title
import xploreapp.composeapp.generated.resources.server_offline_msg
import xploreapp.composeapp.generated.resources.server_offline_ok
import io.ktor.client.request.get

/**
 * Root navigation host for the Xplore application.
 *
 * ## Navigation Graph
 * ```
 * WelcomeRoute ──┬──> LoginRoute ──┬──> MainRoute (HomeScreen w/ BottomNav)
 *                │                 └──> RegisterRoute ──> MainRoute
 *                └──> MainRoute (Guest mode)
 *
 * MainRoute ──> PoiDetailRoute (POI Detail Screen)
 * ```
 *
 * ## Start Destination
 * Always starts at [WelcomeRoute]. The WelcomeScreen checks
 * for existing tokens and can auto-skip to MainRoute.
 */
@Composable
fun XploreNavHost(
    navController: NavHostController = rememberNavController(),
    tokenManager: TokenManager = koinInject(),
) {
    val startDest = if (tokenManager.isLoggedIn || tokenManager.isGuest) MainRoute else WelcomeRoute

    // ── Session-expired dialog state ──────────────────────────
    val showSessionExpired = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tokenManager.sessionExpiredEvent.collect {
            showSessionExpired.value = true
        }
    }

    if (showSessionExpired.value) {
        AlertDialog(
            onDismissRequest = { /* non-dismissable – user must tap the button */ },
            title = { Text(stringResource(Res.string.session_expired_title)) },
            text  = { Text(stringResource(Res.string.session_expired_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showSessionExpired.value = false
                    navController.navigate(WelcomeRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text(stringResource(Res.string.close))
                }
            },
        )
    }

    // ── Server-offline alert (shown once at app launch) ──────
    val showServerOffline = remember { mutableStateOf(false) }
    val httpClient: io.ktor.client.HttpClient = koinInject()

    LaunchedEffect(Unit) {
        try {
            httpClient.get("https://10.0.2.2:7109/api/auth/health")
        } catch (_: Exception) {
            showServerOffline.value = true
        }
    }

    if (showServerOffline.value) {
        AlertDialog(
            onDismissRequest = { showServerOffline.value = false },
            title = { Text(stringResource(Res.string.server_offline_title)) },
            text  = { Text(stringResource(Res.string.server_offline_msg)) },
            confirmButton = {
                TextButton(onClick = { showServerOffline.value = false }) {
                    Text(stringResource(Res.string.server_offline_ok))
                }
            },
        )
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
    ) {
        // ── Welcome Gate ─────────────────────────────────────
        composable<WelcomeRoute> {
            WelcomeScreen(
                onNavigateToLogin = {
                    navController.navigate(LoginRoute)
                },
                onGuestAccess = {
                    navController.navigate(MainRoute) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                    }
                },
            )
        }

        // ── Login ────────────────────────────────────────────
        composable<LoginRoute> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(MainRoute) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(RegisterRoute)
                },
            )
        }

        // ── Register ─────────────────────────────────────────
        composable<RegisterRoute> {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(MainRoute) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        // ── Main App ────────────────────────────────────────
        composable<MainRoute> {
            HomeScreen(
                onLogout = {
                    navController.navigate(WelcomeRoute) {
                        popUpTo(MainRoute) { inclusive = true }
                    }
                },
                onNavigateToPoiDetail = { poiId ->
                    navController.navigate(PoiDetailRoute(poiId))
                },
                onNavigateToGroupDetail = { groupId ->
                    navController.navigate(GroupDetailRoute(groupId))
                },
            )
        }

        // ── POI Detail ──────────────────────────────────────
        composable<PoiDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PoiDetailRoute>()
            val homeVm: HomeViewModel = koinViewModel()
            PoiDetailScreen(
                poiId = route.poiId,
                onBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(LoginRoute) },
                onAddStop = { pin -> homeVm.addItineraryStop(pin) },
            )
        }

        // ── Group Detail ──────────────────────────────────────
        composable<GroupDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<GroupDetailRoute>()
            GroupDetailScreen(
                groupId = route.groupId,
                onBack = { navController.popBackStack() },
                onNavigateToCreateCompetition = { gId -> navController.navigate(CreateCompetitionRoute(gId)) },
                onNavigateToCompetitionMap = { compId, allowedPois -> 
                    // Pass parameters via saved state since Screen.kt might not support list structures directly
                    navController.currentBackStackEntry?.savedStateHandle?.set("allowed_pois", allowedPois)
                    navController.navigate(CompetitionMapRoute(compId))
                }
            )
        }
        
        // ── Create Competition ────────────────────────────────
        composable<CreateCompetitionRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CreateCompetitionRoute>()
            
            // Shared ViewModel scoped to this NavBackStackEntry 
            // so returning from Map Screen preserves form state
            val viewModel: CreateCompetitionViewModel = koinViewModel()
            
            // Listen for result from Map Selection
            val savedStateHandle = backStackEntry.savedStateHandle
            val selectedPoisStr = savedStateHandle.get<String>("selected_pois")
            LaunchedEffect(selectedPoisStr) {
                if (selectedPoisStr != null) {
                    val ids = if (selectedPoisStr.isBlank()) emptyList() else selectedPoisStr.split(",")
                    viewModel.updateSelectedPois(ids)
                    savedStateHandle.remove<String>("selected_pois")
                }
            }
            
            CreateCompetitionScreen(
                groupId = route.groupId,
                onBack = { navController.popBackStack() },
                onNavigateToMapSelector = { gId -> 
                    navController.navigate(PoiSelectionMapRoute(gId)) 
                },
                viewModel = viewModel
            )
        }
        
        // ── POI Selection Map ────────────────────────────────
        composable<PoiSelectionMapRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PoiSelectionMapRoute>()
            PoiSelectionMapScreen(
                groupId = route.groupId,
                onBackWithSelection = { selectedIds ->
                    // Pass selection back to previous screen (CreateCompetition)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selected_pois", selectedIds.joinToString(","))
                    navController.popBackStack()
                }
            )
        }

        // ── Participant Competition Map ──────────────────────
        composable<CompetitionMapRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CompetitionMapRoute>()
            val allowedPois: List<String> = navController.previousBackStackEntry?.savedStateHandle?.get<List<String>>("allowed_pois") ?: emptyList()
            
            CompetitionMapScreen(
                compId = route.compId,
                allowedPoiIds = allowedPois,
                onBack = { navController.popBackStack() },
                onNavigateToPoiDetail = { poiId -> navController.navigate(PoiDetailRoute(poiId)) }
            )
        }
    }
}
