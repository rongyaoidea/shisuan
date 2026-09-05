package com.example.shisuan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shisuan.ui.theme.Foggy
import com.example.shisuan.ui.theme.Ink
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToInt

/**
 * 便捷输入组件库
 *
 * 设计目标：在**不减少功能与信息**的前提下，把一线人员的高频数字录入从
 * 「弹出键盘 → 逐位输入 → 检查」降为「点加减 / 拖滑条 / 点常用标签」，
 * 键盘输入保留作为兜底精确通道。
 *
 * - [StepperNumberField]  数字输入框 + −/＋ 步进按钮：适合重量、金额等小步调整
 * - [SliderNumberField]   数字输入框 + 滑条：适合有明确区间的值（出品率、pH、毛利率）
 * - [QuickChipsRow]       常用数值快捷标签：一键填入，点中高亮
 * - [TextChipsRow]        常用文本快捷标签：一键填入，再次点选清除（可选字段）
 */

/** 数值文本格式化：保留最多 [places] 位小数并去尾零（2.50 → 2.5、1000.0 → 1000） */
fun formatNumber(value: Double, places: Int = 2): String =
    BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

/** 按步长推断小数位数：step=1 → 0 位，step=0.01 → 2 位 */
private fun decimalsFor(step: Double): Int =
    if (step >= 1.0) 0
    else BigDecimal.valueOf(step).stripTrailingZeros().scale().coerceAtLeast(0)

/**
 * 带 −/＋ 步进按钮的数字输入框。
 *
 * 点击按钮以 [step] 增减，结果钳制在 [min] ~ [max]；
 * 键盘输入通道完整保留（步进只改文本，不接管状态）。
 * 空文本视为 0 起步。
 */
@Composable
fun StepperNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    step: Double = 1.0,
    min: Double = 0.0,
    max: Double = Double.MAX_VALUE,
    placeholder: String? = null,
    suffix: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            suffix = suffix?.let { { Text(it, color = Foggy, fontSize = 13.sp) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        StepButton("−") {
            adjustValue(value, -step, min, max, onValueChange)
        }
        StepButton("＋") {
            adjustValue(value, step, min, max, onValueChange)
        }
    }
}

private fun adjustValue(
    value: String,
    delta: Double,
    min: Double,
    max: Double,
    onValueChange: (String) -> Unit
) {
    val current = value.toDoubleOrNull() ?: 0.0
    val next = (current + delta).coerceIn(min, max)
    onValueChange(formatNumber(next, decimalsFor(kotlin.math.abs(delta))))
}

/**
 * 数字输入框 + 滑条。
 *
 * 适合有天然区间的数值：拖动粗调、键盘细调，两者共享同一文本状态。
 * 空文本时滑条停在 [range] 起点，拖动即写入值。
 *
 * @param increment 滑条最小步进（如 1f 整数步、0.1f 十分位步）
 */
@Composable
fun SliderNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    range: ClosedFloatingPointRange<Float>,
    increment: Float,
    modifier: Modifier = Modifier,
    places: Int = 1,
    suffix: String = "",
    placeholder: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            suffix = { Text(suffix, color = Foggy, fontSize = 13.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        val current = value.toDoubleOrNull()?.toFloat()
            ?.coerceIn(range.start, range.endInclusive) ?: range.start
        val steps = (((range.endInclusive - range.start) / increment).roundToInt() - 1)
            .coerceAtLeast(0)
        Slider(
            value = current,
            onValueChange = { onValueChange(formatNumber(it.toDouble(), places)) },
            valueRange = range,
            steps = steps
        )
    }
}

/**
 * 常用数值快捷标签行：一键填入，当前值与某项相等时该项高亮。
 *
 * @param places 标签显示与写入时的小数位（重量用 0、比例用 2）
 */
@Composable
fun QuickChipsRow(
    options: List<Double>,
    currentText: String,
    onPick: (Double) -> Unit,
    modifier: Modifier = Modifier,
    places: Int = 2,
    suffix: String = "",
) {
    val current = currentText.toDoubleOrNull()
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(options) { option ->
            val selected = current != null &&
                formatNumber(current, places) == formatNumber(option, places)
            FilterChip(
                selected = selected,
                onClick = { onPick(option) },
                label = { Text(formatNumber(option, places) + suffix, fontSize = 12.sp) }
            )
        }
    }
}

/**
 * 常用文本快捷标签行：一键填入；再次点选已选项则清除（适合可选字段）。
 */
@Composable
fun TextChipsRow(
    options: List<String>,
    current: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(options) { option ->
            FilterChip(
                selected = current == option,
                onClick = { onPick(if (current == option) "" else option) },
                label = { Text(option, fontSize = 12.sp) }
            )
        }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    OutlinedIconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Text(symbol, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}
