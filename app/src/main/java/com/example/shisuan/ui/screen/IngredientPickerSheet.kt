package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.ui.components.QuickChipsRow
import com.example.shisuan.ui.components.StepperNumberField
import com.example.shisuan.ui.components.TextChipsRow
import com.example.shisuan.ui.components.formatNumber
import com.example.shisuan.ui.icons.Add
import com.example.shisuan.ui.icons.Camera
import com.example.shisuan.ui.theme.ButtonShape
import com.example.shisuan.ui.theme.Foggy
import com.example.shisuan.ui.theme.PillShape
import com.example.shisuan.ui.theme.Rausch
import com.example.shisuan.ui.theme.RauschDisabled
import com.example.shisuan.ui.theme.WarningOrange
import com.example.shisuan.utils.CostCalculator
import com.example.shisuan.utils.WeightFormatter
import java.util.Locale

/**
 * 原料选择器底部抽屉（自 NewBatchScreen 拆出，原同文件超 300 行）
 * 支持从原料库选择、快速添加新原料入库、OCR 拍照识别配料表
 *
 * 用量支持两种模式：
 * - 克重 (g)：直接填克重
 * - 比例 (%)：填占样品重量的百分比（香精/添加剂微量场景），按样品重量换算克重
 * 单价一律取原料库库存价，不再重复填写。
 *
 * 滚动说明：抽屉外层 Column 带 verticalScroll，内层原料列表用普通 Column
 * （不嵌套 LazyColumn），避免 LazyColumn 嵌 verticalScroll 的滚动冲突与测量异常。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientPickerSheet(
    ingredients: List<Ingredient>,
    sampleWeightGram: Double = 0.0,
    onDismiss: () -> Unit,
    onPick: (Ingredient, Double, Double?) -> Unit, // 原料, 克重, 比例%(null=按克重)
    onCreateIngredient: (String, String, String, Double) -> Unit = { _, _, _, _ -> },
    onOcrScan: () -> Unit = {}
) {
    var selected by remember { mutableStateOf<Ingredient?>(null) }
    var weight by remember { mutableStateOf("") }
    var usePercent by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newBrand by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("选择原料", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // OCR 识别入口
            OutlinedButton(
                onClick = onOcrScan,
                modifier = Modifier.fillMaxWidth(),
                shape = PillShape
            ) {
                androidx.compose.material3.Icon(Camera, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("拍照识别配料表")
            }

            if (ingredients.isEmpty() || showQuickAdd) {
                // 快速添加原料入库
                Text(
                    if (ingredients.isEmpty()) "原料库为空，先添加一种原料" else "新原料入库",
                    fontSize = 13.sp,
                    color = Foggy
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("原料名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newBrand,
                    onValueChange = { newBrand = it },
                    label = { Text("品牌（可选）") },
                    placeholder = { Text("留空=不区分品牌") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it },
                    label = { Text("分类（可选）") },
                    placeholder = { Text("如：水果 / 糖类 / 添加剂") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextChipsRow(
                    options = listOf("水果", "蔬菜", "蛋类", "乳制品", "糖类", "粮油", "添加剂", "包材", "其他"),
                    current = newCategory,
                    onPick = { newCategory = it }
                )
                StepperNumberField(
                    value = newPrice,
                    onValueChange = { newPrice = it },
                    label = "参考单价",
                    step = 0.5,
                    suffix = "元/kg"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (ingredients.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showQuickAdd = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("返回选择") }
                    }
                    Button(
                        onClick = {
                            val p = newPrice.toDoubleOrNull() ?: 0.0
                            if (newName.isNotBlank()) {
                                onCreateIngredient(newName.trim(), newBrand.trim(), newCategory.trim(), p)
                                newName = ""; newBrand = ""; newCategory = ""; newPrice = ""
                                showQuickAdd = false
                            }
                        },
                        enabled = newName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Rausch)
                    ) { Text("入库") }
                }
            } else {
                // 搜索过滤：原料多时免滚动查找；名称或品牌匹配，忽略大小写
                // 搜索框固定在列表顶部（外层滚动时随内容走，但始终在列表上方，属同一 Column）
                val filtered = if (search.isBlank()) ingredients else ingredients.filter {
                    it.name.contains(search, ignoreCase = true) ||
                        it.supplier.contains(search, ignoreCase = true)
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("搜索原料名称 / 品牌") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (filtered.isEmpty()) {
                    Text(
                        "没有匹配「${search.trim()}」的原料，可点下方「新原料入库」",
                        fontSize = 12.sp,
                        color = WarningOrange
                    )
                } else {
                    // 原料列表：普通 Column（外层已可滚动，不再嵌套 LazyColumn）
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        filtered.forEach { ingredient ->
                            val isSelected = selected?.id == ingredient.id
                            Card(
                                onClick = { selected = ingredient },
                                modifier = Modifier.fillMaxWidth(),
                                shape = ButtonShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        RauschDisabled else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ingredient.name, fontWeight = FontWeight.Medium)
                                        Row {
                                            if (ingredient.supplier.isNotEmpty()) {
                                                Text(
                                                    "${ingredient.supplier} ·",
                                                    fontSize = 11.sp,
                                                    color = Foggy
                                                )
                                            }
                                            if (ingredient.category.isNotEmpty()) {
                                                Text(
                                                    ingredient.category,
                                                    fontSize = 11.sp,
                                                    color = Foggy
                                                )
                                            }
                                            if (ingredient.unitPrice > 0) {
                                                Text(
                                                    " · ¥${"%.2f".format(Locale.CHINA, ingredient.unitPrice)}/${ingredient.priceUnit.removePrefix("元/")}",
                                                    fontSize = 11.sp,
                                                    color = Foggy
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { showQuickAdd = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("新原料入库")
                }

                // 用量输入（克重 / 比例两种模式）
                selected?.let { ing ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    // 输入模式切换
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !usePercent,
                            onClick = { usePercent = false },
                            label = { Text("克重 (g)", fontSize = 13.sp) }
                        )
                        FilterChip(
                            selected = usePercent,
                            onClick = { usePercent = true },
                            label = { Text("比例 (%)", fontSize = 13.sp) }
                        )
                    }
                    if (usePercent) {
                        StepperNumberField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = "占样品比例 (%)",
                            step = 0.01,
                            placeholder = "如 0.05（万分之五）"
                        )
                        QuickChipsRow(
                            options = listOf(0.01, 0.05, 0.1, 0.5, 1.0),
                            currentText = weight,
                            onPick = { weight = formatNumber(it, 2) },
                            places = 2,
                            suffix = "%"
                        )
                        val pct = weight.toDoubleOrNull() ?: 0.0
                        if (sampleWeightGram > 0 && pct > 0) {
                            Text(
                                "按样品 ${WeightFormatter.format(sampleWeightGram)} 换算 ≈ ${WeightFormatter.format(CostCalculator.ratioPercentToGram(sampleWeightGram, pct))}",
                                fontSize = 12.sp,
                                color = Foggy
                            )
                        } else if (sampleWeightGram <= 0) {
                            Text(
                                "请先在上方填写「样品重量」，才能按比例换算",
                                fontSize = 12.sp,
                                color = WarningOrange
                            )
                        }
                    } else {
                        StepperNumberField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = "用量 (g)",
                            step = 1.0
                        )
                        QuickChipsRow(
                            options = listOf(1.0, 5.0, 10.0, 20.0, 50.0, 100.0),
                            currentText = weight,
                            onPick = { weight = formatNumber(it, 0) },
                            places = 0,
                            suffix = "g"
                        )
                    }
                    // 单价只读：取原料库库存价，避免重复填写
                    Text(
                        if (ing.unitPrice > 0)
                            "单价（库存）：¥${"%.2f".format(Locale.CHINA, ing.unitPrice)}/${ing.priceUnit.removePrefix("元/")}"
                        else "该原料未设置库存单价，成本按 ¥0 计",
                        fontSize = 12.sp,
                        color = Foggy
                    )
                    Button(
                        onClick = {
                            if (usePercent) {
                                val pct = weight.toDoubleOrNull() ?: return@Button
                                if (pct > 0 && sampleWeightGram > 0) {
                                    val grams = CostCalculator.ratioPercentToGram(sampleWeightGram, pct)
                                    onPick(ing, grams, pct)
                                }
                            } else {
                                val w = weight.toDoubleOrNull() ?: return@Button
                                if (w > 0) onPick(ing, w, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = if (usePercent) {
                            (weight.toDoubleOrNull() ?: 0.0) > 0 && sampleWeightGram > 0
                        } else {
                            (weight.toDoubleOrNull() ?: 0.0) > 0
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Rausch)
                    ) {
                        Text("添加")
                    }
                }
            }
        }
    }
}
