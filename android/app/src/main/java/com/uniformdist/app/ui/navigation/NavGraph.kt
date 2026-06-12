package com.uniformdist.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.uniformdist.app.ui.screens.camera.CameraScreen
import com.uniformdist.app.ui.screens.confirmation.MatchConfirmationScreen
import com.uniformdist.app.ui.screens.crop.ManualCropScreen
import com.uniformdist.app.ui.screens.itemdetail.ItemDetailScreen
import com.uniformdist.app.ui.screens.itemslist.ItemsListScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.ItemsList.route) {
        composable(Screen.ItemsList.route) {
            ItemsListScreen(
                onAddClothes = { navController.navigate(Screen.Camera.route) },
                onItemDetail = { itemId ->
                    navController.navigate(Screen.ItemDetail.createRoute(itemId))
                }
            )
        }
        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() },
                onPhotoProcessed = { resultJson ->
                    navController.navigate(Screen.MatchConfirmation.createRoute(resultJson))
                },
                onManualCrop = { imageUri ->
                    navController.navigate(Screen.ManualCrop.createRoute(imageUri))
                }
            )
        }
        composable(
            route = Screen.ManualCrop.route,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) {
            ManualCropScreen(
                imageUri = it.arguments?.getString("imageUri") ?: "",
                onCropComplete = { resultJson ->
                    navController.navigate(Screen.MatchConfirmation.createRoute(resultJson)) {
                        popUpTo(Screen.Camera.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.MatchConfirmation.route,
            arguments = listOf(navArgument("resultJson") { type = NavType.StringType })
        ) {
            MatchConfirmationScreen(
                onDone = {
                    navController.popBackStack(Screen.ItemsList.route, inclusive = false)
                }
            )
        }
        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) {
            ItemDetailScreen(
                onBack = { navController.popBackStack() },
                onItemDeleted = { navController.popBackStack() }
            )
        }
    }
}
