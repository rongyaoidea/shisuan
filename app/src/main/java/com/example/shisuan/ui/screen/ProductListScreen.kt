package com.example.shisuan.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.shisuan.data.database.Product
import com.example.shisuan.ui.animation.entranceAnimation
import com.example.shisuan.ui.animation.pressScale
import com.example.shisuan.ui.icons.Add
import com.example.shisuan.ui.icons.Flask
import com.example.shisuan.ui.icons.Jar
import com.example.shisuan.ui.icons.Package
import com.example.shisuan.ui.icons.categoryIcon
import com.example.shisuan.ui.theme.*
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.components.QuickChipsRow
import com.example.shisuan.ui.components.SliderNumberField
import com.example.shisuan.ui.components.StepperNumberField
import com.example.shisuan.ui.components.TextChipsRow
import com.example.shisuan.ui.components.formatNumber
import com.example.shisuan.ui.viewModel.BackupViewModel
import com.example.shisuan.ui.viewModel.ProductViewModel
import com.example.shisuan.utils.WeightFormatter
import java.time.LocalDate

/**
 * 产品列表页 - 主页
 * 显示所有产品（草莓酱、蓝莓酱等），顶栏提供配料库与备份/恢复入口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToIngredientLibrary: () -> Unit = {},
    viewModel: ProductViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val errorMessage by viewModel.error.collectAsState()
    var showNewProductDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf(false) } // 二次确认导入

    // 备份操作提示（成功与失败共用）
    val backupMessage by backupViewModel.message.collectAsState()
    val backupError by backupViewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage, backupMessage, backupError) {
        (backupMessage ?: backupError ?: errorMessage)?.let {
            snackbarHostState.showSnackbar(it)
            backupViewModel.consumeMessage()
            backupViewModel.consumeError()
            viewModel.consumeError()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? -> uri?.let { backupViewModel.exportTo(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { backupViewModel.importFrom(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("食算", style = MaterialTheme.typography.headlineLarge) },
                actions = {
                    TextButton(onClick = { showBackupDialog = true }) {
                        Text("备份", color = Rausch)
                    }
                    IconButton(onClick = onNavigateToIngredientLibrary) {
                        Icon(Flask, "配料库")
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
            onSave = { name, category, desc, pkgBox, pkgGram, marginRate ->
                viewModel.saveProduct(
                    name = name,
                    category = category,
                    description = desc,
                    packagesPerBox = pkgBox,
                    weightPerPackageGram = pkgGram,
                    targetMarginRate = marginRate
                )
                showNewProductDialog = false
            }
        )
    }

    // 备份 / 恢复入口对话框
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("数据备份与恢复") },
            text = {
                Text(
                    "所有数据仅保存在本机。建议定期导出备份，避免手机丢失或更换时历史批次记录丢失。\n\n" +
                        "恢复备份会覆盖现有全部数据。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackupDialog = false
                        exportLauncher.launch("shisuan_backup_${LocalDate.now()}.db")
                    }
                ) { Text("导出备份", color = Rausch) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBackupDialog = false
                        pendingImport = true
                    }
                ) { Text("恢复数据", color = DangerRed) }
            }
        )
    }

    // 恢复前二次确认：数据覆盖不可逆
    if (pendingImport) {
        AlertDialog(
            onDismissRequest = { pendingImport = false },
            title = { Text("确认恢复") },
            text = { Text("恢复备份将覆盖当前全部产品与批次数据，且无法撤销。确定继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImport = false
                        importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    }
                ) { Text("选择备份文件", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = false }) { Text("取消") }
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
 * 新建产品对话框 - 含包装规格与目标毛利率
 * 包装规格输入：每箱包数 + 每包克数，每箱克数自动计算
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var pkgBox by remember { mutableStateOf("20") }
    var pkgGram by remember { mutableStateOf("250") }
    var marginRate by remember { mutableStateOf("") }

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
                TextChipsRow(
                    options = listOf("果酱", "酱料", "调味品", "罐头", "烘焙", "其他"),
                    current = category,
                    onPick = { category = it }
                )
                Text("包装规格", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Foggy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StepperNumberField(
                        value = pkgBox,
                        onValueChange = { pkgBox = it },
                        label = "每箱包数",
                        step = 1.0,
                        min = 1.0,
                        inline = true,
                        modifier = Modifier.weight(1f)
                    )
                    StepperNumberField(
                        value = pkgGram,
                        onValueChange = { pkgGram = it },
                        label = "每包克数 (g)",
                        step = 10.0,
                        min = 1.0,
                        inline = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                QuickChipsRow(
                    options = listOf(100.0, 200.0, 250.0, 500.0, 1000.0),
                    currentText = pkgGram,
                    onPick = { pkgGram = formatNumber(it, 0) },
                    places = 0,
                    suffix = "g"
                )
                Text(
                    "每箱克数 = $pkgBoxNum × $pkgGramNum = ${WeightFormatter.format(boxGramComputed)}（自动计算）",
                    fontSize = 12.sp,
                    color = Foggy
                )
                SliderNumberField(
                    value = marginRate,
                    onValueChange = { marginRate = it },
                    label = "目标毛利率 %（可选）",
                    placeholder = "拖动滑条或输入，如 30",
                    range = 0f..60f,
                    increment = 1f,
                    places = 0,
                    suffix = "%"
                )
                QuickChipsRow(
                    options = listOf(20.0, 30.0, 40.0, 50.0),
                    currentText = marginRate,
                    onPick = { marginRate = formatNumber(it, 0) },
                    places = 0,
                    suffix = "%"
                )
                Text(
                    "设置后批次卡将显示建议出厂价",
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
                            pkgGram.toDoubleOrNull() ?: 250.0,
                            marginRate.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.0
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
