package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.shisuan.data.database.Product
import com.example.shisuan.ui.animation.entranceAnimation
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.icons.Add
import com.example.shisuan.ui.icons.ArrowBack
import com.example.shisuan.ui.icons.Camera
import com.example.shisuan.ui.icons.Delete
import com.example.shisuan.ui.icons.Edit
import com.example.shisuan.ui.icons.Flask
import com.example.shisuan.ui.icons.Gallery
import com.example.shisuan.ui.icons.Jar
import com.example.shisuan.ui.icons.Package
import com.example.shisuan.ui.icons.Scale
import com.example.shisuan.ui.theme.*
import com.example.shisuan.ui.viewModel.BatchWithCostUI
import com.example.shisuan.ui.viewModel.ProductDetailViewModel

/**
 * 产品详情页 - 显示产品包装规格 + 所有批次成本
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNewBatch: (Long) -> Unit,
    onNavigateToEditBatch: (Long, Long) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val product by viewModel.product.collectAsState()
    val batchesWithCost by viewModel.batchesWithCost.collectAsState()
    var pendingDelete by remember { mutableStateOf<BatchWithCostUI?>(null) }

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
                        Icon(ArrowBack, "返回")
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
                containerColor = Rausch
            ) {
                Icon(Add, "新建批次", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 包装规格卡片
            if (product != null) {
                item(key = "spec") {
                    PackagingSpecCard(product = product!!)
                }
            }

            if (batchesWithCost.isEmpty()) {
                item(key = "empty") {
                    EmptyState(Jar, "还没有批次，点 ＋ 新建第一个批次")
                }
            } else {
                itemsIndexed(batchesWithCost, key = { _, b -> b.batch.id }) { index, item ->
                    BatchCard(
                        item = item,
                        index = index,
                        onEdit = { onNavigateToEditBatch(item.batch.productId, item.batch.id) },
                        onDelete = { pendingDelete = item }
                    )
                }
            }
        }
    }

    // 删除确认对话框
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除批次") },
            text = { Text("确定删除批次「${item.batch.batchName}」吗？其原料明细将一并删除，此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBatch(item.batch)
                        pendingDelete = null
                    }
                ) { Text("删除", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

/**
 * 产品包装规格卡
 * 显示：每箱克数 / 每箱包数 / 每包克数
 */
@Composable
fun PackagingSpecCard(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = SoftBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpecCell("每箱克数", "${"%.0f".format(product.weightPerBoxGram)}g")
            SpecCell("每箱包数", "${product.packagesPerBox}包")
            SpecCell("每包克数", "${"%.0f".format(product.weightPerPackageGram)}g")
        }
    }
}

@Composable
private fun SpecCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Foggy)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}

/**
 * 批次卡片 - 显示成本计算结果 + 编辑/删除操作
 */
@Composable
fun BatchCard(
    item: BatchWithCostUI,
    index: Int,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .entranceAnimation(index = index),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.batch.batchName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
                Spacer(Modifier.weight(1f))
                // 差异显示
                item.differential?.let { diff ->
                    val color = when {
                        diff.diffPercent < -0.1 -> SuccessGreen
                        diff.diffPercent > 0.1 -> WarningOrange
                        else -> Foggy
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
            Spacer(Modifier.height(4.dp))
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Edit, null, modifier = Modifier.size(16.dp), tint = Foggy)
                    Spacer(Modifier.width(4.dp))
                    Text("编辑", fontSize = 12.sp, color = Foggy)
                }
                TextButton(onClick = onDelete) {
                    Icon(Delete, null, modifier = Modifier.size(16.dp), tint = DangerRed)
                    Spacer(Modifier.width(4.dp))
                    Text("删除", fontSize = 12.sp, color = DangerRed)
                }
            }
        }
    }
}

@Composable
private fun CostCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Foggy)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}
