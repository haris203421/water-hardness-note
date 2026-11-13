package com.waterhardness.note.core.navigation

sealed interface Screen {
    data object Splash : Screen
    data object Menu : Screen
    data class Game(val draftId: String, val applianceId: String) : Screen
    data class GameOver(val draftId: String, val applianceId: String, val recordId: String? = null) : Screen
    data class HardnessInfo(val canonical: Int? = null, val fromTest: Boolean = false) : Screen
    data class DescalePlan(val applianceId: String) : Screen
    data class History(val applianceId: String? = null) : Screen
    data object Settings : Screen
}
