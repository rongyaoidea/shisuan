package com.example.shisuan.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shisuan.core.ocr.OcrAnalyzer
import com.example.shisuan.data.database.*
import com.example.shisuan.data.repository.CostRepository
import com.example.shisuan.domain.model.CostResult
import com.example.shisuan.utils.CostCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 产品列表 ViewModel - Hilt 注入
 */
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repo: CostRepository
) : ViewModel() {
    
    val products: StateFlow<List<Product>> = 
        repo.allProducts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun saveProduct(
        name: String, category: String = "", description: String = "",
        packagesPerBox: Int = 20, weightPerPackageGram: Double = 250.0
    ) {
        viewModelScope.launch {
            repo.saveProduct(
                Product(
                    name = name,
                    category = category,
                    description = description,
                    // 每箱克数 = 每箱包数 × 每包克数，无需用户手算
                    weightPerBoxGram = packagesPerBox * weightPerPackageGram,
                    packagesPerBox = packagesPerBox,
                    weightPerPackageGram = weightPerPackageGram
                )
            )
        }
    }
    
    fun deleteProduct(product: Product) {
        viewModelScope.launch {
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
) : ViewModel() {
    
    private val _productId = MutableStateFlow<Long?>(null)
    
    fun setProduct(id: Long) {
        _productId.value = id
    }
    
    val product: StateFlow<Product?> = _productId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repo.getProductById(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
    
    /**
     * 预计算：批次 + 成本结果
     * 使用产品级包装规格（Product.weightPerBoxGram/packagesPerBox），
     * 不再依赖全局 UnitConfig（向后兼容保留）
     *
     * 每次批次/产品流发射时实时重查原料明细（不缓存），
     * 保证「先改批次再删插原料」的编辑链路成本计算不读到空数据。
     */
    val batchesWithCost: StateFlow<List<BatchWithCostUI>> =
        _productId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else {
                combine(repo.getBatchesByProduct(id), repo.getProductById(id)) { bs, prod ->
                    val boxGram = prod?.weightPerBoxGram ?: 5000.0
                    val pkgBox = prod?.packagesPerBox ?: 20

                    bs.mapIndexed { index, batch ->
                        val ingredients = repo.getBatchIngredients(batch.id).first()
                        val materialCost = ingredients.sumOf { it.totalCost }

                        val result = CostCalculator.calculate(
                            sampleWeightGram = batch.sampleWeightGram,
                            materialCost = materialCost,
                            weightPerBoxGram = boxGram,
                            packagesPerBox = pkgBox
                        )

                        val prev = bs.getOrNull(index + 1)
                        val prevTonCost = if (prev != null) {
                            val prevIngredients = repo.getBatchIngredients(prev.id).first()
                            val prevMaterialCost = prevIngredients.sumOf { it.totalCost }
                            CostCalculator.calculate(
                                prev.sampleWeightGram,
                                prevMaterialCost,
                                boxGram,
                                pkgBox
                            ).unitCostPerTon
                        } else null

                        val diff = CostCalculator.calcDifferential(result.unitCostPerTon, prevTonCost)

                        BatchWithCostUI(batch, ingredients, materialCost, result, diff)
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun deleteBatch(batch: BatchRecord) {
        viewModelScope.launch {
            repo.deleteBatch(batch)
        }
    }
}

/**
 * 新建批次 ViewModel
 */
@HiltViewModel
class NewBatchViewModel @Inject constructor(
    private val repo: CostRepository,
    private val ocrAnalyzer: OcrAnalyzer
) : ViewModel() {
    
    private val _ingredients = MutableStateFlow<List<BatchIngredient>>(emptyList())
    val ingredients: StateFlow<List<BatchIngredient>> = _ingredients.asStateFlow()
    
    val allIngredients: StateFlow<List<Ingredient>> = 
        repo.allIngredients.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val totalMaterialCost: StateFlow<Double> = _ingredients.map { list ->
        list.sumOf { it.totalCost }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)
    
    fun addIngredient(ingredient: BatchIngredient) {
        _ingredients.value = _ingredients.value + ingredient
    }

    /**
     * 快速添加原料入库（新建批次页的原料选择器内）
     * 走配料库去重（名称+品牌）：同名同品牌更新成本，同名不同品牌各自建档
     */
    fun saveIngredient(name: String, brand: String, category: String, unitPricePerKg: Double) {
        viewModelScope.launch {
            repo.saveIngredientByNameAndBrand(name, brand, category, unitPricePerKg)
        }
    }

    /**
     * OCR 批量入库：识别出的配料名全部加入配料库（去重，品牌留空）
     */
    fun saveIngredientBatch(names: List<String>, category: String = "") {
        viewModelScope.launch {
            names.forEach { name ->
                repo.saveIngredientByNameAndBrand(name, "", category, 0.0)
            }
        }
    }

    /** OCR 扫描状态 */
    private val _ocrScanning = MutableStateFlow(false)
    val ocrScanning: StateFlow<Boolean> = _ocrScanning.asStateFlow()

    /**
     * OCR 识别配料表图片，识别结果批量入库
     */
    fun recognizeIngredients(uri: android.net.Uri, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _ocrScanning.value = true
            try {
                val names = ocrAnalyzer.recognizeIngredientNames(uri)
                if (names.isNotEmpty()) {
                    saveIngredientBatch(names)
                }
            } finally {
                _ocrScanning.value = false
                onDone()
            }
        }
    }
    
    fun removeIngredient(ingredient: BatchIngredient) {
        _ingredients.value = _ingredients.value - ingredient
    }
    
    fun updateIngredient(old: BatchIngredient, new: BatchIngredient) {
        _ingredients.value = _ingredients.value.map { if (it == old) new else it }
    }
    
    fun saveBatch(
        productId: Long,
        batchDate: String, // 批次日期 yyyy-MM-dd，批次名自动生成为 日期-序号
        sampleWeight: Double,
        note: String
    ) {
        viewModelScope.launch {
            val batchName = generateBatchName(productId, batchDate)
            val batch = BatchRecord(
                productId = productId,
                batchName = batchName,
                sampleWeightGram = sampleWeight,
                note = note
            )
            repo.saveBatchWithIngredients(batch, _ingredients.value)
            _ingredients.value = emptyList()
        }
    }

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

    // ─────────── 批次编辑 ───────────

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
        viewModelScope.launch {
            val existing = _editBatchInfo.value
            if (existing != null && existing.batchName.take(10) == date) {
                _batchNamePreview.value = existing.batchName
            } else {
                _batchNamePreview.value = generateBatchName(productId, date)
            }
        }
    }

    /**
     * 加载现有批次到草稿态用于编辑
     */
    fun loadBatchForEdit(batchId: Long) {
        _editBatchId.value = batchId
        viewModelScope.launch {
            val batch = repo.getBatchById(batchId).first()
            _editBatchInfo.value = batch
            if (batch != null) {
                val ings = repo.getBatchIngredients(batchId).first()
                _ingredients.value = ings
            }
        }
    }

    fun clearEditMode() {
        _editBatchId.value = null
        _editBatchInfo.value = null
        _ingredients.value = emptyList()
    }

    /**
     * 更新批次：替换批次记录 + 全量替换原料明细
     * @param batchDate 日期 yyyy-MM-dd；与原名日期相同时保留原序号
     */
    fun updateBatch(
        batchDate: String,
        sampleWeight: Double,
        note: String
    ) {
        if (_editBatchId.value == null) return
        val existing = _editBatchInfo.value ?: return
        viewModelScope.launch {
            val oldDate = existing.batchName.take(10)
            val batchName = if (oldDate == batchDate) {
                existing.batchName // 日期未变，保留原批次名
            } else {
                generateBatchName(existing.productId, batchDate)
            }
            val updated = existing.copy(
                batchName = batchName,
                sampleWeightGram = sampleWeight,
                note = note,
                updatedAt = System.currentTimeMillis()
            )
            // 原子化更新批次 + 全量替换原料明细（单事务，避免成本计算读到中间状态）
            repo.updateBatchWithIngredients(updated, _ingredients.value)
            _ingredients.value = emptyList()
            _editBatchId.value = null
            _editBatchInfo.value = null
        }
    }
}

/**
 * UI 数据类
 */
data class BatchWithCostUI(
    val batch: BatchRecord,
    val ingredients: List<BatchIngredient>,
    val materialCost: Double,
    val result: CostResult,
    val differential: CostCalculator.CostDifferential?
)
