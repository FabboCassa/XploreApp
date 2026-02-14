# Xplore Architecture Documentation

This document outlines the high-level architecture of the Xplore KMP application. We follow **Clean Architecture** principles combined with the **MVVM (Model-View-ViewModel)** pattern.

## 🏗️ Architectural Pattern

The application is structured into three main layers:

1.  **Domain Layer (Core)**
    *   **Responsibility**: Defines the core business logic and data models. It is pure Kotlin and has NO dependencies on Android, iOS, or UI frameworks.
    *   **Components**: Entities (`Museum`, `MapPin`), Repository Interfaces (`MuseumRepository`).

2.  **Data Layer**
    *   **Responsibility**: Implements the repository interfaces defined in the Domain layer. Handles data retrieval from networking (Ktor) or local storage (Settings/SQLDelight).
    *   **Components**: Repository Implementations (`MuseumRepositoryImpl`), Data Sources, DTOs.

3.  **Presentation Layer (UI)**
    *   **Responsibility**: Handles UI rendering and user interaction.
    *   **Components**:
        *   **ViewModel** (`HomeViewModel`): Manages UI state, handles user intent, and communicates with the Domain/Data layer.
        *   **UI Components** (`HomeScreen`, `XploreMap`, etc.): Compose Multiplatform composables that render the state provided by the ViewModel.

4.  **Dependency Injection (DI)**
    *   **Tool**: **Koin**.
    *   **Responsibility**: Provides dependencies to classes, ensuring loose coupling and easier testing.

---

## 📂 Key Components & Responsibilities

### Domain (Common)
*   **`Museum`**: Data class representing a museum entity.
*   **`MapPin`**: UI-agnostic representation of a pin on the map.
*   **`MuseumRepository`**: Interface defining *what* data operations are possible (e.g., `getMuseums()`).

### Data (Common)
*   **`MuseumRepositoryImpl`**: Concrete implementation of the repository. currently uses **Mock Data** but is structured to fetch data via Ktor.

### Presentation (Common)
*   **`HomeViewModel`**:
    *   Exposes `HomeUiState` via `StateFlow` (Single Source of Truth).
    *   Handles events: `onSearchQueryChange`, `onPinClick`, `onTabSelected`.
    *   Filters data based on search queries and active filters.
*   **`HomeScreen`**: The main screen composable. Interacts with the ViewModel.
*   **`XploreMap`**: Abstracted map component. Currently a placeholder, designed to be swapped with Google Maps/Mapbox SDK.
*   **`XploreTheme`**: Custom Material3 theme design system.

---

## 🔄 Happy Flows

### 1. App Launch & Data Loading
1.  **App Start**: `App.kt` initializes the Koin DI graph and sets the `XploreTheme`.
2.  **Navigation**: The app defaults to showing `HomeScreen`.
3.  **ViewModel Init**: `HomeViewModel` is injected into `HomeScreen`. On generic initialization, it calls `repository.getMuseums()`.
4.  **State Update**: The repository returns data (mock or network). The ViewModel updates `_uiState` with the list of museums and pins.
5.  **Render**: `HomeScreen` observes the state and passes the `pins` list to `XploreMap`, which draws them.

### 2. Search & Filter
1.  **User Action**: User types "Uffizi" in `XploreSearchBar`.
2.  **Event**: `onSearchQueryChange("Uffizi")` is called on `HomeViewModel`.
3.  **Logic**: The ViewModel updates the query state and re-runs the filtering logic on the cached museums list.
4.  **Update**: The `filteredMuseums` and `mapPins` in `HomeUiState` are updated.
5.  **Render**: `XploreMap` recomposes to show only the matching pins.

---

## 🛠️ Tech Stack
*   **Language**: Kotlin (Multiplatform)
*   **UI Framework**: Compose Multiplatform (Jetpack Compose)
*   **Dependency Injection**: Koin
*   **Networking**: Ktor (Client)
*   **Serialization**: Kotlinx Serialization
*   **Async**: Kotlin Coroutines & Flow
*   **Resources**: Compose Multiplatform Resources (strings, drawables)
