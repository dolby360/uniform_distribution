package com.uniformdist.app.ui.navigation

sealed class Screen(val route: String) {
    data object Camera : Screen("camera")
    data object MatchConfirmation : Screen("match_confirmation/{resultJson}") {
        fun createRoute(resultJson: String): String = "match_confirmation/$resultJson"
    }
    data object ManualCrop : Screen("manual_crop/{imageUri}") {
        fun createRoute(imageUri: String): String = "manual_crop/$imageUri"
    }
    data object ItemDetail : Screen("item_detail/{itemId}") {
        fun createRoute(itemId: String): String = "item_detail/$itemId"
    }
    data object ItemsList : Screen("items_list")
}
