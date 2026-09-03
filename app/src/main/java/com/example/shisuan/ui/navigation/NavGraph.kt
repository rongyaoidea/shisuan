package com.example.shisuan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shisuan.ui.screen.*

sealed class Screen(val route: String) {
    object BatchList : Screen("batch_list")
    object BatchDetail : Screen("batch_detail/{batchId}") {
        fun createRoute(batchId: Long) = "batch_detail/$batchId"
    }
    object Stats : Screen("stats")
    object UnitConfig : Screen("unit_config")
    object IngredientList : Screen("ingredient_list")
    object BatchResult : Screen("batch_result/{batchId}") {
        fun createRoute(batchId: Long) = "batch_result/$batchId"
    }
    object BatchProblem : Screen("batch_problem/{batchId}") {
        fun createRoute(batchId: Long) = "batch_problem/$batchId"
    }
}

@Composable
fun NavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.BatchList.route, modifier = modifier) {
        composable(Screen.BatchList.route) { BatchListScreen(onNavigateToDetail = { id -> navController.navigate(Screen.BatchDetail.createRoute(id)) }) }
        composable(Screen.BatchDetail.route, arguments = listOf(navArgument("batchId") { type = NavType.LongType })) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getLong("batchId") ?: return@composable
            BatchDetailScreen(batchId = batchId, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Stats.route) { StatsScreen() }
        composable(Screen.UnitConfig.route) { UnitConfigScreen() }
        composable(Screen.BatchResult.route, arguments = listOf(navArgument("batchId") { type = NavType.LongType })) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getLong("batchId") ?: return@composable
        }
        composable(Screen.BatchProblem.route, arguments = listOf(navArgument("batchId") { type = NavType.LongType })) { backStackEntry ->
            val batchId = backStackEntry.arguments?.getLong("batchId") ?: return@composable
        }
    }
}
