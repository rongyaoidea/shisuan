package com.example.shisuan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shisuan.ui.animation.entranceAnimation
import com.example.shisuan.ui.animation.pressScale
import com.example.shisuan.ui.theme.*

@Composable
fun CostCard(
    productName: String, batchName: String,
    unitCostPerTon: Double, costPerBox: Double, costPerPackage: Double,
    diffPercent: Double?, onClick: () -> Unit,
    index: Int = 0
) {
    val up = diffPercent != null && diffPercent > 0.1
    val down = diffPercent != null && diffPercent < -0.1
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .pressScale(onClick = onClick)
            .entranceAnimation(index = index),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$productName · $batchName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                Spacer(Modifier.weight(1f))
                if (diffPercent != null) {
                    val (c, l) = when { down -> SuccessGreen to "↓ ${"%.1f".format(-diffPercent)}%" ; up -> WarningOrange to "↑ ${"%.1f".format(diffPercent)}%" ; else -> Color(0xFF999999) to "持平" }
                    Text(l, color = c, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                hCell("吨价", "¥${"%,.0f".format(unitCostPerTon)}")
                hCell("箱价", "¥${"%.2f".format(costPerBox)}")
                hCell("包价", "¥${"%.2f".format(costPerPackage)}")
            }
        }
    }
}

@Composable private fun hCell(label: String, value: String) {
    Column { Text(label, fontSize = 12.sp, color = Color(0xFF6B6B6B)) ; Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A)) }
}

@Composable fun CostResultBlock(result: com.example.shisuan.domain.model.CostResult) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¥ ${"%.4f".format(result.unitCostPerGram)} / g", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            rCell("吨价", "¥${"%,.0f".format(result.unitCostPerTon)}")
            rCell("箱价", "¥${"%.2f".format(result.costPerBox)}")
            rCell("包价", "¥${"%.2f".format(result.costPerPackage)}")
        }
        Spacer(Modifier.height(8.dp))
        Text("每吨可出 ${"%.0f".format(result.boxesPerTon)} 箱", fontSize = 13.sp, color = Color(0xFF6B6B6B))
    }
}

@Composable private fun rCell(label: String, value: String) {
    Box(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 12.sp, color = Color(0xFF6B6B6B))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
        }
    }
}

@Composable fun BrutalInput(value: String, onValueChange: (String) -> Unit, label: String = "", suffix: String = "", keyboardType: KeyboardType = KeyboardType.Text) {
    Column {
        if (label.isNotEmpty()) Text(label, fontSize = 13.sp, color = Color(0xFF6B6B6B), modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            modifier = Modifier.fillMaxWidth().height(52.dp).border(1.5.dp, Color(0xFF2D2D2D), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE8345B), unfocusedBorderColor = Color(0xFFD0D0CC), focusedContainerColor = Color.White, unfocusedContainerColor = Color(0xFFF2F2F0), cursorColor = Color(0xFFE8345B)),
            textStyle = LocalTextStyle.current.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 16.sp),
            suffix = { if (suffix.isNotEmpty()) Text(suffix, fontSize = 14.sp, color = Color(0xFF6B6B6B)) }
        )
    }
}

@Composable fun BrutalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = true) {
    val cc = if (filled) Color(0xFFE8345B) else Color.Transparent
    val bc = if (filled) Color.Transparent else Color(0xFF2D2D2D)
    val tc = if (filled) Color.White else Color(0xFF1A1A1A)
    Box(
        modifier = modifier.height(52.dp).border(1.5.dp, bc, RoundedCornerShape(12.dp)).background(cc, RoundedCornerShape(12.dp)).pressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = tc, letterSpacing = 2.sp)
    }
}

@Composable fun EmptyState(icon: String, message: String) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(icon, fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF6B6B6B), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** 可滚动的新建批次表单 — 保存按钮始终可触达 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBatchSheet(onDismiss: () -> Unit, onSave: (String, String, String, String, String, String) -> Unit) {
    var pn by remember { mutableStateOf("") }
    var bn by remember { mutableStateOf("") }
    var sw by remember { mutableStateOf("") }
    var mc by remember { mutableStateOf("") }
    var pc by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新建批次", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            BrutalInput(pn, { pn = it }, "产品名称")
            BrutalInput(bn, { bn = it }, "批次编号")
            BrutalInput(sw, { sw = it }, "样品重量", "g", KeyboardType.Decimal)
            BrutalInput(mc, { mc = it }, "原料成本", "元", KeyboardType.Decimal)
            BrutalInput(pc, { pc = it }, "加工费(可选)", "元", KeyboardType.Decimal)
            BrutalInput(note, { note = it }, "备注")
            Spacer(Modifier.height(8.dp))
            BrutalButton("保 存", { if (pn.isNotBlank() && bn.isNotBlank()) onSave(pn, bn, sw, mc, pc, note) })
            Spacer(Modifier.height(32.dp))
        }
    }
}
