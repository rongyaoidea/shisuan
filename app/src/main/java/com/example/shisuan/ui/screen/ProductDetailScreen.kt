package com.example.shisuan.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shisuan.data.database.BatchSnapshot
import com.example.shisuan.data.database.Product
import com.example.shisuan.ui.animation.entranceAnimation
import com.example.shisuan.ui.animation.pressScale
import com.example.shisuan.ui.components.CostTrendChart
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.components.IngredientCostDonut
import com.example.shisuan.ui.icons.Add
import com.example.shisuan.ui.icons.ArrowBack
import com.example.shisuan.ui.icons.Delete
import com.example.shisuan.ui.icons.Edit
import com.example.shisuan.ui.icons.Jar
import com.example.shisuan.ui.theme.*
import com.example.shisuan.ui.viewModel.BatchWithCostUI
import com.example.shisuan.ui.viewModel.ProductDetailViewModel
import com.example.shisuan.ui.viewModel.YieldAnalysis
import com.example.shisuan.utils.WeightFormatter
import com.example.shisuan.utils.countSnapshotIngredients
import com.example.shisuan.utils.formatSnapshotLabel
import java.util.Locale

/**
 * 产品详情页 - 成本趋势 + 批次列表（含加工费、建议售价、配料占比）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNewBatch: (Long) -> Unit,
    onNavigateToEditBatch: (Long, Long) -> Unit,
    onNavigateToCopyBatch: (Long, Long) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val product by viewModel.product.collectAsStateWithLifecycle()
    val batchesWithCost by viewModel.batchesWithCost.collectAsStateWithLifecycle()
    val yieldAnalysis by viewModel.yieldAnalysis.collectAsStateWithLifecycle()
    val yieldTrend by viewModel.yieldTrend.collectAsStateWithLifecycle()
    val errorMessage by viewModel.error.collectAsStateWithLifecycle()
    val historyBatchId by viewModel.historyBatchId.collectAsStateWithLifecycle()
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()
    val currentDigest by viewModel.historyCurrentDigest.collectAsStateWithLifecycle()
    val outcomeBatchId by viewModel.outcomeBatchId.collectAsStateWithLifecycle()
    val batchResult by viewModel.batchResult.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<BatchWithCostUI?>(null) }
    var pendingRestore by remember { mutableStateOf<BatchSnapshot?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    LaunchedEffect(productId) {
        viewModel.setProduct(productId)
    }

    // 派生值缓存：reversed().map 每次重组都新建列表，remember 后仅当源数据变化才重算
    val costTrendData = remember(batchesWithCost) {
        batchesWithCost.reversed()
            .map { it.batch.batchName to it.result.unitCostPerTon }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

            // 成本趋势折线图（批次 >= 2 时展示）
            if (batchesWithCost.size >= 2) {
                item(key = "trend") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "成本趋势（元/吨，由旧到新）",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ink
                            )
                            Spacer(Modifier.height(8.dp))
                            CostTrendChart(
                                // 折线按时间从旧到新绘制，符合时间序列阅读习惯
                                data = costTrendData
                            )
                        }
                    }
                }
            }

            // 损耗分析卡（有批次记录出品率时展示）
            yieldAnalysis?.let { analysis ->
                item(key = "yield") {
                    YieldAnalysisCard(analysis = analysis, trend = yieldTrend)
                }
            }

            // 配方模板快捷入口：以最近批次为模板新建（列表按 createdAt DESC，首项即最新）
            batchesWithCost.firstOrNull()?.let { latest ->
                item(key = "template") {
                    TemplateShortcutCard(
                        latestBatchName = latest.batch.batchName,
                        onClick = { onNavigateToCopyBatch(productId, latest.batch.id) }
                    )
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
                        onDelete = { pendingDelete = item },
                        onCopy = { onNavigateToCopyBatch(item.batch.productId, item.batch.id) },
                        onShowHistory = { viewModel.showHistory(item.batch.id) },
                        onShowOutcome = { viewModel.showOutcome(item.batch.id) }
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

    // 版本历史时间线（git 式）
    if (historyBatchId != null) {
        SnapshotHistorySheet(
            snapshots = snapshots,
            currentDigest = currentDigest,
            onDismiss = { viewModel.dismissHistory() },
            onRestoreClick = { pendingRestore = it }
        )
    }

    // 批次成果记录面板
    outcomeBatchId?.let { batchId ->
        BatchOutcomeSheet(
            batchId = batchId,
            existing = batchResult,
            onDismiss = { viewModel.dismissOutcome() },
            onSave = { viewModel.saveOutcome(it) }
        )
    }

    // 恢复前确认
    pendingRestore?.let { snap ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("恢复到此版本") },
            text = {
                Text(
                    "批次将回到版本 #${snap.digest}（第 ${snap.version} 版）的投料量、配料与加工费。\n" +
                        "恢复后可再次从时间线回到最新版本。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreSnapshot(snap)
                        pendingRestore = null
                    }
                ) { Text("恢复", color = Rausch) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("取消") }
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
            SpecCell("每箱克数", WeightFormatter.format(product.weightPerBoxGram))
            SpecCell("每箱包数", "${product.packagesPerBox}包")
            SpecCell("每包克数", WeightFormatter.format(product.weightPerPackageGram))
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
 * 损耗分析卡 - 产品维度的出品率摘要
 *
 * 平均/最近出品率 + 损耗对吨价的抬升幅度 + 恢复到最佳的潜在节省，
 * 出品率记录 >= 2 时附带趋势折线（复用成本趋势图，值格式改为百分比）。
 */
@Composable
fun YieldAnalysisCard(analysis: YieldAnalysis, trend: List<Pair<String, Double>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "损耗分析 · 出品率",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpecCell("平均出品率", "%.1f%%".format(Locale.CHINA, analysis.avgYieldPercent))
                SpecCell("最近批次", "%.1f%%".format(Locale.CHINA, analysis.latestYieldPercent))
                SpecCell("已记录批次", "${analysis.recordedCount}")
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "熬煮蒸发使实际产量低于投料，吨价已按成品重量折算，较不折算上升约 %.1f%%"
                    .format(Locale.CHINA, analysis.lossImpactPercent),
                fontSize = 12.sp,
                color = Foggy
            )
            if (trend.size >= 2) {
                Spacer(Modifier.height(8.dp))
                CostTrendChart(
                    data = trend,
                    valueLabelFormat = { "%.1f%%".format(Locale.CHINA, it) }
                )
            }
            analysis.potentialSavingPerTon?.let { saving ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "若恢复到最佳出品率 %.1f%%（%s），吨价可降约 ¥%,.0f"
                        .format(Locale.CHINA, analysis.bestYieldPercent ?: 0.0, analysis.bestBatchName ?: "", saving),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SuccessGreen
                )
            }
        }
    }
}

/**
 * 配方模板快捷入口 - 以最近批次为模板新建
 *
 * 复制入口原先只埋在批次卡操作行里，不易被发现；试产迭代场景
 * （80% 配料不变只调一两处）需要一步直达的显式入口。
 */
@Composable
fun TemplateShortcutCard(latestBatchName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pressScale(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = SoftBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Add, null, tint = Rausch, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "以上一批次为模板新建",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
                Text(
                    "「$latestBatchName」的配料、加工费与出品率将自动带入，只改差异项",
                    fontSize = 11.sp,
                    color = Foggy
                )
            }
        }
    }
}

/**
 * 批次卡片 - 显示成本计算结果 + 展开配料占比 + 编辑/复制/删除操作
 */
@Composable
fun BatchCard(
    item: BatchWithCostUI,
    index: Int,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCopy: () -> Unit = {},
    onShowHistory: () -> Unit = {},
    onShowOutcome: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

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
            // 标题行可点击展开/收起；操作行 TextButton 独立，避免整卡 clickable 误触
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        if (diff.diffPercent > 0) "↑ ${"%.1f".format(Locale.CHINA, diff.diffPercent)}%"
                        else "↓ ${"%.1f".format(Locale.CHINA, -diff.diffPercent)}%",
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
                CostCell("吨价", "¥${"%,.0f".format(Locale.CHINA, item.result.unitCostPerTon)}")
                CostCell("箱价", "¥${"%.2f".format(Locale.CHINA, item.result.costPerBox)}")
                CostCell("包价", "¥${"%.2f".format(Locale.CHINA, item.result.costPerPackage)}")
            }
            // 总成本构成与建议售价
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "原料 ¥%,.2f + 加工 ¥%,.2f = ¥%,.2f"
                        .format(Locale.CHINA, item.materialCost, item.processingCost, item.totalCost),
                    fontSize = 12.sp,
                    color = Foggy
                )
                item.suggestedTonPrice?.let { price ->
                    Text(
                        "建议出厂价 ¥%,.0f/吨".format(Locale.CHINA, price),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Rausch
                    )
                }
            }
            // 展开：出品率与配料成本占比
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(8.dp))
                item.batch.yieldRatePercent?.let { yield ->
                    Text(
                        "出品率 %.1f%%（成本已按成品重量折算）".format(Locale.CHINA, yield),
                        fontSize = 12.sp,
                        color = Foggy
                    )
                    Spacer(Modifier.height(8.dp))
                }
                IngredientCostDonut(
                    items = item.ingredients.map { it.ingredientName to it.totalCost }
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    TextButton(onClick = onShowOutcome) {
                        Text("成果记录", fontSize = 12.sp, color = Rausch)
                    }
                    TextButton(onClick = onShowHistory) {
                        Text("版本历史", fontSize = 12.sp, color = Rausch)
                    }
                }
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
                TextButton(onClick = onCopy) {
                    Text("复制为新批次", fontSize = 12.sp, color = Foggy)
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

/**
 * 版本历史时间线（git log 式）
 *
 * 每个节点 = 一次内容变更，#编号为内容指纹（唯一变更编号）。
 * 「当前」标在内容与批次现状一致的版本上（按 digest 匹配）——
 * 恢复到旧版本后，旧版本成为「当前」，更新的版本仍可再恢复回去。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapshotHistorySheet(
    snapshots: List<BatchSnapshot>,
    currentDigest: String?,
    onDismiss: () -> Unit,
    onRestoreClick: (BatchSnapshot) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // 派生值移出组合：配料计数 + 时间格式化在 remember 中预计算，
        // 避免每次重组重复 lines().count 与 java.time 转换
        // Triple(snap, ingredientCount, label)：不用局部 data class，保证编译兼容
        val rows = remember(snapshots) {
            snapshots.map { snap ->
                val count = countSnapshotIngredients(snap.snapshotData)
                Triple(snap, count, formatSnapshotLabel(snap.version, snap.createdAt, count))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text("版本历史", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                "每次保存生成一个版本，#编号为内容指纹；内容相同的保存不会重复记录",
                fontSize = 12.sp,
                color = Foggy
            )
            Spacer(Modifier.height(12.dp))
            if (snapshots.isEmpty()) {
                Text("暂无历史版本", color = Foggy, fontSize = 13.sp)
                Spacer(Modifier.height(24.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(rows) { index, row ->
                        val snap = row.first
                        val label = row.third
                        // 「当前」= 内容指纹与批次现状一致；digest 尚未就绪时退回「最新一条」
                        val isCurrent = if (currentDigest == null) index == 0
                        else snap.digest == currentDigest
                        Row(modifier = Modifier.fillMaxWidth()) {
                             // 时间轴：圆点 + 连接竖线
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (isCurrent) Rausch else Foggy, CircleShape)
                                )
                                if (index < rows.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(52.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "#${snap.digest}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (isCurrent) Rausch else Ink
                                    )
                                    if (isCurrent) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "当前",
                                            fontSize = 11.sp,
                                            color = Rausch,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        TextButton(
                                            onClick = { onRestoreClick(snap) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("恢复到此版本", fontSize = 12.sp, color = Rausch)
                                        }
                                    }
                                }
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    color = Foggy
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
