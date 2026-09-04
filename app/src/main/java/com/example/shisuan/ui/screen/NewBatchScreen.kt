package com.example.shisuan.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import com.example.shisuan.utils.WeightFormatter
import com.example.shisuan.utils.formatDateMillis
import java.io.File

/**
 * 新建/编辑批次页 - 配置原料配料
 *
 * 表单输入本身保存在 [NewBatchViewModel] 中（而非此处的 remember），
 * 因此旋转屏幕等配置变更不会丢失已填写的内容。
 *
 * @param editBatchId 非空表示编辑已有批次
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBatchScreen(
    productId: Long,
    editBatchId: Long? = null,
    copyFromBatchId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: NewBatchViewModel = hiltViewModel()
) {
    val isEdit = editBatchId != null

    var showDatePicker by remember { mutableStateOf(false) }
    var showIngredientPicker by remember { mutableStateOf(false) }
    var showOcrSource by remember { mutableStateOf(false) }
    var showProcessingCost by remember { mutableStateOf(false) } // 加工费折叠区

    val ingredients by viewModel.ingredients.collectAsState()
    val allIngredients by viewModel.allIngredients.collectAsState()
    val totalMaterialCost by viewModel.totalMaterialCost.collectAsState()
    val totalProcessingCost by viewModel.totalProcessingCost.collectAsState()
    val ocrScanning by viewModel.ocrScanning.collectAsState()
    val batchNamePreview by viewModel.batchNamePreview.collectAsState()
    val errorMessage by viewModel.error.collectAsState()

    // 表单状态来自 ViewModel，配置变更后仍可恢复
    val sampleWeight by viewModel.sampleWeight.collectAsState()
    val note by viewModel.note.collectAsState()
    val batchDateMillis by viewModel.batchDateMillis.collectAsState()
    val canSubmit by viewModel.canSubmit.collectAsState()
    val packagingCost by viewModel.packagingCost.collectAsState()
    val laborCost by viewModel.laborCost.collectAsState()
    val overheadCost by viewModel.overheadCost.collectAsState()
    val yieldRate by viewModel.yieldRate.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    // 编辑模式：加载批次数据到草稿态（含表单回填）
    LaunchedEffect(editBatchId) {
        if (editBatchId != null) {
            viewModel.loadBatchForEdit(editBatchId)
        }
    }

    // 复制模式：以现有批次为模板预填表单（配料/加工费/出品率/备注带入，日期重置为今天）
    LaunchedEffect(copyFromBatchId) {
        if (copyFromBatchId != null) {
            viewModel.copyFromTemplate(copyFromBatchId)
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                        onValueChange = { viewModel.onSampleWeightChange(it) },
                        label = { Text("样品重量 (g) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { viewModel.onNoteChange(it) },
                        label = { Text("备注（可选）") },
                        placeholder = { Text("如：本次试产改用新供应商草莓") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    // 出品率：熬煮蒸发使成品少于投料，不折算会低估吨价
                    OutlinedTextField(
                        value = yieldRate,
                        onValueChange = { viewModel.onYieldRateChange(it) },
                        label = { Text("出品率 %（可选）") },
                        placeholder = { Text("如 85：投料 1000g 出成品 850g") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val weightForPreview = sampleWeight.toDoubleOrNull() ?: 0.0
                    val yieldPreview = yieldRate.toDoubleOrNull() ?: 0.0
                    if (weightForPreview > 0 && yieldPreview in 0.0001..100.0) {
                        Text(
                            "投料 ${WeightFormatter.format(weightForPreview)} × ${"%.1f".format(yieldPreview)}% ≈ 成品 ${WeightFormatter.format(weightForPreview * yieldPreview / 100)}，成本将按成品重量折算",
                            fontSize = 12.sp,
                            color = Foggy
                        )
                    }
                }
            }

            // 加工费（制造费用）：包材 / 人工 / 水电折旧，默认折叠不增加录入负担
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProcessingCost = !showProcessingCost },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "加工费（包材 / 人工 / 水电折旧）",
                            fontWeight = FontWeight.Medium,
                            color = Ink
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (showProcessingCost) "收起" else "展开填写",
                            fontSize = 13.sp,
                            color = Rausch
                        )
                    }
                    if (showProcessingCost) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = packagingCost,
                            onValueChange = { viewModel.onPackagingCostChange(it) },
                            label = { Text("包材：瓶/盖/标签/外箱 (元)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = laborCost,
                            onValueChange = { viewModel.onLaborCostChange(it) },
                            label = { Text("人工 (元)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = overheadCost,
                            onValueChange = { viewModel.onOverheadCostChange(it) },
                            label = { Text("水电蒸汽 / 折旧 / 其他 (元)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                                onDelete = { viewModel.removeIngredientAt(index) },
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
                    if (totalProcessingCost > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("加工费", color = Foggy)
                            Text(
                                "¥%,.2f".format(totalProcessingCost),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("总成本", fontWeight = FontWeight.Bold)
                        Text(
                            "¥%,.2f".format(totalMaterialCost + totalProcessingCost),
                            fontWeight = FontWeight.Bold,
                            color = Rausch
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val date = batchDateStr ?: return@Button
                    if (isEdit) {
                        viewModel.updateBatch(batchDate = date)
                    } else {
                        viewModel.saveBatch(productId = productId, batchDate = date)
                    }
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canSubmit,
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
            sampleWeightGram = sampleWeight.toDoubleOrNull() ?: 0.0, // 百分比换算基准
            onDismiss = { showIngredientPicker = false },
            onPick = { ingredient, weight, ratioPercent ->
                // 单价与小计较由 ViewModel 内的 BatchIngredient.create 统一换算
                viewModel.addIngredient(ingredient, weight, ratioPercent)
                showIngredientPicker = false
            },
            onCreateIngredient = { name, brand, category, price ->
                viewModel.saveIngredient(name, brand, category, price)
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
                        datePickerState.selectedDateMillis?.let { viewModel.onBatchDateChange(it) }
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
                buildString {
                    append(ingredient.ingredientName)
                    if (ingredient.ingredientSupplier.isNotEmpty()) {
                        append("（")
                        append(ingredient.ingredientSupplier)
                        append("）")
                    }
                },
                fontWeight = FontWeight.Medium,
                color = Ink
            )
            Text(
                if (ingredient.ratioPercent != null) {
                    "${"%.4f".format(ingredient.ratioPercent)}% · ${WeightFormatter.format(ingredient.weight)} × ¥${"%.2f".format(ingredient.unitPrice)}/${ingredient.priceUnit.removePrefix("元/")}"
                } else {
                    "${WeightFormatter.format(ingredient.weight)} × ¥${"%.2f".format(ingredient.unitPrice)}/${ingredient.priceUnit.removePrefix("元/")}"
                },
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
 *
 * 用量支持两种模式：
 * - 克重 (g)：直接填克重
 * - 比例 (%)：填占样品重量的百分比（香精/添加剂微量场景），按样品重量换算克重
 * 单价一律取原料库库存价，不再重复填写。
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
                                                " · ¥${"%.2f".format(ingredient.unitPrice)}/${ingredient.priceUnit.removePrefix("元/")}",
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
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("占样品比例 (%)") },
                            placeholder = { Text("如 0.05（万分之五）") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
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
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("用量 (g)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // 单价只读：取原料库库存价，避免重复填写
                    Text(
                        if (ing.unitPrice > 0)
                            "单价（库存）：¥${"%.2f".format(ing.unitPrice)}/${ing.priceUnit.removePrefix("元/")}"
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
