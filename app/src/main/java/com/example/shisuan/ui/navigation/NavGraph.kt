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
 * 流程: 产品列表 → 产品详情 → 新建/编辑批次
 *
 * 保留 sealed Screen 字符串路由，不迁到类型安全导航（kotlinx.serialization 目的地）：
 * 路由表小且稳定，大改收益低；如需深链/参数校验再整体迁移。
 */
sealed class Screen(val route: String) {
    object ProductList : Screen("products")
    object ProductDetail : Screen("products/{productId}") {
        fun createRoute(productId: Long) = "products/$productId"
    }
    object NewBatch : Screen("products/{productId}/new_batch") {
        fun createRoute(productId: Long) = "products/$productId/new_batch"
    }
    object EditBatch : Screen("products/{productId}/edit_batch/{batchId}") {
        fun createRoute(productId: Long, batchId: Long) = "products/$productId/edit_batch/$batchId"
    }
    object CopyBatch : Screen("products/{productId}/copy_batch/{batchId}") {
        fun createRoute(productId: Long, batchId: Long) = "products/$productId/copy_batch/$batchId"
    }
    object IngredientLibrary : Screen("ingredient_library")
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
                    navController.navigate(Screen.ProductDetail.createRoute(productId)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToIngredientLibrary = {
                    navController.navigate(Screen.IngredientLibrary.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // 配料库（全局原料管理）
        composable(Screen.IngredientLibrary.route) {
            IngredientLibraryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 产品详情页（批次列表）
        composable(
            Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.LongType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId")?.takeIf { v -> v != 0L } ?: return@composable
            ProductDetailScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNewBatch = { id ->
                    navController.navigate(Screen.NewBatch.createRoute(id)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToEditBatch = { pid, bid ->
                    navController.navigate(Screen.EditBatch.createRoute(pid, bid)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToCopyBatch = { pid, bid ->
                    navController.navigate(Screen.CopyBatch.createRoute(pid, bid)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // 新建批次页
        composable(
            Screen.NewBatch.route,
            arguments = listOf(navArgument("productId") { type = NavType.LongType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId")?.takeIf { v -> v != 0L } ?: return@composable
            NewBatchScreen(
                productId = productId,
                editBatchId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 编辑批次页
        composable(
            Screen.EditBatch.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.LongType },
                navArgument("batchId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId")?.takeIf { v -> v != 0L } ?: return@composable
            val batchId = backStackEntry.arguments?.getLong("batchId")?.takeIf { v -> v != 0L } ?: return@composable
            NewBatchScreen(
                productId = productId,
                editBatchId = batchId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 复制批次页：以现有批次为模板预填表单，保存时生成全新批次
        composable(
            Screen.CopyBatch.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.LongType },
                navArgument("batchId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId")?.takeIf { v -> v != 0L } ?: return@composable
            val batchId = backStackEntry.arguments?.getLong("batchId")?.takeIf { v -> v != 0L } ?: return@composable
            NewBatchScreen(
                productId = productId,
                copyFromBatchId = batchId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
