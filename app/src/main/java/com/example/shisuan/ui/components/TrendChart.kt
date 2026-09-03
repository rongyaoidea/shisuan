package com.example.shisuan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shisuan.data.repository.CostRepository
import com.example.shisuan.ui.theme.*

@Composable
fun TrendChart(points: List<CostRepository.TrendPoint>, modifier: Modifier = Modifier, height: Int = 160) {
    if (points.isEmpty()) {
        Box(modifier = modifier.height(height.dp), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Text(text = "暂无数据", fontSize = 13.sp, color = TextTertiary)
        }
        return
    }
    val colors = listOf(AirbnbRed, SuccessGreen, WarningOrange, InfoBlue, Color(0xFF8B5CF6))
    val productColors = mutableMapOf<String, Color>()
    points.forEach { p -> if (p.productName !in productColors) productColors[p.productName] = colors[productColors.size % colors.size] }
    val productGroups = points.groupBy { it.productName }
    val allValues = points.map { it.unitCostPerTon }
    val minVal = allValues.minOrNull() ?: 0.0
    val maxVal = allValues.maxOrNull() ?: 1.0
    val dataRange = maxVal - minVal
    val pad = 32.dp
    Canvas(modifier = modifier.fillMaxWidth().height(height.dp).padding(horizontal = pad.value.dp / 2)) {
        val w = size.width
        val h = size.height
        val chartW = w - pad.value.dp.toPx() * 2
        val chartH = h - 30.dp.toPx()
        repeat(5) { i ->
            val y = h - 10.dp.toPx() - (chartH * i / 4f)
            drawLine(color = DividerGray, start = Offset(pad.value.dp.toPx(), y), end = Offset(w - pad.value.dp.toPx(), y), strokeWidth = 1f)
        }
        productGroups.forEach { (name, pts) ->
            val color = productColors[name] ?: AirbnbRed
            if (pts.size < 2) return@forEach
            val path = Path()
            pts.sortedBy { it.date }.forEachIndexed { i, p ->
                val x = pad.value.dp.toPx() + (i.toFloat() / (pts.size - 1f)) * chartW
                val numerator = (p.unitCostPerTon - minVal).toFloat()
                val denom = if (dataRange > 0) dataRange.toFloat() else 1f
                val y = h - 10.dp.toPx() - (numerator / denom) * chartH
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
            }
            drawPath(path, color = color, style = Stroke(width = 2.5f))
        }
    }
}
