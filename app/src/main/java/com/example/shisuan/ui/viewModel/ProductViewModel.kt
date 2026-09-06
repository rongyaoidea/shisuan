package com.example.shisuan.ui.viewModel

import androidx.lifecycle.viewModelScope
import com.example.shisuan.core.ocr.OcrAnalyzer
import com.example.shisuan.data.database.*
import com.example.shisuan.data.repository.CostRepository
import com.example.shisuan.data.repository.IngredientUpsert
import com.example.shisuan.domain.model.CostResult
import com.example.shisuan.utils.BatchSnapshotCodec
import com.example.shisuan.utils.CostCalculator
import com.example.shisuan.utils.parseBatchDateMillis
import com.example.shisuan.utils.toUtcDateMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 产品列表 ViewModel - Hilt 注入
 */
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repo: CostRepository
) : BaseViewModel() {

    val products: StateFlow<List<Product>> =
        repo.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveProduct(
        name: String, category: String = "", description: String = "",
        packagesPerBox: Int = 20, weightPerPackageGram: Double = 250.0,
        targetMarginRate: Double = 0.0
    ) {
        launchSafe {
            repo.saveProduct(
                Product(
                    name = name,
                    category = category,
                    description = description,
                    // 每箱克数 = 每箱包数 × 每包克数，无需用户手算
                    weightPerBoxGram = packagesPerBox * weightPerPackageGram,
                    packagesPerBox = packagesPerBox,
                    weightPerPackageGram = weightPerPackageGram,
                    targetMarginRate = targetMarginRate
                )
            )
        }
    }

    fun deleteProduct(product: Product) {
        launchSafe {
            repo.deleteProduct(product)
        }
    }
}

/**
 * 产品详情 ViewModel - 显示某产品的所有批次
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repo: CostRepository
) : BaseViewModel() {

    private val _productId = MutableStateFlow<Long?>(null)

    fun setProduct(id: Long) {
        _productId.value = id
    }

    val product: StateFlow<Product?> = _productId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repo.getProductById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 预计算：批次 + 成本结果
     *
     * 通过 [CostRepository.getBatchesWithIngredients] 一次性取回全部批次及其配料，
     * 避免在 Flow 变换中逐批次查库（N 批次会触发 2N 次查询）以及在流处理线程上挂起等待。
     *
     * 吨价先集中算一遍再取相邻值做差异对比，避免 prev 成本被重复计算 N-1 次。
     */
    val batchesWithCost: StateFlow<List<BatchWithCostUI>> =
        _productId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList<BatchWithCostUI>())
            else {
                combine(
                    repo.getBatchesWithIngredients(id),
                    repo.getProductById(id)
                ) { rows, prod ->
                    val boxGram = prod?.weightPerBoxGram ?: DEFAULT_WEIGHT_PER_BOX_GRAM
                    val pkgBox = prod?.packagesPerBox ?: DEFAULT_PACKAGES_PER_BOX
                    val marginRate = prod?.targetMarginRate ?: 0.0

                    // 一次遍历算出所有批次的吨价，供相邻批次差异对比复用
                    val tonCosts = rows.map { row ->
                        CostCalculator.calculate(
                            sampleWeightGram = row.batch.sampleWeightGram,
                            materialCost = row.ingredients.sumOf { it.totalCost },
                            processingCost = row.batch.processingCost,
                            weightPerBoxGram = boxGram,
                            packagesPerBox = pkgBox,
                            yieldRatePercent = row.batch.yieldRatePercent
                        ).unitCostPerTon
                    }

                    rows.mapIndexed { index, row ->
                        val materialCost = row.ingredients.sumOf { it.totalCost }
                        val processingCost = row.batch.processingCost
                        val result = CostCalculator.calculate(
                            sampleWeightGram = row.batch.sampleWeightGram,
                            materialCost = materialCost,
                            processingCost = processingCost,
                            weightPerBoxGram = boxGram,
                            packagesPerBox = pkgBox,
                            yieldRatePercent = row.batch.yieldRatePercent
                        )
                        BatchWithCostUI(
                            batch = row.batch,
                            ingredients = row.ingredients,
                            materialCost = materialCost,
                            processingCost = processingCost,
                            totalCost = materialCost + processingCost,
                            result = result,
                            differential = CostCalculator.calcDifferential(
                                currentTonCost = result.unitCostPerTon,
                                previousTonCost = tonCosts.getOrNull(index + 1)
                            ),
                            suggestedTonPrice = CostCalculator.suggestedTonPrice(
                                result.unitCostPerTon, marginRate
                            )
                        )
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteBatch(batch: BatchRecord) {
        launchSafe {
            repo.deleteBatch(batch)
        }
    }

    // ─────────── 版本历史（git 式时间线） ───────────

    private val _historyBatchId = MutableStateFlow<Long?>(null)

    /** 正在查看版本时间线的批次 id，null = 时间线关闭 */
    val historyBatchId: StateFlow<Long?> = _historyBatchId.asStateFlow()

    /**
     * 版本时间线（version 倒序，最新在前）。
     * 每条记录带唯一变更编号（内容指纹 digest），内容相同的保存不会产生重复提交。
     */
    val snapshots: StateFlow<List<BatchSnapshot>> = _historyBatchId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repo.getBatchSnapshots(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 当前批次内容的内容指纹。
     * 时间线的「当前」标记按它与各快照的 digest 匹配——恢复到旧版本后，
     * 「当前」应指向内容一致的旧版本，而不是版本号最大的那条。
     */
    val historyCurrentDigest: StateFlow<String?> =
        combine(_historyBatchId, batchesWithCost) { batchId, rows ->
            rows.firstOrNull { it.batch.id == batchId }?.let {
                BatchSnapshotCodec.digest(it.batch, it.ingredients)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun showHistory(batchId: Long) {
        _historyBatchId.value = batchId
    }

    fun dismissHistory() {
        _historyBatchId.value = null
    }

    // ─────────── 批次成果记录（口感/pH/糖度/评分） ───────────

    private val _outcomeBatchId = MutableStateFlow<Long?>(null)
    val outcomeBatchId: StateFlow<Long?> = _outcomeBatchId.asStateFlow()

    /** 正在记录成果的批次的已有成果，null = 从未记录 */
    val batchResult: StateFlow<BatchResult?> = _outcomeBatchId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repo.getBatchResult(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun showOutcome(batchId: Long) {
        _outcomeBatchId.value = batchId
    }

    fun dismissOutcome() {
        _outcomeBatchId.value = null
    }

    fun saveOutcome(result: BatchResult) {
        launchSafe {
            repo.saveOrUpdateResult(result)
            _outcomeBatchId.value = null
        }
    }

    /**
     * 恢复到指定版本。
     * 成功后关闭时间线，批次卡与成本趋势随 Room Flow 自动刷新。
     */
    fun restoreSnapshot(snapshot: BatchSnapshot) {
        launchSafe {
            if (repo.restoreSnapshot(snapshot)) {
                _historyBatchId.value = null
            } else {
                showError("快照数据已损坏，无法恢复")
            }
        }
    }

    // ─────────── 损耗分析（出品率维度） ───────────

    /**
     * 损耗分析：由 [batchesWithCost] 派生，无需额外查库。
     *
     * 总成本不随出品率变化，而吨价 ∝ 1/出品率（effectiveWeight = 投料 × 出品率），
     * 因此「损耗影响」与「恢复到最佳的节省」都是纯比例换算，不必重算成本。
     */
    val yieldAnalysis: StateFlow<YieldAnalysis?> = batchesWithCost.map { rows ->
        val recorded = rows.filter { (it.batch.yieldRatePercent ?: 0.0) > 0.0 }
        if (recorded.isEmpty()) return@map null

        // rows 按 createdAt DESC 排序，first 即最近批次
        val latest = recorded.first()
        val latestYield = latest.batch.yieldRatePercent!!
        val avg = recorded.sumOf { it.batch.yieldRatePercent!! } / recorded.size
        val best = recorded.maxBy { it.batch.yieldRatePercent!! }

        val savingPerTon = if (best.batch.id != latest.batch.id) {
            latest.result.unitCostPerTon *
                (1.0 - latestYield / best.batch.yieldRatePercent!!)
        } else null

        YieldAnalysis(
            avgYieldPercent = avg,
            recordedCount = recorded.size,
            latestBatchName = latest.batch.batchName,
            latestYieldPercent = latestYield,
            lossImpactPercent = 100.0 / latestYield - 1.0,
            bestBatchName = best.batch.batchName,
            bestYieldPercent = best.batch.yieldRatePercent,
            potentialSavingPerTon = savingPerTon?.let { CostCalculator.round2(it) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 出品率趋势（按时间从旧到新），不足 2 个记录时为空表 —— 供折线图复用 */
    val yieldTrend: StateFlow<List<Pair<String, Double>>> = batchesWithCost.map { rows ->
        rows.filter { (it.batch.yieldRatePercent ?: 0.0) > 0.0 }
            .reversed()
            .map { it.batch.batchName to it.batch.yieldRatePercent!! }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        /** 与 Product 实体默认值保持一致 */
        private const val DEFAULT_WEIGHT_PER_BOX_GRAM = 5000.0
        private const val DEFAULT_PACKAGES_PER_BOX = 20
    }
}

/**
 * 新建批次 ViewModel
 *
 * 表单输入（批次日期 / 样品重量 / 备注 / 加工费 / 出品率）保存在 ViewModel
 * 而非 Composable 中，这样 Activity 重建（旋转屏幕、切换深色模式、
 * 内存回收后返回）时，用户已填写的内容不会丢失。
 */
@HiltViewModel
class NewBatchViewModel @Inject constructor(
    private val repo: CostRepository,
    private val ocrAnalyzer: OcrAnalyzer
) : BaseViewModel() {

    private val _ingredients = MutableStateFlow<List<BatchIngredient>>(emptyList())
    val ingredients: StateFlow<List<BatchIngredient>> = _ingredients.asStateFlow()

    val allIngredients: StateFlow<List<Ingredient>> =
        repo.allIngredientsWithUseCount.map { rows ->
            rows.map { it.ingredient }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalMaterialCost: StateFlow<Double> = _ingredients.map { list ->
        list.sumOf { it.totalCost }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ─────────── 表单输入（跨配置变更保留） ───────────

    /** 批次日期（UTC 毫秒），null = 尚未选择 */
    private val _batchDateMillis = MutableStateFlow<Long?>(null)
    val batchDateMillis: StateFlow<Long?> = _batchDateMillis.asStateFlow()

    /**
     * 样品重量原始输入。
     * 保留字符串而非 Double，允许用户输入 "12." 这类中间态而不被打断。
     */
    private val _sampleWeight = MutableStateFlow("")
    val sampleWeight: StateFlow<String> = _sampleWeight.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    // ── 加工费分项（元）──
    private val _packagingCost = MutableStateFlow("")
    val packagingCost: StateFlow<String> = _packagingCost.asStateFlow()

    private val _laborCost = MutableStateFlow("")
    val laborCost: StateFlow<String> = _laborCost.asStateFlow()

    private val _overheadCost = MutableStateFlow("")
    val overheadCost: StateFlow<String> = _overheadCost.asStateFlow()

    /** 加工费合计（实时预览，供汇总卡片展示）。注意：必须声明在三个分项字段之后 */
    val totalProcessingCost: StateFlow<Double> = combine(
        _packagingCost, _laborCost, _overheadCost
    ) { p, l, o ->
        (p.toDoubleOrNull() ?: 0.0) + (l.toDoubleOrNull() ?: 0.0) + (o.toDoubleOrNull() ?: 0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** 出品率(%)，空串 = 不折算（按 100% 计） */
    private val _yieldRate = MutableStateFlow("")
    val yieldRate: StateFlow<String> = _yieldRate.asStateFlow()

    /** 是否满足提交条件：已选日期且样品重量为正数 */
    val canSubmit: StateFlow<Boolean> = combine(_batchDateMillis, _sampleWeight) { date, weight ->
        date != null && (weight.toDoubleOrNull() ?: 0.0) > 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 样品重量解析结果，无效或非正数返回 null */
    private val parsedSampleWeight: Double?
        get() = _sampleWeight.value.toDoubleOrNull()?.takeIf { it > 0.0 }

    /** 出品率解析结果：空串或非法 → null（不折算，按 100% 计） */
    private val parsedYieldRate: Double?
        get() = _yieldRate.value.toDoubleOrNull()?.takeIf { it > 0.0 }

    private val parsedPackagingCost: Double
        get() = _packagingCost.value.toDoubleOrNull() ?: 0.0

    private val parsedLaborCost: Double
        get() = _laborCost.value.toDoubleOrNull() ?: 0.0

    private val parsedOverheadCost: Double
        get() = _overheadCost.value.toDoubleOrNull() ?: 0.0

    fun onBatchDateChange(millis: Long?) {
        _batchDateMillis.value = millis
    }

    fun onSampleWeightChange(value: String) {
        _sampleWeight.value = value
    }

    fun onNoteChange(value: String) {
        _note.value = value
    }

    fun onPackagingCostChange(value: String) {
        _packagingCost.value = value
    }

    fun onLaborCostChange(value: String) {
        _laborCost.value = value
    }

    fun onOverheadCostChange(value: String) {
        _overheadCost.value = value
    }

    fun onYieldRateChange(value: String) {
        _yieldRate.value = value
    }

    /**
     * 添加配料：由原料档案 + 用量构造，小计较走 [BatchIngredient.create] 统一换算
     *
     * @param weightGram 用量（g）；比例输入时请先经 CostCalculator.ratioPercentToGram 换算
     * @param ratioPercent 用户输入的原始比例(%)，null = 按克重输入
     */
    fun addIngredient(
        ingredient: Ingredient,
        weightGram: Double,
        ratioPercent: Double? = null
    ) {
        _ingredients.value = _ingredients.value + BatchIngredient.create(
            ingredient = ingredient,
            weightGram = weightGram,
            ratioPercent = ratioPercent
        )
    }

    /**
     * 按索引移除配料。
     * 按值移除在存在两条内容相同的配料时行为不明确，按索引定位更可靠。
     */
    fun removeIngredientAt(index: Int) {
        if (index !in _ingredients.value.indices) return
        _ingredients.value = _ingredients.value.toMutableList().also { it.removeAt(index) }
    }

    /**
     * 快速添加原料入库（新建批次页的原料选择器内）
     * 走配料库去重（名称+品牌）：同名同品牌更新成本，同名不同品牌各自建档
     */
    fun saveIngredient(name: String, brand: String, category: String, unitPricePerKg: Double) {
        launchSafe {
            repo.saveIngredientByNameAndBrand(name, brand, category, unitPricePerKg)
        }
    }

    /**
     * OCR 批量入库：识别出的配料名全部加入配料库（去重，品牌留空）
     * 走单事务批量写入，避免几十种配料产生同等数量的独立写事务
     */
    fun saveIngredientBatch(names: List<String>, category: String = "") {
        launchSafe {
            repo.saveIngredients(names.map { IngredientUpsert(name = it, category = category) })
        }
    }

    /** OCR 扫描状态 */
    private val _ocrScanning = MutableStateFlow(false)
    val ocrScanning: StateFlow<Boolean> = _ocrScanning.asStateFlow()

    /**
     * OCR 识别配料表图片，识别结果批量入库。
     *
     * 三种结果都有明确反馈：
     * - 识别成功有配料 → 入库
     * - 识别成功但无配料 → 提示重拍
     * - 识别失败 → 提示失败原因（OcrAnalyzer 不再吞异常）
     */
    fun recognizeIngredients(uri: android.net.Uri, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _ocrScanning.value = true
            try {
                ocrAnalyzer.recognizeIngredientNames(uri)
                    .onSuccess { names ->
                        if (names.isNotEmpty()) {
                            repo.saveIngredients(names.map { IngredientUpsert(name = it) })
                        } else {
                            showError("没有识别到配料文字，请重拍或换一张更清晰的照片")
                        }
                    }
                    .onFailure {
                        showError("识别失败：${it.userMessage()}")
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showError(e.userMessage())
            } finally {
                _ocrScanning.value = false
                onDone()
            }
        }
    }

    /**
     * 保存新批次。样品重量、备注、加工费、出品率均从 ViewModel 表单状态读取。
     *
     * 保存发生在 viewModelScope（跨配置变更存活），完成后经 [onResult] 回调；
     * UI 只在成功时导航返回 —— 修复「点保存立即弹栈，协程被取消导致静默丢单」：
     * 失败时页面保留，错误经 Snackbar 提示。
     *
     * @param batchDate 批次日期 yyyy-MM-dd，批次名自动生成为「日期-序号」
     */
    fun saveBatch(
        productId: Long,
        batchDate: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (_saving.value) return
        val weight = parsedSampleWeight ?: run { onResult(false); return }
        val ingredientsSnapshot = _ingredients.value
        val draft = buildBatchDraft(productId, weight)
        _saving.value = true
        viewModelScope.launch {
            val ok = try {
                val batchName = generateBatchName(productId, batchDate)
                repo.saveBatchWithIngredients(draft.copy(batchName = batchName), ingredientsSnapshot)
                resetForm()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showError(e.userMessage())
                false
            }
            _saving.value = false
            onResult(ok)
        }
    }

    private fun buildBatchDraft(productId: Long, sampleWeight: Double) = BatchRecord(
        productId = productId,
        batchName = "", // 保存时由 generateBatchName 填充
        sampleWeightGram = sampleWeight,
        packagingCost = parsedPackagingCost,
        laborCost = parsedLaborCost,
        overheadCost = parsedOverheadCost,
        yieldRatePercent = parsedYieldRate,
        note = _note.value
    )

    /**
     * 生成批次名：日期 + 序号（如 2026-09-03-01）
     * 序号 = 该产品同一日期下已有批次的最大序号 + 1
     */
    private suspend fun generateBatchName(productId: Long, date: String): String {
        val prefix = "$date-"
        val batches = repo.getBatchesByProduct(productId).first()
        val maxSeq = batches.asSequence()
            .map { it.batchName }
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.removePrefix(prefix).toIntOrNull() }
            .maxOrNull() ?: 0
        return "$prefix${(maxSeq + 1).toString().padStart(2, '0')}"
    }

    // ─────────── 批次编辑与复制 ───────────

    private val _editBatchId = MutableStateFlow<Long?>(null)
    val editBatchId: StateFlow<Long?> = _editBatchId.asStateFlow()

    /** 编辑模式：批次信息草稿 */
    private val _editBatchInfo = MutableStateFlow<BatchRecord?>(null)
    val editBatchInfo: StateFlow<BatchRecord?> = _editBatchInfo.asStateFlow()

    /** 自动生成的批次名预览（日期+序号） */
    private val _batchNamePreview = MutableStateFlow("")
    val batchNamePreview: StateFlow<String> = _batchNamePreview.asStateFlow()

    val isEditMode: Boolean get() = _editBatchId.value != null

    /**
     * 刷新批次名预览（选择日期或进入编辑时调用）
     * 编辑模式且日期未变时，保持原名
     */
    fun refreshBatchNamePreview(productId: Long, date: String) {
        launchSafe {
            val existing = _editBatchInfo.value
            if (existing != null && existing.batchName.take(10) == date) {
                _batchNamePreview.value = existing.batchName
            } else {
                _batchNamePreview.value = generateBatchName(productId, date)
            }
        }
    }

    /**
     * 已成功加载进草稿态的批次 id（编辑模式）与复制来源 id（复制模式）。
     *
     * Composable 在每次旋转等配置变更后重新进入组合，LaunchedEffect 会再次触发；
     * 若不设守卫，loadBatchForEdit/copyFromTemplate 会用数据库旧值覆盖用户
     * 已修改未保存的表单——「状态存 ViewModel 防丢失」的前提是这里不再重复加载。
     */
    private var loadedEditBatchId: Long? = null
    private var copiedFromBatchId: Long? = null

    /**
     * 加载现有批次到草稿态用于编辑。
     *
     * 同时回填表单字段，使配置变更后用户已修改的内容仍可恢复；
     * 同一批次只加载一次，之后以 ViewModel 草稿为准。
     */
    fun loadBatchForEdit(batchId: Long) {
        if (loadedEditBatchId == batchId) return
        _editBatchId.value = batchId
        launchSafe {
            val batch = repo.getBatchById(batchId).first()
            if (batch == null) {
                loadedEditBatchId = batchId
                showError("要编辑的批次不存在或已被删除")
                return@launchSafe
            }
            loadedEditBatchId = batchId
            fillFormFrom(batch, repo.getBatchIngredients(batchId).first())
        }
    }

    /**
     * 复制批次为模板：配料、重量、加工费、出品率、备注原样带入，
     * 日期重置为今天，批次号按今天重新生成。
     *
     * 试产迭代时 80% 的配料不变，只调整一两种 —— 以此替代逐项重录。
     * 同一来源批次只加载一次，防止配置变更后草稿被重置。
     */
    fun copyFromTemplate(templateBatchId: Long) {
        if (copiedFromBatchId == templateBatchId) return
        launchSafe {
            val batch = repo.getBatchById(templateBatchId).first()
            if (batch == null) {
                showError("要复制的批次不存在或已被删除")
                return@launchSafe
            }
            fillFormFrom(batch, repo.getBatchIngredients(templateBatchId).first())
            // 复制件不进入编辑模式：保存时会生成全新批次
            _editBatchId.value = null
            _editBatchInfo.value = null
            _batchDateMillis.value = toUtcDateMillis(System.currentTimeMillis())
            copiedFromBatchId = templateBatchId
        }
    }

    /** 把批次数据回填到表单草稿（编辑与复制共用） */
    private fun fillFormFrom(batch: BatchRecord, ingredients: List<BatchIngredient>) {
        _ingredients.value = ingredients
        _sampleWeight.value = batch.sampleWeightGram.toString()
        _note.value = batch.note
        _packagingCost.value = batch.packagingCost.takeIf { it > 0.0 }?.toString() ?: ""
        _laborCost.value = batch.laborCost.takeIf { it > 0.0 }?.toString() ?: ""
        _overheadCost.value = batch.overheadCost.takeIf { it > 0.0 }?.toString() ?: ""
        _yieldRate.value = batch.yieldRatePercent?.toString() ?: ""
        _batchDateMillis.value = parseBatchDateMillis(batch.batchName)
            ?: toUtcDateMillis(batch.createdAt)
    }

    fun clearEditMode() {
        _editBatchId.value = null
        _editBatchInfo.value = null
        loadedEditBatchId = null
        copiedFromBatchId = null
        resetForm()
    }

    /**
     * 更新批次：替换批次记录 + 全量替换原料明细。
     * 样品重量、备注、加工费、出品率从 ViewModel 表单状态读取。
     *
     * 与 [saveBatch] 相同：完成后再经 [onResult] 回调导航，失败留在本页。
     *
     * @param batchDate 日期 yyyy-MM-dd；与原名日期相同时保留原序号
     */
    fun updateBatch(batchDate: String, onResult: (Boolean) -> Unit = {}) {
        if (_saving.value) return
        val existing = _editBatchInfo.value ?: run { onResult(false); return }
        val weight = parsedSampleWeight ?: run { onResult(false); return }
        val ingredientsSnapshot = _ingredients.value
        _saving.value = true
        viewModelScope.launch {
            val ok = try {
                val oldDate = existing.batchName.take(10)
                val batchName = if (oldDate == batchDate) {
                    existing.batchName // 日期未变，保留原批次名
                } else {
                    generateBatchName(existing.productId, batchDate)
                }
                val updated = existing.copy(
                    batchName = batchName,
                    sampleWeightGram = weight,
                    packagingCost = parsedPackagingCost,
                    laborCost = parsedLaborCost,
                    overheadCost = parsedOverheadCost,
                    yieldRatePercent = parsedYieldRate,
                    note = _note.value,
                    updatedAt = System.currentTimeMillis()
                )
                // 原子化更新批次 + 全量替换原料明细（单事务，避免成本计算读到中间状态）
                repo.updateBatchWithIngredients(updated, ingredientsSnapshot)
                _editBatchId.value = null
                _editBatchInfo.value = null
                resetForm()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                showError(e.userMessage())
                false
            }
            _saving.value = false
            onResult(ok)
        }
    }

    /** 保存进行中：防止双击重复提交，UI 据此禁用按钮 */
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    /** 清空表单与配料草稿（保存成功或主动退出编辑时调用） */
    private fun resetForm() {
        _ingredients.value = emptyList()
        _sampleWeight.value = ""
        _note.value = ""
        _packagingCost.value = ""
        _laborCost.value = ""
        _overheadCost.value = ""
        _yieldRate.value = ""
        _batchDateMillis.value = null
        _batchNamePreview.value = ""
        loadedEditBatchId = null
        copiedFromBatchId = null
    }
}

/**
 * UI 数据类 - 批次卡片完整信息
 *
 * @param processingCost 加工费合计（包材 + 人工 + 水电折旧）
 * @param totalCost 总成本 = 原料成本 + 加工费
 * @param suggestedTonPrice 按产品目标毛利率推导的建议出厂价，未设置毛利率时为 null
 */
data class BatchWithCostUI(
    val batch: BatchRecord,
    val ingredients: List<BatchIngredient>,
    val materialCost: Double,
    val processingCost: Double,
    val totalCost: Double,
    val result: CostResult,
    val differential: CostCalculator.CostDifferential?,
    val suggestedTonPrice: Double? = null
)

/**
 * UI 数据类 - 产品维度损耗分析摘要
 *
 * @param avgYieldPercent 有出品率记录批次的均值
 * @param recordedCount 记录了出品率的批次数
 * @param latestBatchName / latestYieldPercent 最近一次记录的批次名与出品率
 * @param lossImpactPercent 熬煮损耗使吨价上升的百分比 = 100/出品率 - 1
 * @param bestBatchName / bestYieldPercent 历史最佳出品率及其批次
 * @param potentialSavingPerTon 最近批次若恢复到最佳出品率，吨价可降金额（元/吨）；
 *   最近批次已是最佳时为 null
 */
data class YieldAnalysis(
    val avgYieldPercent: Double,
    val recordedCount: Int,
    val latestBatchName: String,
    val latestYieldPercent: Double,
    val lossImpactPercent: Double,
    val bestBatchName: String? = null,
    val bestYieldPercent: Double? = null,
    val potentialSavingPerTon: Double? = null
)
