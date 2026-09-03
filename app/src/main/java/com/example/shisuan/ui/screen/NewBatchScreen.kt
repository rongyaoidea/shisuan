package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.theme.DangerRed
import com.example.shisuan.ui.theme.AirbnbRed
import com.example.shisuan.ui.theme.AirbnbRedLight
import com.example.shisuan.ui.theme.TextPrimary
import com.example.shisuan.ui.theme.TextSecondary
import com.example.shisuan.ui.viewModel.NewBatchViewModel
import com.example.shisuan.utils.CostCalculator

/**
 * 新建批次页 - 配置原料配料
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBatchScreen(
    productId: Long,
    onNavigateBack: () -> Unit,
    viewModel: NewBatchViewModel = hiltViewModel()
) {
    var batchName by remember { mutableStateOf("") }
    var sampleWeight by remember { mutableStateOf("") }
    var processingCost by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showIngredientPicker by remember { mutableStateOf(false) }

    val ingredients by viewModel.ingredients.collectAsState()
    val allIngredients by viewModel.allIngredients.collectAsState()
    val totalMaterialCost by viewModel.totalMaterialCost.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建批次", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基本信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = batchName,
                        onValueChange = { batchName = it },
                        label = { Text("批次编号 *") },
                        placeholder = { Text("如：试产01") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sampleWeight,
                        onValueChange = { sampleWeight = it },
                        label = { Text("样品重量 (g) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = processingCost,
                        onValueChange = { processingCost = it },
                        label = { Text("加工费 (元)") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 原料配料列表
            Text(
                "原料配料",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (ingredients.isEmpty()) {
                EmptyState("🧪", "还没有添加原料，点下方按钮添加")
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        ingredients.forEachIndexed { index, ingredient ->
                            IngredientRow(
                                ingredient = ingredient,
                                onDelete = { viewModel.removeIngredient(ingredient) },
                                isLast = index == ingredients.size - 1
                            )
                        }
                    }
                }
            }

            // 添加原料按钮
            OutlinedButton(
                onClick = { showIngredientPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("添加原料")
            }

            // 底部汇总和保存
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("原料成本", color = TextSecondary)
                        Text(
                            "¥%,.2f".format(totalMaterialCost),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("加工费", color = TextSecondary)
                        Text("¥%,.2f".format(processingCost.toDoubleOrNull() ?: 0.0))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("总成本", fontWeight = FontWeight.Bold)
                        Text(
                            "¥%,.2f".format(totalMaterialCost + (processingCost.toDoubleOrNull() ?: 0.0)),
                            fontWeight = FontWeight.Bold,
                            color = AirbnbRed
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val weight = sampleWeight.toDoubleOrNull() ?: return@Button
                    if (batchName.isNotBlank() && weight > 0) {
                        viewModel.saveBatch(
                            productId = productId,
                            batchName = batchName,
                            sampleWeight = weight,
                            processingCost = processingCost.toDoubleOrNull() ?: 0.0,
                            note = note
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = batchName.isNotBlank() && (sampleWeight.toDoubleOrNull() ?: 0.0) > 0,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AirbnbRed)
            ) {
                Text("保存批次", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }

    // 原料选择器底部抽屉
    if (showIngredientPicker) {
        IngredientPickerSheet(
            ingredients = allIngredients,
            onDismiss = { showIngredientPicker = false },
            onPick = { ingredient, weight, price ->
                viewModel.addIngredient(
                    BatchIngredient(
                        batchId = 0, // 保存时再关联
                        ingredientName = ingredient.name,
                        ingredientId = ingredient.id,
                        weight = weight,
                        unitPrice = price,
                        // 元/kg 单价 × 克重 ÷ 1000 —— 由 Rust 引擎换算
                        totalCost = CostCalculator.unitPriceToTotal(weight, price, isPerGram = false)
                    )
                )
                showIngredientPicker = false
            },
            onCreateIngredient = { name, category, price ->
                viewModel.saveIngredient(name, category, price)
            }
        )
    }
}

/**
 * 原料行
 */
@Composable
private fun IngredientRow(
    ingredient: BatchIngredient,
    onDelete: () -> Unit,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                ingredient.ingredientName,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                "${"%.2f".format(ingredient.weight)}g × ¥${"%.2f".format(ingredient.unitPrice)}/kg",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Text(
            "¥${"%.2f".format(ingredient.totalCost)}",
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                "删除",
                tint = DangerRed,
                modifier = Modifier.size(18.dp)
            )
        }
    }
    if (!isLast) {
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

/**
 * 原料选择器底部抽屉
 * 支持从原料库选择，或快速添加新原料入库
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientPickerSheet(
    ingredients: List<Ingredient>,
    onDismiss: () -> Unit,
    onPick: (Ingredient, Double, Double) -> Unit,
    onCreateIngredient: (String, String, Double) -> Unit = { _, _, _ -> }
) {
    var selected by remember { mutableStateOf<Ingredient?>(null) }
    var weight by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var showQuickAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
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

            if (ingredients.isEmpty() || showQuickAdd) {
                // 快速添加原料入库
                Text(
                    if (ingredients.isEmpty()) "原料库为空，先添加一种原料" else "新原料入库",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("原料名称 *") },
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
                OutlinedTextField(
                    value = newPrice,
                    onValueChange = { newPrice = it },
                    label = { Text("参考单价 (元/kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
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
                                onCreateIngredient(newName.trim(), newCategory.trim(), p)
                                newName = ""; newCategory = ""; newPrice = ""
                                showQuickAdd = false
                            }
                        },
                        enabled = newName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AirbnbRed)
                    ) { Text("入库") }
                }
            } else {
                // 原料列表
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(ingredients) { _, ingredient ->
                        val isSelected = selected?.id == ingredient.id
                        Card(
                            onClick = { selected = ingredient },
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    AirbnbRedLight else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ingredient.name, fontWeight = FontWeight.Medium)
                                    Row {
                                        if (ingredient.category.isNotEmpty()) {
                                            Text(
                                                ingredient.category,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        if (ingredient.unitPrice > 0) {
                                            Text(
                                                " · ¥${"%.2f".format(ingredient.unitPrice)}/kg",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
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
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("新原料入库")
                }

                // 用量和单价输入
                selected?.let { ing ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("用量 (g)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("单价 (元/kg)") },
                            placeholder = {
                                if (ing.unitPrice > 0) Text("库存价 ${"%.2f".format(ing.unitPrice)}")
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            val w = weight.toDoubleOrNull() ?: return@Button
                            val p = price.toDoubleOrNull() ?: return@Button
                            if (w > 0 && p > 0) onPick(ing, w, p)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = (weight.toDoubleOrNull() ?: 0.0) > 0 && (price.toDoubleOrNull() ?: 0.0) > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = AirbnbRed)
                    ) {
                        Text("添加")
                    }
                }
            }
        }
    }
}
