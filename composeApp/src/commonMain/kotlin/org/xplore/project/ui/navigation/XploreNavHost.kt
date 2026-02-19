package org.xplore.project.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.xplore.project.ui.auth.LoginScreen
import org.xplore.project.ui.auth.RegisterScreen
import org.xplore.project.ui.auth.WelcomeScreen
import org.xplore.project.ui.home.HomeScreen

/**
 * Root navigation host for the Xplore application.
 *
 * ## Navigation Graph
 * ```
 * WelcomeRoute ──┬──> LoginRoute ──┬──> MainRoute (HomeScreen w/ BottomNav)
 *                │                 └──> RegisterRoute ──> MainRoute
 *                └──> MainRoute (Guest mode)
 * ```
 *
 * ## Start Destination
 * Always starts at [WelcomeRoute]. The WelcomeScreen checks
 * for existing tokens and can auto-skip to MainRoute.
 */
@Composable
fun XploreNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = WelcomeRoute,
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
                }
            )
        }
    }
}
