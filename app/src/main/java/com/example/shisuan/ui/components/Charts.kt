package com.example.shisuan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shisuan.ui.theme.Foggy
import com.example.shisuan.ui.theme.Ink
import kotlin.math.max

/**
 * 成本趋势折线图 + 配料占比环形图。
 *
 * 用 Compose Canvas 自绘而非引入图表库：
 * 项目保持零冗余依赖，且两个图的形态都很简单，自绘完全可控。
 */

/** 环形图色板：主红起手，按占比依次取色，超出后循环 */
private val DonutPalette = listOf(
    Color(0xFFD85A30),
    Color(0xFFEF9F27),
    Color(0xFF639922),
    Color(0xFF378ADD),
    Color(0xFF7F77DD),
    Color(0xFFD4537E),
    Color(0xFF1D9E75),
    Color(0xFF888780)
)

/**
 * 批次吨价趋势折线图。
 *
 * @param data 按时间顺序的 (批次名, 吨价)；少于 2 个点不绘制
 */
@Composable
fun CostTrendChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return
    val maxValue = data.maxOf { it.second }
    val minValue = data.minOf { it.second }
    if (maxValue <= 0.0) return
    val range = max(maxValue - minValue, 1.0)
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "最低 ¥%,.0f".format(minValue),
                fontSize = 11.sp,
                color = Color(0xFF3B6D11)
            )
            Text(
                "最高 ¥%,.0f".format(maxValue),
                fontSize = 11.sp,
                color = Color(0xFF854F0B)
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(vertical = 4.dp)
        ) {
            val stepX = size.width / (data.size - 1)
            val points = data.mapIndexed { index, (_, value) ->
                val norm = ((value - minValue) / range).toFloat().coerceIn(0f, 1f)
                // 上下各留 8% 边距，避免端点贴边被裁切
                Offset(index * stepX, size.height * (0.92f - norm * 0.84f))
            }
            // 网格基线
            drawLine(
                color = gridColor,
                start = Offset(0f, size.height * 0.92f),
                end = Offset(size.width, size.height * 0.92f),
                strokeWidth = 2f
            )
            // 折线
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = DonutPalette[0],
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
            // 数据点：外圈实色 + 内圈白心
            points.forEach { p ->
                drawCircle(DonutPalette[0], radius = 9f, center = p)
                drawCircle(Color.White, radius = 4f, center = p)
            }
        }
        // 批次名轴标签：均分宽度，超出截断
        Row(modifier = Modifier.fillMaxWidth()) {
            data.forEach { (name, _) ->
                Text(
                    name.takeLast(5),
                    fontSize = 10.sp,
                    color = Foggy,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * 配料成本占比环形图（含图例）。
 *
 * @param items (配料名, 成本)；总成本非正时不绘制
 */
@Composable
fun IngredientCostDonut(
    items: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val total = items.sumOf { it.second }
    if (total <= 0.0 || items.isEmpty()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(110.dp)) {
            var startAngle = -90f
            items.forEachIndexed { index, (_, value) ->
                val sweep = (value / total * 360.0).toFloat()
                drawArc(
                    color = DonutPalette[index % DonutPalette.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = size.minDimension / 5)
                )
                startAngle += sweep
            }
        }
        Spacer(Modifier.width(16.dp))
        // 图例：按占比降序排列，但颜色沿用各配料在扇区绘制时的原始顺序色，
        // 保证图例色块与环形图扇区一一对应（扇区按 items 原始顺序取色）
        Column(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.withIndex()
                .sortedByDescending { it.value.second }
                .forEach { indexedValue ->
                    val (sectorIndex, item) = indexedValue
                    val (name, value) = item
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawRect(DonutPalette[sectorIndex % DonutPalette.size])
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            name,
                            fontSize = 12.sp,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(88.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "%.0f%%".format(value / total * 100),
                            fontSize = 12.sp,
                            color = Foggy,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
        }
    }
}
