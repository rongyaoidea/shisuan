package com.example.shisuan.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickableimport androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.shisuan.domain.model.IngredientDiffKind
import com.example.shisuan.domain.model.SnapshotDiffer
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
import com.example.shisuan.utils.BatchSnapshotCodec
import com.example.shisuan.utils.WeightFormatter
import com.example.shisuan.utils.groupByBatchMonth
import com.example.shisuan.utils.countSnapshotIngredients
import com.example.shisuan.utils.formatSnapshotLabel
import java.util.Locale

/**
 * 产品详情页 - 成本趋势 + 批次列表（含加工费、建议售价、配料占比）
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    // 批次搜索：匹配批次号 / 备注 / 原料名 / 品牌
    var batchQuery by remember { mutableStateOf("") }
    // 月份折叠：null = 未手动操作（默认只展开最新一月）；点表头后转为显式集合
    var expandedMonths: Set<String>? by remember(productId) { mutableStateOf(null) }

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

    // 批次搜索过滤（批次号/备注/原料名/品牌，忽略大小写）
    val filteredBatches = remember(batchesWithCost, batchQuery) {
        val q = batchQuery.trim()
        if (q.isEmpty()) batchesWithCost
        else batchesWithCost.filter { row ->
            row.batch.batchName.contains(q, ignoreCase = true) ||
                row.batch.note.contains(q, ignoreCase = true) ||
                row.ingredients.any {
                    it.ingredientName.contains(q, ignoreCase = true) ||
                        it.ingredientSupplier.contains(q, ignoreCase = true)
                }
        }
    }
    // 按月分组（输入已是 createdAt DESC，组序由新到旧）
    val monthGroups = remember(filteredBatches) {
        groupByBatchMonth(filteredBatches) { it.batch.batchName }
    }
    // 搜索时全展开（结果默认可见）；平时默认只展开最新一月
    val effectiveExpanded: Set<String> = if (batchQuery.isBlank()) {
        expandedMonths ?: setOfNotNull(monthGroups.firstOrNull()?.key)
    } else {
        monthGroups.map { it.key }.toSet()
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
                item(key = "batch-search") {
                    OutlinedTextField(
                        value = batchQuery,
                        onValueChange = { batchQuery = it },
                        placeholder = { Text("搜索批次号 / 备注 / 原料…") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        trailingIcon = {
                            if (batchQuery.isNotEmpty()) {
                                TextButton(onClick = { batchQuery = "" }) {
                                    Text("清除", fontSize = 12.sp)
                                }
                            }
                        }
                    )
                }
                if (filteredBatches.isEmpty()) {
                    item(key = "no-match") {
                        EmptyState(Jar, "没有匹配的批次，换个关键词试试")
                    }
                } else {
                    monthGroups.forEach { group ->
                        stickyHeader(key = "month-${group.key}") {
                            MonthHeader(
                                label = group.label,
                                count = group.items.size,
                                expanded = group.key in effectiveExpanded,
                                onToggle = {
                                    val cur = effectiveExpanded
                                    expandedMonths =
                                        if (group.key in cur) cur - group.key else cur + group.key
                                }
                            )
                        }
                        if (group.key in effectiveExpanded) {
                            itemsIndexed(group.items, key = { _, b -> b.batch.id }) { index, item ->
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
 * 月份分组吸顶表头：点击折叠/展开该月批次。
 * 自带页面背景色，避免吸顶滚动时透出下方卡片。
 */
@Composable
private fun MonthHeader(
    label: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$label · $count 批",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Ink
        )
        Spacer(Modifier.weight(1f))
        Text(
            if (expanded) "收起" else "展开",
            fontSize = 12.sp,
            color = Rausch
        )
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
            // 原料改价断点：快照价与原料库现价不一致即历史价批，不折叠也能看到断点
            if (item.priceDrifts.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    priceDriftBanner(item.priceDrifts),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = WarningOrange
                )
            }
            // 按现价重算预览：只算不存，帮用户决定是否接受涨价/是否以新价建批次
            item.recalculated?.let { preview ->
                Spacer(Modifier.height(4.dp))
                val pct = preview.diffPercentPerTon ?: 0.0
                Text(
                    recalcPreviewLine(preview),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        pct < -0.1 -> SuccessGreen
                        pct > 0.1 -> WarningOrange
                        else -> Foggy
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            // 折叠态只留核心：吨价 + 建议出厂价；箱价/包价与成本构成收进展开区
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("吨价", fontSize = 11.sp, color = Foggy)
                Spacer(Modifier.width(6.dp))
                Text(
                    "¥${"%,.0f".format(Locale.CHINA, item.result.unitCostPerTon)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Spacer(Modifier.weight(1f))
                item.suggestedTonPrice?.let { price ->
                    Text(
                        "建议出厂价 ¥%,.0f/吨".format(Locale.CHINA, price),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Rausch
                    )
                }
            }
            // 展开：箱价/包价 + 成本构成 + 出品率与配料成本占比
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CostCell("吨价", "¥${"%,.0f".format(Locale.CHINA, item.result.unitCostPerTon)}")
                    CostCell("箱价", "¥${"%.2f".format(Locale.CHINA, item.result.costPerBox)}")
                    CostCell("包价", "¥${"%.2f".format(Locale.CHINA, item.result.costPerPackage)}")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "原料 ¥%,.2f + 加工 ¥%,.2f = ¥%,.2f"
                        .format(Locale.CHINA, item.materialCost, item.processingCost, item.totalCost),
                    fontSize = 12.sp,
                    color = Foggy
                )
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
                // 偏离明细：哪种原料、本批价、现价、涨跌幅
                if (item.priceDrifts.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    item.priceDrifts.forEach { drift ->
                        Text(
                            priceDriftDetail(drift),
                            fontSize = 12.sp,
                            color = Foggy
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                }
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

/** 折叠态横幅：指出本批哪些原料用了历史价（单项展示全价，双项以上收拢） */
private fun priceDriftBanner(drifts: List<com.example.shisuan.domain.model.IngredientPriceDrift>): String {
    if (drifts.isEmpty()) return ""
    if (drifts.size == 1) {
        val d = drifts[0]
        return "原料改价：${d.name} ¥${"%.2f".format(Locale.CHINA, d.batchPricePerKg)}" +
            "→¥${"%.2f".format(Locale.CHINA, d.latestPricePerKg)}/kg（本批为历史价）"
    }
    val firstTwo = drifts.take(2).joinToString("、") { it.name }
    return "原料改价：$firstTwo 等 ${drifts.size} 项（本批为历史价，展开查看）"
}

/** 展开态明细：原料名（含品牌）+ 本批价 → 现价 + 涨跌幅 */
private fun priceDriftDetail(drift: com.example.shisuan.domain.model.IngredientPriceDrift): String {
    val label = if (drift.brand.isNotEmpty()) "${drift.name}（${drift.brand}）" else drift.name
    val prices = "本批 ¥${"%.2f".format(Locale.CHINA, drift.batchPricePerKg)}/kg" +
        " → 现价 ¥${"%.2f".format(Locale.CHINA, drift.latestPricePerKg)}/kg"
    val pct = drift.diffPercent?.let {
        if (it > 0) " ↑${"%.1f".format(Locale.CHINA, it)}%"
        else " ↓${"%.1f".format(Locale.CHINA, -it)}%"
    } ?: ""
    return "$label：$prices$pct"
}

/** 按现价重算预览行：重算吨价 + 涨跌幅 + 影响种数，明确只算不存 */
private fun recalcPreviewLine(preview: com.example.shisuan.domain.model.RecalculatedPreview): String {
    val ton = "按现价重算约 ¥${"%,.0f".format(Locale.CHINA, preview.recalcTonCost)}/吨"
    val pct = preview.diffPercentPerTon?.let {
        if (it > 0) "↑${"%.1f".format(Locale.CHINA, it)}%"
        else "↓${"%.1f".format(Locale.CHINA, -it)}%"
    }
    val scope = if (pct != null) "$pct · ${preview.affectedCount}种原料" else "${preview.affectedCount}种原料"
    return "$ton（$scope），仅预览不改存档"
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
        // Diff 选择态：点「对比」选两个版本查看差异（单 sheet 内切换，不叠 bottom sheet）
        var diffBase by remember { mutableStateOf<BatchSnapshot?>(null) }
        var diffPair by remember { mutableStateOf<Pair<BatchSnapshot, BatchSnapshot>?>(null) }
        val pair = diffPair
        if (pair != null) {
            SnapshotDiffContent(
                a = pair.first,
                b = pair.second,
                onBack = { diffPair = null; diffBase = null }
            )
            return@ModalBottomSheet
        }
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
                "每次保存生成一个版本，#编号为内容指纹；点「对比」任选两个版本查看差异",
                fontSize = 12.sp,
                color = Foggy
            )
            diffBase?.let { base ->
                Spacer(Modifier.height(2.dp))
                Text(
                    "已选 #${base.digest} 为基准，再点另一个版本的「对比」即查看差异",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Rausch
                )
            }
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
                                    // Diff 入口：第一次点设为基准，第二次点即打开两版本对比
                                    val isBase = diffBase?.id == snap.id
                                    TextButton(
                                        onClick = {
                                            val base = diffBase
                                            when {
                                                base == null -> diffBase = snap
                                                base.id == snap.id -> diffBase = null
                                                else -> diffPair = base to snap
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            if (isBase) "取消选择" else "对比",
                                            fontSize = 12.sp,
                                            color = if (isBase) WarningOrange else Foggy
                                        )
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

/**
 * 版本对比视图（git diff 式）：两版本批次字段 + 配料增删改高亮。
 *
 * 新旧按版本号自动定向（小为旧、大为新），与点选顺序无关；
 * 解码失败（损坏快照）时明确提示而非空白页。
 */
@Composable
private fun SnapshotDiffContent(
    a: BatchSnapshot,
    b: BatchSnapshot,
    onBack: () -> Unit
) {
    val decoded = remember(a, b) {
        val da = BatchSnapshotCodec.decode(a.snapshotData)
        val db = BatchSnapshotCodec.decode(b.snapshotData)
        if (da == null || db == null) null
        else if (a.version <= b.version) Triple(a, b, SnapshotDiffer.diff(da, db))
        else Triple(b, a, SnapshotDiffer.diff(db, da))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("版本对比", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack) {
                Text("返回时间线", fontSize = 12.sp, color = Rausch)
            }
        }
        Spacer(Modifier.height(4.dp))
        if (decoded == null) {
            Text("快照数据损坏，无法对比", color = Foggy, fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))
        } else {
            val (oldS, newS, result) = decoded
            Text(
                "旧 #${oldS.digest}（第 ${oldS.version} 版） → 新 #${newS.digest}（第 ${newS.version} 版）",
                fontSize = 12.sp,
                color = Foggy
            )
            Spacer(Modifier.height(12.dp))
            if (!result.hasChanges) {
                Text("两个版本内容一致，无差异", color = Foggy, fontSize = 13.sp)
                Spacer(Modifier.height(24.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (result.fieldChanges.isNotEmpty()) {
                        item(key = "diff-fields-header") {
                            Text(
                                "批次字段",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ink
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        items(result.fieldChanges, key = { "diff-field-${it.label}" }) { f ->
                            Text(
                                "${f.label}：${f.oldText} → ${f.newText}",
                                fontSize = 13.sp,
                                color = Body
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        item(key = "diff-fields-gap") { Spacer(Modifier.height(8.dp)) }
                    }
                    if (result.ingredientDiffs.isNotEmpty()) {
                        item(key = "diff-ings-header") {
                            Text(
                                "配料变化",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Ink
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        items(
                            result.ingredientDiffs,
                            key = { "${it.kind}-${it.name}-${it.brand}-${it.detail}" }
                        ) { d ->
                            val color = when (d.kind) {
                                IngredientDiffKind.ADDED -> SuccessGreen
                                IngredientDiffKind.REMOVED -> DangerRed
                                IngredientDiffKind.CHANGED -> Ink
                            }
                            val prefix = when (d.kind) {
                                IngredientDiffKind.ADDED -> "＋ "
                                IngredientDiffKind.REMOVED -> "－ "
                                IngredientDiffKind.CHANGED -> "· "
                            }
                            Text(
                                "$prefix${d.label}：${d.detail}",
                                fontSize = 13.sp,
                                color = color
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}
