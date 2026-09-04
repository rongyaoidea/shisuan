package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.shisuan.data.database.Product
import com.example.shisuan.ui.animation.entranceAnimation
import com.example.shisuan.ui.animation.pressScale
import com.example.shisuan.ui.icons.Add
import com.example.shisuan.ui.icons.ArrowBack
import com.example.shisuan.ui.icons.Camera
import com.example.shisuan.ui.icons.Delete
import com.example.shisuan.ui.icons.Edit
import com.example.shisuan.ui.icons.Flask
import com.example.shisuan.ui.icons.Gallery
import com.example.shisuan.ui.icons.Jar
import com.example.shisuan.ui.icons.Package
import com.example.shisuan.ui.icons.Scale
import com.example.shisuan.ui.icons.categoryIcon
import com.example.shisuan.ui.theme.*
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.theme.Rausch
import com.example.shisuan.ui.viewModel.ProductViewModel

/**
 * 产品列表页 - 新的主页
 * 显示所有产品（草莓酱、蓝莓酱等）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToIngredientLibrary: () -> Unit = {},
    viewModel: ProductViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    var showNewProductDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("食算", style = MaterialTheme.typography.headlineLarge) },
                actions = {
                    IconButton(onClick = onNavigateToIngredientLibrary) {
                        Icon(Flask, "配料库")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewProductDialog = true },
                containerColor = Rausch
            ) {
                Icon(Add, "新建产品", tint = Color.White)
            }
        }
    ) { padding ->
        if (products.isEmpty()) {
            EmptyState(Package, "还没有产品，点 ＋ 开始添加")
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(products, key = { _, p -> p.id }) { index, product ->
                    ProductCard(
                        product = product,
                        onClick = { onNavigateToDetail(product.id) },
                        index = index
                    )
                }
            }
        }
    }
    
    if (showNewProductDialog) {
        NewProductDialog(
            onDismiss = { showNewProductDialog = false },
            onSave = { name, category, desc, pkgBox, pkgGram ->
                viewModel.saveProduct(
                    name = name,
                    category = category,
                    description = desc,
                    packagesPerBox = pkgBox,
                    weightPerPackageGram = pkgGram
                )
                showNewProductDialog = false
            }
        )
    }
}

/**
 * 产品卡片
 */
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    index: Int = 0
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pressScale(onClick = onClick)
            .entranceAnimation(index = index),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 产品图标（按分类匹配烘焙行业图标）
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    categoryIcon(product.category),
                    null,
                    tint = Foggy,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
                if (product.category.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        product.category,
                        fontSize = 13.sp,
                        color = Foggy
                    )
                }
            }
            
            Icon(
                imageVector = Add,
                contentDescription = null,
                tint = SoftBg,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 新建产品对话框 - 含包装规格设置
 * 包装规格输入：每箱包数 + 每包克数，每箱克数自动计算
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var pkgBox by remember { mutableStateOf("20") }
    var pkgGram by remember { mutableStateOf("250") }

    val pkgBoxNum = pkgBox.toIntOrNull() ?: 0
    val pkgGramNum = pkgGram.toDoubleOrNull() ?: 0.0
    val boxGramComputed = pkgBoxNum * pkgGramNum

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建产品") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("产品名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类（可选）") },
                    placeholder = { Text("如：果酱、酱料") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("包装规格", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Foggy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pkgBox,
                        onValueChange = { pkgBox = it },
                        label = { Text("每箱包数") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pkgGram,
                        onValueChange = { pkgGram = it },
                        label = { Text("每包克数 (g)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "每箱克数 = $pkgBoxNum × $pkgGramNum = ${"%.0f".format(boxGramComputed)} g（自动计算）",
                    fontSize = 12.sp,
                    color = Foggy
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name, category, description,
                            pkgBox.toIntOrNull() ?: 20,
                            pkgGram.toDoubleOrNull() ?: 250.0
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
