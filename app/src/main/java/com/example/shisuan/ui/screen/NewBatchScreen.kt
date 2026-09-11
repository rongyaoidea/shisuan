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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shisuan.data.database.BatchIngredient
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.ui.components.EmptyState
import com.example.shisuan.ui.components.QuickChipsRow
import com.example.shisuan.ui.components.SliderNumberField
import com.example.shisuan.ui.components.StepperNumberField
import com.example.shisuan.ui.components.TextChipsRow
import com.example.shisuan.ui.components.formatNumber
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
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    val ingredients by viewModel.ingredients.collectAsStateWithLifecycle()
    val allIngredients by viewModel.allIngredients.collectAsStateWithLifecycle()
    val totalMaterialCost by viewModel.totalMaterialCost.collectAsStateWithLifecycle()
    val totalProcessingCost by viewModel.totalProcessingCost.collectAsStateWithLifecycle()
    val ocrScanning by viewModel.ocrScanning.collectAsStateWithLifecycle()
    val batchNamePreview by viewModel.batchNamePreview.collectAsStateWithLifecycle()
    val errorMessage by viewModel.error.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()

    // 表单状态来自 ViewModel，配置变更后仍可恢复
    val sampleWeight by viewModel.sampleWeight.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val batchDateMillis by viewModel.batchDateMillis.collectAsStateWithLifecycle()
    val canSubmit by viewModel.canSubmit.collectAsStateWithLifecycle()
    val packagingCost by viewModel.packagingCost.collectAsStateWithLifecycle()
    val laborCost by viewModel.laborCost.collectAsStateWithLifecycle()
    val overheadCost by viewModel.overheadCost.collectAsStateWithLifecycle()
    val yieldRate by viewModel.yieldRate.collectAsStateWithLifecycle()

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
    // 文件 IO 移到 Dispatchers.IO，避免 listFiles/delete 阻塞主线程导致掉帧
    val scope = rememberCoroutineScope()
    val openCamera: () -> Unit = {
        scope.launch {
            val dir = withContext(Dispatchers.IO) {
                val d = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
                    .firstOrNull()
                if (d != null) {
                    // 固定文件名复用：每次拍照覆盖同一文件，不再累积占用存储；
                    // 顺带清理旧版本「时间戳文件名」策略遗留的图片
                    d.listFiles { f -> f.name.startsWith("ocr_") && f.name != "ocr_capture.jpg" }
                        ?.forEach { it.delete() }
                }
                d
            }
            if (dir != null) {
                val file = File(dir, "ocr_capture.jpg")
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
                    // 样品重量：加减步进 + 常用重量一键填入，键盘输入保留为精确通道
                    StepperNumberField(
                        value = sampleWeight,
                        onValueChange = { viewModel.onSampleWeightChange(it) },
                        label = "样品重量 (g) *",
                        step = 10.0,
                        placeholder = "如 1000"
                    )
                    QuickChipsRow(
                        options = listOf(100.0, 250.0, 500.0, 1000.0, 2000.0),
                        currentText = sampleWeight,
                        onPick = { viewModel.onSampleWeightChange(formatNumber(it, 0)) },
                        places = 0,
                        suffix = "g"
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { viewModel.onNoteChange(it) },
                        label = { Text("备注（可选）") },
                        placeholder = { Text("如：本次试产改用新供应商草莓") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    // 出品率：滑条 50~100% 粗调 + 快捷标签，熬煮蒸发使成品少于投料，
                    // 不折算会低估吨价
                    SliderNumberField(
                        value = yieldRate,
                        onValueChange = { viewModel.onYieldRateChange(it) },
                        label = "出品率 %（可选，拖动滑条或输入）",
                        placeholder = "如 85：投料 1000g 出成品 850g",
                        range = 50f..100f,
                        increment = 1f,
                        places = 1,
                        suffix = "%"
                    )
                    QuickChipsRow(
                        options = listOf(80.0, 85.0, 90.0, 95.0),
                        currentText = yieldRate,
                        onPick = { viewModel.onYieldRateChange(formatNumber(it, 0)) },
                        places = 0,
                        suffix = "%"
                    )
                    val weightForPreview = sampleWeight.toDoubleOrNull() ?: 0.0
                    val yieldPreview = yieldRate.toDoubleOrNull() ?: 0.0
                    if (weightForPreview > 0 && yieldPreview in 0.0001..100.0) {
                        Text(
                            "投料 ${WeightFormatter.format(weightForPreview)} × ${"%.1f".format(Locale.CHINA, yieldPreview)}% ≈ 成品 ${WeightFormatter.format(weightForPreview * yieldPreview / 100)}，成本将按成品重量折算",
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
                        StepperNumberField(
                            value = packagingCost,
                            onValueChange = { viewModel.onPackagingCostChange(it) },
                            label = "包材：瓶/盖/标签/外箱",
                            step = 1.0,
                            suffix = "元"
                        )
                        Spacer(Modifier.height(8.dp))
                        StepperNumberField(
                            value = laborCost,
                            onValueChange = { viewModel.onLaborCostChange(it) },
                            label = "人工",
                            step = 1.0,
                            suffix = "元"
                        )
                        Spacer(Modifier.height(8.dp))
                        StepperNumberField(
                            value = overheadCost,
                            onValueChange = { viewModel.onOverheadCostChange(it) },
                            label = "水电蒸汽 / 折旧 / 其他",
                            step = 1.0,
                            suffix = "元"
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
                            "¥%,.2f".format(Locale.CHINA, totalMaterialCost),
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
                                "¥%,.2f".format(Locale.CHINA, totalProcessingCost),
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
                            "¥%,.2f".format(Locale.CHINA, totalMaterialCost + totalProcessingCost),
                            fontWeight = FontWeight.Bold,
                            color = Rausch
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val date = batchDateStr ?: return@Button
                    // 保存由 ViewModel 在 viewModelScope 中完成，成功才导航返回；
                    // 失败时留在本页，Snackbar 提示原因（修复静默丢单）
                    if (isEdit) {
                        viewModel.updateBatch(batchDate = date) { ok -> if (ok) onNavigateBack() }
                    } else {
                        viewModel.saveBatch(productId = productId, batchDate = date) { ok ->
                            if (ok) onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canSubmit && !saving,
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Rausch,
                    disabledContainerColor = RauschDisabled
                )
            ) {
                Text(
                    if (saving) "保存中…" else if (isEdit) "保存修改" else "保存批次",
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
                    "${"%.4f".format(Locale.CHINA, ingredient.ratioPercent)}% · ${WeightFormatter.format(ingredient.weight)} × ¥${"%.2f".format(Locale.CHINA, ingredient.unitPrice)}/${ingredient.priceUnit.removePrefix("元/")}"
                } else {
                    "${WeightFormatter.format(ingredient.weight)} × ¥${"%.2f".format(Locale.CHINA, ingredient.unitPrice)}/${ingredient.priceUnit.removePrefix("元/")}"
                },
                fontSize = 12.sp,
                color = Foggy
            )
        }
        Text(
            "¥${"%.2f".format(Locale.CHINA, ingredient.totalCost)}",
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

// 注：IngredientPickerSheet 已拆至同包 IngredientPickerSheet.kt（原同文件超 300 行）。
