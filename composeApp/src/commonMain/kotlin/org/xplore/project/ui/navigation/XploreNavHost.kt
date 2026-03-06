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
import org.xplore.project.ui.auth.WelcomeScreen
import org.xplore.project.ui.home.HomeScreen
import org.xplore.project.ui.poi.PoiDetailScreen
import org.xplore.project.ui.community.detail.GroupDetailScreen

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
            PoiDetailScreen(
                poiId = route.poiId,
                onBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(LoginRoute) }
            )
        }

        // ── Group Detail ──────────────────────────────────────
        composable<GroupDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<GroupDetailRoute>()
            GroupDetailScreen(
                groupId = route.groupId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
