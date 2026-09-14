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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.components.StepperNumberField
import com.example.shisuan.ui.components.TextChipsRow
import com.example.shisuan.ui.icons.Add
import com.example.shisuan.ui.icons.ArrowBack
import com.example.shisuan.ui.icons.Delete
import com.example.shisuan.ui.icons.Edit
import com.example.shisuan.ui.icons.Flask
import com.example.shisuan.ui.theme.*
import com.example.shisuan.ui.viewModel.IngredientLibraryViewModel
import java.util.Locale

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
    val ingredients by viewModel.ingredients.collectAsStateWithLifecycle()
    val errorMessage by viewModel.error.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Ingredient?>(null) }
    var editingUseCount by remember { mutableStateOf(0) }
    var pendingDelete by remember { mutableStateOf<Ingredient?>(null) }
    // 分类筛选：选项来自已有数据的分类，切换后离开页面即重置
    var categoryFilter by remember { mutableStateOf("全部") }

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
        // 分类选项与过滤结果：派生自数据，分类消失时回落到全部
        val categories = remember(ingredients) {
            listOf("全部") + ingredients.map { it.ingredient.category }
                .filter { it.isNotEmpty() }.distinct().sorted()
        }
        val effectiveFilter = categoryFilter.takeIf { it in categories } ?: "全部"
        val visibleIngredients = remember(ingredients, effectiveFilter) {
            if (effectiveFilter == "全部") ingredients
            else ingredients.filter { it.ingredient.category == effectiveFilter }
        }
        if (ingredients.isEmpty()) {
            EmptyState(Flask, "配料库为空，点 ＋ 添加第一种原料")
        } else if (visibleIngredients.isEmpty()) {
            EmptyState(Flask, "该分类下暂无原料")
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 仅多于「全部」一个选项时展示筛选行
                if (categories.size > 1) {
                    item(key = "category-filter") {
                        TextChipsRow(
                            options = categories,
                            current = effectiveFilter,
                            onPick = { categoryFilter = it.ifEmpty { "全部" } },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                items(visibleIngredients, key = { it.ingredient.id }) { row ->
                    val ingredient = row.ingredient
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
                                        append("%.2f".format(Locale.CHINA, ingredient.unitPrice))
                                        append("/kg")
                                    },
                                    fontSize = 12.sp,
                                    color = Foggy
                                )
                            }
                            // 使用频次：列表按频次降序，徽章解释排序依据
                            if (row.useCount > 0) {
                                Text(
                                    "用于 ${row.useCount} 个批次",
                                    fontSize = 11.sp,
                                    color = Foggy,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            IconButton(onClick = {
                                editing = ingredient
                                editingUseCount = row.useCount
                            }) {
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
            useCount = editingUseCount,
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
    onSave: (String, String, String, Double) -> Unit,
    useCount: Int = 0
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var brand by remember { mutableStateOf(initial?.supplier ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var price by remember {
        mutableStateOf(
            initial?.takeIf { it.unitPrice > 0 }?.let { "%.2f".format(Locale.CHINA, it.unitPrice) } ?: ""
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
                TextChipsRow(
                    options = listOf("水果", "蔬菜", "蛋类", "乳制品", "糖类", "粮油", "添加剂", "包材", "其他"),
                    current = category,
                    onPick = { category = it }
                )
                StepperNumberField(
                    value = price,
                    onValueChange = { price = it },
                    label = "参考单价",
                    step = 0.5,
                    suffix = "元/kg",
                    placeholder = "如 12.5"
                )
                // 改价影响说明：历史批次保留旧价，断点在产品详情页以「历史价」标出
                if (initial != null && useCount > 0) {
                    Text(
                        "改价仅影响新建批次；已有 $useCount 个批次保留历史价，" +
                            "产品详情页会标注“历史价”。",
                        fontSize = 12.sp,
                        color = Foggy
                    )
                }
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
