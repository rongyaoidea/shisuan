package com.example.shisuan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shisuan.ui.screen.*

/**
 * 导航路由定义
 * 新流程: 产品列表 → 产品详情 → 新建批次
 */
sealed class Screen(val route: String) {
    object ProductList : Screen("products")
    object ProductDetail : Screen("products/{productId}") {
        fun createRoute(productId: Long) = "products/$productId"
    }
    object NewBatch : Screen("products/{productId}/new_batch") {
        fun createRoute(productId: Long) = "products/$productId/new_batch"
    }
}

/**
 * 导航图
 */
@Composable
fun NavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.ProductList.route,
        modifier = modifier
    ) {
        // 产品列表页（主页）
        composable(Screen.ProductList.route) {
            ProductListScreen(
                onNavigateToDetail = { productId ->
                    navController.navigate(Screen.ProductDetail.createRoute(productId))
                }
            )
        }
        
        // 产品详情页（批次列表）
        composable(
            Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.LongType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId") ?: return@composable
            ProductDetailScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNewBatch = { id ->
                    navController.navigate(Screen.NewBatch.createRoute(id))
                }
            )
        }
        
        // 新建批次页
        composable(
            Screen.NewBatch.route,
            arguments = listOf(navArgument("productId") { type = NavType.LongType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId") ?: return@composable
            NewBatchScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}