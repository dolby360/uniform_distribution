package com.uniformdist.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.uniformdist.app.ui.screens.camera.CameraScreen
import com.uniformdist.app.ui.screens.confirmation.MatchConfirmationScreen
import com.uniformdist.app.ui.screens.home.HomeScreen
import com.uniformdist.app.ui.screens.statistics.StatisticsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onTakePhoto = { navController.navigate(Screen.Camera.route) },
                onViewStats = { navController.navigate(Screen.Statistics.route) }
            )
        }
        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() },
                onPhotoProcessed = { resultJson ->
                    navController.navigate(Screen.MatchConfirmation.createRoute(resultJson))
                }
            )
        }
        composable(
            route = Screen.MatchConfirmation.route,
            arguments = listOf(navArgument("resultJson") { type = NavType.StringType })
        ) {
            MatchConfirmationScreen(
                onDone = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
