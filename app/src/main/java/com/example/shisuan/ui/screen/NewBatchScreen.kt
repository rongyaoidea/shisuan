package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.viewModel.NewBatchViewModel

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
    var processingCost by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    var showIngredientPicker by remember { mutableStateOf(false) }
    
    val ingredients by viewModel.ingredients.collectAsState()
    val allIngredients by viewModel.allIngredients.collectAsState()
    val totalMaterialCost by viewModel.totalMaterialCost.collectAsState()
    
    // 临时编辑用状态
    var editingName by remember { mutableStateOf("") }
    var editingWeight by remember { mutableStateOf("") }
    var editingPrice by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建批次", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = processingCost,
                        onValueChange = { processingCost = it },
                        label = { Text("加工费 (元)") },
                        singleLine = true,
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
                    LazyColumn(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        itemsIndexed(ingredients) { index, ingredient ->
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
            
            Spacer(Modifier.weight(1f))
            
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
                        Text("原料成本", color = Color(0xFF6B6B6B))
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
                        Text("加工费", color = Color(0xFF6B6B6B))
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
                            color = Color(0xFFE8345B)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8345B))
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
                        totalCost = weight * price
                    )
                )
                showIngredientPicker = false
            }
        )
    }
    
    // 临时编辑对话框（未使用状态声明）
    if (editingName.isNotEmpty()) { }
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
                color = Color(0xFF1A1A1A)
            )
            Text(
                "${"%.2f".format(ingredient.weight)}g × ¥${"%.2f".format(ingredient.unitPrice)}",
                fontSize = 12.sp,
                color = Color(0xFF6B6B6B)
            )
        }
        Text(
            "¥${"%.2f".format(ingredient.totalCost)}",
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A1A)
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                "删除",
                tint = Color(0xFFD00000),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 原料选择器底部抽屉
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientPickerSheet(
    ingredients: List<Ingredient>,
    onDismiss: () -> Unit,
    onPick: (Ingredient, Double, Double) -> Unit
) {
    var selected by remember { mutableStateOf<Ingredient?>(null) }
    var weight by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("选择原料", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            
            if (ingredients.isEmpty()) {
                Text(
                    "原料库为空，请先添加原料",
                    color = Color(0xFF6B6B6B),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // 原料列表（横向滚动 chips 或简单列表）
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
                                    Color(0xFFFFE0E6) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ingredient.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        ingredient.category,
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B6B6B)
                                    )
                                }
                            }
                        }
                    }
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
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("单价") },
                            singleLine = true,
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
                        enabled = (weight.toDoubleOrNull() ?: 0.0) > 0 && (price.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("添加")
                    }
                }
            }
        }
    }
}