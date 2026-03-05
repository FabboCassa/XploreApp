package org.xplore.project.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the Xplore app.
 *
 * Uses Kotlin Serialization for type-safe navigation (Navigation Compose 2.8+).
 * Routes are grouped by feature area:
 * - **Auth**: Login, Register flows.
 * - **Main**: Bottom-nav destinations (Map, Chat, Profile).
 */

// ── Auth Routes ──────────────────────────────────────────────

@Serializable
data object WelcomeRoute

@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

// ── Main App Routes ──────────────────────────────────────────

@Serializable
data object MainRoute          // Container with Bottom Nav

@Serializable
data object MapRoute           // Tab 0: Map / Home

@Serializable
data object CommunityRoute     // Tab 1: Community

@Serializable
data object ProfileRoute       // Tab 2: User Profile

// ── Detail Routes ────────────────────────────────────────────

@Serializable
data class PoiDetailRoute(val poiId: String)  // POI Detail Screen
