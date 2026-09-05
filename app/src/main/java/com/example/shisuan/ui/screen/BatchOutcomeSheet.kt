package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shisuan.data.database.BatchResult
import com.example.shisuan.ui.components.SliderNumberField
import com.example.shisuan.ui.components.TextChipsRow
import com.example.shisuan.ui.components.formatNumber
import com.example.shisuan.ui.theme.Foggy
import com.example.shisuan.ui.theme.Ink
import com.example.shisuan.ui.theme.Rausch
import kotlin.math.round

/**
 * 批次成果记录面板（BatchResult 表的录入 UI）
 *
 * 录入试产成果：总体评分、口感、颜色、pH、糖度(Brix)，与批次成本数据关联，
 * 用于回答「哪个配方既好又便宜」。
 *
 * 便捷输入设计：
 * - 评分/滑条：1~5 整数步进，拖动即定，免键盘
 * - 口感/颜色：常用值快捷标签，点选填入、再点清除，保留键盘兜底
 * - pH/糖度：滑条粗调 + 输入框细调
 *
 * 未覆盖的字段（aroma/taste/appearance 等）在保存时原样保留，不丢失历史数据。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchOutcomeSheet(
    batchId: Long,
    existing: BatchResult?,
    onDismiss: () -> Unit,
    onSave: (BatchResult) -> Unit
) {
    var rating by remember { mutableFloatStateOf(3f) }
    var texture by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var pH by remember { mutableStateOf("") }
    var brix by remember { mutableStateOf("") }

    // 数据库既有成果异步到达后回填。该流程中只有「保存」会触发表变更，
    // 而保存后面板即关闭，因此不会覆盖用户正在编辑的内容。
    LaunchedEffect(existing) {
        existing?.let {
            rating = it.overallRating?.toFloat() ?: 3f
            texture = it.texture ?: ""
            color = it.color ?: ""
            pH = it.pHValue?.let { v -> formatNumber(v, 1) } ?: ""
            brix = it.brixDegree?.let { v -> formatNumber(v, 1) } ?: ""
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("批次成果记录", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink)
            Text(
                "记录试产结果，与成本数据对照复盘：好批次是否也便宜",
                fontSize = 12.sp,
                color = Foggy
            )

            // 总体评分：滑条 1~5
            Text(
                "总体评分：${"★".repeat(rating.toInt())}（${rating.toInt()}/5）",
                fontSize = 14.sp,
                color = Ink,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = rating,
                onValueChange = { rating = round(it).coerceIn(1f, 5f) },
                valueRange = 1f..5f,
                steps = 3
            )

            Text("口感", fontSize = 13.sp, color = Foggy)
            TextChipsRow(
                options = listOf("细腻", "顺滑", "偏粗", "凝胶", "正常"),
                current = texture,
                onPick = { texture = it }
            )

            Text("颜色", fontSize = 13.sp, color = Foggy)
            TextChipsRow(
                options = listOf("正常", "偏深", "偏浅", "褐变"),
                current = color,
                onPick = { color = it }
            )

            SliderNumberField(
                value = pH,
                onValueChange = { pH = it },
                label = "pH（可选）",
                range = 2f..9f,
                increment = 0.1f,
                places = 1
            )
            SliderNumberField(
                value = brix,
                onValueChange = { brix = it },
                label = "糖度 Brix（可选）",
                range = 0f..70f,
                increment = 0.5f,
                places = 1
            )

            Button(
                onClick = {
                    val base = existing ?: BatchResult(batchId = batchId)
                    onSave(
                        base.copy(
                            overallRating = rating.toInt(),
                            texture = texture.ifBlank { null },
                            color = color.ifBlank { null },
                            pHValue = pH.toDoubleOrNull(),
                            brixDegree = brix.toDoubleOrNull()
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rausch)
            ) {
                Text("保存成果", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
