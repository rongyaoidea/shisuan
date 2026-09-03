package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.shisuan.ShisuanApplication
import com.example.shisuan.ui.components.*
import com.example.shisuan.ui.theme.*
import com.example.shisuan.ui.viewModel.BatchViewModel
import com.example.shisuan.utils.CostCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchListScreen(onNavigateToDetail: (Long) -> Unit) {
    val app = LocalContext.current.applicationContext as ShisuanApplication
    val vm = viewModel { BatchViewModel(app.repo) }
    val batches by vm.batches.collectAsState()
    val config by vm.config.collectAsState()
    var sel by remember { mutableStateOf("全部") }
    var showSheet by remember { mutableStateOf(false) }

    val prods = remember(batches) { val s = mutableSetOf("全部"); batches.forEach { s.add(it.productName) }; s.toList() }
    val filtered = if (sel == "全部") batches else batches.filter { it.productName == sel }

    Scaffold(
        topBar = { TopAppBar(title = { Text("食 算", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF1A1A1A)) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        floatingActionButton = { FloatingActionButton(onClick = { showSheet = true }, containerColor = Color(0xFFE8345B)) { Icon(Icons.Default.Add, "新建批次", tint = Color.White) } }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (prods.size > 1) Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                prods.forEach { p -> FilterChip(selected = sel == p, onClick = { sel = p }, label = { Text(p) }, border = if (sel == p) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE8345B)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD0D0CC))) }
            }
            if (filtered.isEmpty()) EmptyState("📋", "还没有批次，点 ＋ 开始录入")
            else LazyColumn(contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(filtered, key = { _, b -> b.id }) { idx, b ->
                    val r = if (config != null) CostCalculator.calculate(b.sampleWeightGram, b.materialCost, b.processingCost, config!!.weightPerBoxGram, config!!.packagesPerBox) else null
                    val prev = batches.firstOrNull { x -> x.productName == b.productName && x.createdAt < b.createdAt && x.id != b.id }
                    val pt = if (prev != null && config != null) CostCalculator.calculate(prev.sampleWeightGram, prev.materialCost, prev.processingCost, config!!.weightPerBoxGram, config!!.packagesPerBox).unitCostPerTon else null
                    val d = CostCalculator.calcDifferential(r?.unitCostPerTon ?: 0.0, pt)
                    if (r != null) CostCard(b.productName, b.batchName, r.unitCostPerTon, r.costPerBox, r.costPerPackage, d, { onNavigateToDetail(b.id) }, index = idx)
                }
            }
        }
    }
    if (showSheet) NewBatchSheet({ showSheet = false }) { pn, bn, sw, mc, pc, n -> vm.saveBatch(pn, bn, sw, mc, pc, n); showSheet = false }
}
