package com.example.shisuan.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.icons.Add
import com.example.shisuan.ui.icons.ArrowBack
import com.example.shisuan.ui.icons.Calendar
import com.example.shisuan.ui.icons.Camera
import com.example.shisuan.ui.icons.Delete
import com.example.shisuan.ui.icons.Edit
import com.example.shisuan.ui.icons.Flask
import com.example.shisuan.ui.icons.Gallery
import com.example.shisuan.ui.icons.Jar
import com.example.shisuan.ui.icons.Package
import com.example.shisuan.ui.icons.Scale
import com.example.shisuan.ui.theme.*
import com.example.shisuan.ui.viewModel.NewBatchViewModel
import com.example.shisuan.utils.CostCalculator
import java.io.File

/**
 * 新建/编辑批次页 - 配置原料配料
 *
 * @param editBatchId 非空表示编辑已有批次
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBatchScreen(
    productId: Long,
    editBatchId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: NewBatchViewModel = hiltViewModel()
) {
    val isEdit = editBatchId != null

    var batchDateMillis by remember { mutableStateOf<Long?>(null) } // 批次日期（UTC 毫秒）
    var showDatePicker by remember { mutableStateOf(false) }
    var sampleWeight by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showIngredientPicker by remember { mutableStateOf(false) }
    var showOcrSource by remember { mutableStateOf(false) }

    val ingredients by viewModel.ingredients.collectAsState()
    val allIngredients by viewModel.allIngredients.collectAsState()
    val totalMaterialCost by viewModel.totalMaterialCost.collectAsState()
    val editBatchInfo by viewModel.editBatchInfo.collectAsState()
    val ocrScanning by viewModel.ocrScanning.collectAsState()
    val batchNamePreview by viewModel.batchNamePreview.collectAsState()

    // 编辑模式：加载批次数据到草稿态
    LaunchedEffect(editBatchId) {
        if (editBatchId != null) {
            viewModel.loadBatchForEdit(editBatchId)
        }
    }

    // 批次信息加载完成后预填表单（日期从批次名解析，批次名格式 yyyy-MM-dd-NN）
    LaunchedEffect(editBatchInfo) {
        val b = editBatchInfo
        if (b != null) {
            sampleWeight = b.sampleWeightGram.toString()
            note = b.note
            batchDateMillis = parseDateMillis(b.batchName) ?: toUtcDateMillis(b.createdAt)
        }
    }

    // 日期变化时刷新自动生成的批次名
    val batchDateStr = batchDateMillis?.let(::formatDateMillis)
    LaunchedEffect(batchDateStr) {
        if (!batchDateStr.isNullOrBlank()) {
            viewModel.refreshBatchNamePreview(productId, batchDateStr)
        }
    }

    // OCR：相机拍照 / 相册选图 → 识别配料表
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // 先声明 runOcr（被 launcher 回调引用）
    val runOcr: (Uri) -> Unit = { uri ->
        viewModel.recognizeIngredients(uri)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraUri?.let { runOcr(it) }
    }
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) runOcr(uri)
    }

    // 最后声明 openCamera（引用 takePictureLauncher）
    val openCamera: () -> Unit = {
        val dir = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
            .firstOrNull()
        if (dir != null) {
            val file = File(dir, "ocr_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraUri = uri
            // 先检查是否有相机应用，避免 ActivityNotFoundException 闪退
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (captureIntent.resolveActivity(context.packageManager) != null) {
                try {
                    takePictureLauncher.launch(uri)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "无法打开相机", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "未检测到相机应用", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "无法访问存储目录", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑批次" else "新建批次", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(ArrowBack, "返回")
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
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 批次日期（日历选择），批次名自动生成：日期+序号
                    OutlinedTextField(
                        value = batchDateStr ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("批次日期 *") },
                        placeholder = { Text("点击选择日期") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Calendar, "选择日期", tint = Foggy)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!batchNamePreview.isNullOrBlank()) {
                        Text(
                            "批次编号：$batchNamePreview（自动生成）",
                            fontSize = 12.sp,
                            color = Foggy
                        )
                    }
                    OutlinedTextField(
                        value = sampleWeight,
                        onValueChange = { sampleWeight = it },
                        label = { Text("样品重量 (g) *") },
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
                EmptyState(Flask, "还没有添加原料，点下方按钮添加")
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
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
                Icon(Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("添加原料")
            }

            // OCR 识别配料表
            if (ocrScanning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = RauschDisabled)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Rausch
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("正在识别配料表…", color = RauschPressed, fontSize = 14.sp)
                    }
                }
            }

            // 底部汇总和保存
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("原料成本", color = Foggy)
                        Text(
                            "¥%,.2f".format(totalMaterialCost),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("总成本", fontWeight = FontWeight.Bold)
                        Text(
                            "¥%,.2f".format(totalMaterialCost),
                            fontWeight = FontWeight.Bold,
                            color = Rausch
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val weight = sampleWeight.toDoubleOrNull() ?: return@Button
                    if (batchDateStr.isNullOrBlank()) return@Button
                    if (weight > 0) {
                        if (isEdit) {
                            viewModel.updateBatch(
                                batchDate = batchDateStr!!,
                                sampleWeight = weight,
                                note = note
                            )
                        } else {
                            viewModel.saveBatch(
                                productId = productId,
                                batchDate = batchDateStr!!,
                                sampleWeight = weight,
                                note = note
                            )
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !batchDateStr.isNullOrBlank() && (sampleWeight.toDoubleOrNull() ?: 0.0) > 0,
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Rausch,
                    disabledContainerColor = RauschDisabled
                )
            ) {
                Text(
                    if (isEdit) "保存修改" else "保存批次",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
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
            },
            onOcrScan = {
                showOcrSource = true
            }
        )
    }

    // OCR 图片来源选择对话框
    if (showOcrSource) {
        AlertDialog(
            onDismissRequest = { showOcrSource = false },
            title = { Text("识别配料表") },
            text = { Text("选择图片来源，识别配料表上的文字并快速创建配料") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOcrSource = false
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) { Text("从相册选择") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOcrSource = false
                        openCamera()
                    }
                ) { Text("拍照") }
            }
        )
    }

    // 批次日期选择器
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = batchDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { batchDateMillis = it }
                        showDatePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ─────────── 批次日期工具 ───────────

/** UTC 毫秒 → yyyy-MM-dd（DatePicker 使用 UTC 午夜） */
private fun formatDateMillis(utcMillis: Long): String =
    java.time.Instant.ofEpochMilli(utcMillis)
        .atZone(java.time.ZoneOffset.UTC)
        .toLocalDate()
        .toString()

/** 从批次名解析日期（yyyy-MM-dd-NN），失败返回 null */
private fun parseDateMillis(batchName: String): Long? {
    if (batchName.length < 10) return null
    val datePart = batchName.take(10)
    return try {
        java.time.LocalDate.parse(datePart)
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

/** epoch 毫秒（本地时区）→ 该日期 UTC 午夜的毫秒 */
private fun toUtcDateMillis(epochMillis: Long): Long {
    val localDate = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return localDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
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
                color = Ink
            )
            Text(
                "${"%.2f".format(ingredient.weight)}g × ¥${"%.2f".format(ingredient.unitPrice)}/kg",
                fontSize = 12.sp,
                color = Foggy
            )
        }
        Text(
            "¥${"%.2f".format(ingredient.totalCost)}",
            fontWeight = FontWeight.SemiBold,
            color = Ink
        )
        IconButton(onClick = onDelete) {
            Icon(
                Delete,
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
 * 支持从原料库选择、快速添加新原料入库、OCR 拍照识别配料表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientPickerSheet(
    ingredients: List<Ingredient>,
    onDismiss: () -> Unit,
    onPick: (Ingredient, Double, Double) -> Unit,
    onCreateIngredient: (String, String, Double) -> Unit = { _, _, _ -> },
    onOcrScan: () -> Unit = {}
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

            // OCR 识别入口
            OutlinedButton(
                onClick = onOcrScan,
                modifier = Modifier.fillMaxWidth(),
                shape = PillShape
            ) {
                Icon(Camera, null, modifier = Modifier.size(18.dp))
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
                        colors = ButtonDefaults.buttonColors(containerColor = Rausch)
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
                                        if (ingredient.category.isNotEmpty()) {
                                            Text(
                                                ingredient.category,
                                                fontSize = 11.sp,
                                                color = Foggy
                                            )
                                        }
                                        if (ingredient.unitPrice > 0) {
                                            Text(
                                                " · ¥${"%.2f".format(ingredient.unitPrice)}/kg",
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
                OutlinedButton(
                    onClick = { showQuickAdd = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Add, null, modifier = Modifier.size(16.dp))
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
                        colors = ButtonDefaults.buttonColors(containerColor = Rausch)
                    ) {
                        Text("添加")
                    }
                }
            }
        }
    }
}
