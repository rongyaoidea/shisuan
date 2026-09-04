package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.icons.Add
import com.example.shisuan.ui.icons.ArrowBack
import com.example.shisuan.ui.icons.Delete
import com.example.shisuan.ui.icons.Edit
import com.example.shisuan.ui.icons.Flask
import com.example.shisuan.ui.theme.*
import com.example.shisuan.ui.viewModel.IngredientLibraryViewModel

/**
 * 配料库页 - 全局原料管理
 * 展示所有产品录入过的原料（名称 + 品牌 + 最新成本），跨产品复用。
 * 同款原料不同品牌各自建档（名称+品牌为去重键），品牌可留空。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientLibraryScreen(
    onNavigateBack: () -> Unit,
    viewModel: IngredientLibraryViewModel = hiltViewModel()
) {
    val ingredients by viewModel.ingredients.collectAsState()
    val errorMessage by viewModel.error.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Ingredient?>(null) }
    var pendingDelete by remember { mutableStateOf<Ingredient?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配料库", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Rausch
            ) {
                Icon(Add, "新增原料", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        if (ingredients.isEmpty()) {
            EmptyState(Flask, "配料库为空，点 ＋ 添加第一种原料")
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ingredients, key = { it.id }) { ingredient ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    ingredient.name,
                                    fontWeight = FontWeight.Medium,
                                    color = Ink
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    buildString {
                                        if (ingredient.supplier.isNotEmpty()) {
                                            append("品牌：")
                                            append(ingredient.supplier)
                                            append(" · ")
                                        }
                                        if (ingredient.category.isNotEmpty()) {
                                            append(ingredient.category)
                                            append(" · ")
                                        }
                                        append("¥")
                                        append("%.2f".format(ingredient.unitPrice))
                                        append("/kg")
                                    },
                                    fontSize = 12.sp,
                                    color = Foggy
                                )
                            }
                            IconButton(onClick = { editing = ingredient }) {
                                Icon(
                                    Edit, "编辑",
                                    tint = Foggy, modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { pendingDelete = ingredient }) {
                                Icon(
                                    Delete, "删除",
                                    tint = DangerRed, modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 新增原料对话框
    if (showAddDialog) {
        IngredientEditDialog(
            title = "新增原料",
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, brand, category, price ->
                viewModel.saveIngredient(name, brand, category, price)
                showAddDialog = false
            }
        )
    }

    // 编辑原料对话框
    editing?.let { ingredient ->
        IngredientEditDialog(
            title = "编辑原料",
            initial = ingredient,
            onDismiss = { editing = null },
            onSave = { name, brand, category, price ->
                viewModel.updateIngredient(ingredient, name, brand, category, price)
                editing = null
            }
        )
    }

    // 删除确认
    pendingDelete?.let { ingredient ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除原料") },
            text = { Text("确定从配料库删除「${ingredient.name}」吗？已有批次中的记录不受影响。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteIngredient(ingredient)
                        pendingDelete = null
                    }
                ) { Text("删除", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

/**
 * 新增/编辑原料对话框
 * 同款原料不同品牌可分别建档（品牌留空 = 不区分品牌）
 */
@Composable
private fun IngredientEditDialog(
    title: String,
    initial: Ingredient?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var brand by remember { mutableStateOf(initial?.supplier ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var price by remember {
        mutableStateOf(
            initial?.takeIf { it.unitPrice > 0 }?.let { "%.2f".format(it.unitPrice) } ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("原料名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("品牌（可选）") },
                    placeholder = { Text("如：雀巢 / 太古，留空=不区分品牌") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类（可选）") },
                    placeholder = { Text("如：水果 / 糖类 / 添加剂") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("单价 (元/kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), brand.trim(), category.trim(), price.toDoubleOrNull() ?: 0.0)
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
