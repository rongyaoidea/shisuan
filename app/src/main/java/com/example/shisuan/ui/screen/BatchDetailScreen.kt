package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shisuan.ShisuanApplication
import com.example.shisuan.ui.components.*
import com.example.shisuan.ui.theme.*
import com.example.shisuan.ui.viewModel.BatchViewModel
import com.example.shisuan.utils.CostCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDetailScreen(batchId: Long, onNavigateBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as ShisuanApplication
    val vm = viewModel { BatchViewModel(app.repo) }
    val config by vm.config.collectAsState()
    val cr by vm.currentCostResult.collectAsState()
    val ptc by vm.previousTonCost.collectAsState()
    val problems by vm.problems.collectAsState()
    val batches by vm.batches.collectAsState()
    val batch = batches.find { it.id == batchId }
    LaunchedEffect(batchId) { vm.selectBatch(batchId) }
    val diff = cr?.let { CostCalculator.calcDifferential(it.unitCostPerTon, ptc) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (batch == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("加载中...", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF6B6B6B))
            }
            return@Scaffold
        }
        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            dSection("📝 基本信息") {
                dRow("产品名称", batch.productName)
                dRow("批次编号", batch.batchName)
                dRow("创建日期", java.time.Instant.ofEpochMilli(batch.createdAt).toString().take(10))
            }
            dSection("🔢 投入数据") {
                BrutalInput(batch.productName, {}, "产品名称")
                BrutalInput(batch.sampleWeightGram.toString(), {}, "样品重量", "g", KeyboardType.Decimal)
                BrutalInput(batch.materialCost.toString(), {}, "原料成本", "元", KeyboardType.Decimal)
                if (batch.processingCost > 0) BrutalInput(batch.processingCost.toString(), {}, "加工费", "元", KeyboardType.Decimal)
            }
            cr?.let { CostResultBlock(it) }
            dSection("📈 趋势") {
                Card(modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("趋势图待实现", fontSize = 13.sp, color = Color(0xFF999999))
                    }
                }
                if (diff != null) {
                    val (c, l) = when { diff < -0.1 -> SuccessGreen to "较上次 ↓ ${"%.1f".format(-diff)}%" ; diff > 0.1 -> WarningOrange to "较上次 ↑ ${"%.1f".format(diff)}%" ; else -> Color(0xFF999999) to "持平" }
                    Text(l, color = c, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { }, modifier = Modifier.weight(1f).height(44.dp), border = BorderStroke(1.5.dp, Color(0xFF2D2D2D)), shape = RoundedCornerShape(12.dp)) { Text("✨ 成果", fontSize = 13.sp) }
                OutlinedButton(onClick = { }, modifier = Modifier.weight(1f).height(44.dp), border = BorderStroke(1.5.dp, Color(0xFF2D2D2D)), shape = RoundedCornerShape(12.dp)) { Text("⚠️ 问题", fontSize = 13.sp) }
            }
            BrutalButton("删 除", { }, modifier = Modifier.fillMaxWidth(), filled = false)
        }
    }
}

@Composable private fun dSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B6B6B), modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}
@Composable private fun dRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = Color(0xFF6B6B6B))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
    }
}
