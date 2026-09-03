package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.shisuan.ui.animation.entranceAnimation
import com.example.shisuan.ui.animation.pressScale
import com.example.shisuan.data.database.BatchRecord
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.viewModel.ProductDetailViewModel

/**
 * 产品详情页 - 显示该产品的所有批次
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNewBatch: (Long) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val product by viewModel.product.collectAsState()
    val batchesWithCost by viewModel.batchesWithCost.collectAsState()
    
    LaunchedEffect(productId) {
        viewModel.setProduct(productId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        product?.name ?: "产品详情",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToNewBatch(productId) },
                containerColor = Color(0xFFE8345B)
            ) {
                Icon(Icons.Default.Add, "新建批次", tint = Color.White)
            }
        }
    ) { padding ->
        if (batchesWithCost.isEmpty()) {
            EmptyState("🍳", "还没有批次，点 ＋ 新建第一个批次")
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(batchesWithCost, key = { _, b -> b.batch.id }) { index, item ->
                    BatchCard(
                        item = item,
                        index = index
                    )
                }
            }
        }
    }
}

/**
 * 批次卡片 - 显示成本计算结果
 */
@Composable
fun BatchCard(
    item: com.example.shisuan.ui.viewModel.BatchWithCostUI,
    index: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pressScale()
            .entranceAnimation(index = index),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.batch.batchName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(Modifier.weight(1f))
                // 差异显示
                item.differential?.let { diff ->
                    val color = when {
                        diff.diffPercent < -0.1 -> Color(0xFF0A8754)
                        diff.diffPercent > 0.1 -> Color(0xFFE8830A)
                        else -> Color(0xFF999999)
                    }
                    Text(
                        if (diff.diffPercent > 0) "↑ ${"%.1f".format(diff.diffPercent)}%" 
                        else "↓ ${"%.1f".format(-diff.diffPercent)}%",
                        color = color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            // 成本三列
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CostCell("吨价", "¥${"%,.0f".format(item.result.unitCostPerTon)}")
                CostCell("箱价", "¥${"%.2f".format(item.result.costPerBox)}")
                CostCell("包价", "¥${"%.2f".format(item.result.costPerPackage)}")
            }
        }
    }
}

@Composable
private fun CostCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Color(0xFF6B6B6B))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
    }
}