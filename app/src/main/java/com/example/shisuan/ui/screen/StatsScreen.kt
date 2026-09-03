package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.shisuan.ui.components.BrutalButton
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.theme.*
import com.example.shisuan.ui.viewModel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    val app = LocalContext.current.applicationContext as ShisuanApplication
    val vm = viewModel { StatsViewModel(app.repo) }
    val products by vm.products.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("统 计", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF1A1A1A)) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) }) { padding ->
        if (products.isEmpty()) { EmptyState("📊", "暂无统计数据") ; return@Scaffold }
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val tb = products.sumOf { it.batchCount }
            val at = products.map { it.avgTonCost }.average()
            val vp = products.filter { it.variation > 0.0 }
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("月度汇总", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B6B6B))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        sItem("本月批次", "$tb 个") ; sItem("平均吨价", "¥${"%,.0f".format(at)}") ; sItem("波动产品", "${vp.size} 个")
                    }
                }
            }
            dSection("各产品吨价排名") { products.take(5).forEachIndexed { i, p -> RankRow(i+1, p.productName, p.avgTonCost) } }
            if (products.any { it.variation > 0.0 }) dSection("成本波动 TOP5") {
                products.filter { it.variation > 0.0 }.sortedByDescending { it.variation }.take(5).forEach { p ->
                    val pct = if (p.avgTonCost > 0.0) (p.variation / p.avgTonCost * 100).toInt() else 0
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(p.productName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A))
                        Text(if (pct > 5) "⚠️ ±${pct}%" else "±${pct}%", fontSize = 13.sp, color = if (pct > 5) Color(0xFFE8830A) else Color(0xFF999999))
                    }
                }
            }
            BrutalButton("导 出 CSV", { })
            BrutalButton("导 出 JSON 备份", { }, filled = false)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable private fun sItem(label: String, value: String) { Column { Text(label, fontSize = 12.sp, color = Color(0xFF6B6B6B)) ; Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A)) } }
@Composable private fun RankRow(i: Int, name: String, cost: Double) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$i.", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF999999), modifier = Modifier.width(24.dp))
        Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1A1A1A), modifier = Modifier.weight(1f))
        Text("¥${"%,.0f".format(cost)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
    }
}
@Composable private fun dSection(title: String, content: @Composable () -> Unit) {
    Column { Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B6B6B), modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
    }
}
