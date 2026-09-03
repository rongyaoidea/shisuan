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
        weightPerBoxGram: Double = 5000.0, packagesPerBox: Int = 20
    ) {
        viewModelScope.launch {
            repo.saveProduct(
                Product(
                    name = name,
                    category = category,
                    description = description,
                    weightPerBoxGram = weightPerBoxGram,
                    packagesPerBox = packagesPerBox,
                    weightPerPackageGram = weightPerBoxGram / packagesPerBox
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
    
    val batches: StateFlow<List<BatchRecord>> = _productId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repo.getBatchesByProduct(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val unitConfig: StateFlow<UnitConfig?> =
        repo.unitConfig.stateIn(viewModelScope, SharingStarted.Lazily, null)

    /**
     * 预计算：批次 + 成本结果
     * 使用产品级包装规格（Product.weightPerBoxGram/packagesPerBox），
     * 不再依赖全局 UnitConfig（向后兼容保留）
     */
    val batchesWithCost: StateFlow<List<BatchWithCostUI>> =
        combine(batches, product, repo.allProducts) { bs, prod, _ ->
            // 产品级包装规格，未加载时用默认值
            val boxGram = prod?.weightPerBoxGram ?: 5000.0
            val pkgBox = prod?.packagesPerBox ?: 20

            bs.mapIndexed { index, batch ->
                val ingredients = repo.getBatchIngredients(batch.id).first()
                val materialCost = ingredients.sumOf { it.totalCost }

                val result = CostCalculator.calculate(
                    sampleWeightGram = batch.sampleWeightGram,
                    materialCost = materialCost,
                    processingCost = batch.processingCost,
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
                        prev.processingCost,
                        boxGram,
                        pkgBox
                    ).unitCostPerTon
                } else null

                val diff = CostCalculator.calcDifferential(result.unitCostPerTon, prevTonCost)

                BatchWithCostUI(batch, ingredients, materialCost, result, diff)
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
     */
    fun saveIngredient(name: String, category: String, unitPricePerKg: Double) {
        viewModelScope.launch {
            repo.saveIngredient(
                Ingredient(
                    name = name,
                    category = category,
                    unitPrice = unitPricePerKg,
                    priceUnit = "元/kg"
                )
            )
        }
    }

    /**
     * OCR 批量入库：识别出的配料名列表全部加入原料库
     */
    fun saveIngredientBatch(names: List<String>, category: String = "") {
        viewModelScope.launch {
            names.forEach { name ->
                repo.saveIngredient(
                    Ingredient(name = name, category = category, priceUnit = "元/kg")
                )
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
        batchName: String,
        sampleWeight: Double,
        processingCost: Double,
        note: String
    ) {
        viewModelScope.launch {
            val batch = BatchRecord(
                productId = productId,
                batchName = batchName,
                sampleWeightGram = sampleWeight,
                processingCost = processingCost,
                note = note
            )
            repo.saveBatchWithIngredients(batch, _ingredients.value)
            _ingredients.value = emptyList()
        }
    }

    // ─────────── 批次编辑 ───────────

    private val _editBatchId = MutableStateFlow<Long?>(null)
    val editBatchId: StateFlow<Long?> = _editBatchId.asStateFlow()

    /** 编辑模式：批次信息草稿 */
    private val _editBatchInfo = MutableStateFlow<BatchRecord?>(null)
    val editBatchInfo: StateFlow<BatchRecord?> = _editBatchInfo.asStateFlow()

    val isEditMode: Boolean get() = _editBatchId.value != null

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
     */
    fun updateBatch(
        batchName: String,
        sampleWeight: Double,
        processingCost: Double,
        note: String
    ) {
        val batchId = _editBatchId.value ?: return
        val existing = _editBatchInfo.value ?: return
        viewModelScope.launch {
            val updated = existing.copy(
                batchName = batchName,
                sampleWeightGram = sampleWeight,
                processingCost = processingCost,
                note = note,
                updatedAt = System.currentTimeMillis()
            )
            repo.updateBatch(updated)
            // 全量替换原料明细：先删后插
            repo.deleteBatchIngredientsByBatchId(batchId)
            val newIngredients = _ingredients.value.map { it.copy(batchId = batchId, id = 0) }
            newIngredients.forEach { repo.addIngredientToBatch(it) }
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
