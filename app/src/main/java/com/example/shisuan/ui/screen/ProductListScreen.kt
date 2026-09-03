package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.shisuan.data.database.Product
import com.example.shisuan.ui.animation.entranceAnimation
import com.example.shisuan.ui.animation.pressScale
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.theme.PrimaryRed
import com.example.shisuan.ui.viewModel.ProductViewModel

/**
 * 产品列表页 - 新的主页
 * 显示所有产品（草莓酱、蓝莓酱等）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    var showNewProductDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("食算", style = MaterialTheme.typography.headlineLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewProductDialog = true },
                containerColor = PrimaryRed
            ) {
                Icon(Icons.Default.Add, "新建产品", tint = Color.White)
            }
        }
    ) { padding ->
        if (products.isEmpty()) {
            EmptyState("📦", "还没有产品，点 ＋ 开始添加")
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
            onSave = { name, category, desc ->
                viewModel.saveProduct(name, category, desc)
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 产品图标（占位符）
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🍓", fontSize = 32.sp)
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
                if (product.category.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        product.category,
                        fontSize = 13.sp,
                        color = Color(0xFF6B6B6B)
                    )
                }
            }
            
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 新建产品对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建产品") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                onClick = { if (name.isNotBlank()) onSave(name, category, description) },
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
