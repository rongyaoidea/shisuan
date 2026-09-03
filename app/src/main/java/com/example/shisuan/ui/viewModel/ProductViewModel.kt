package com.example.shisuan.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    
    fun saveProduct(name: String, category: String = "", description: String = "") {
        viewModelScope.launch {
            repo.saveProduct(
                Product(
                    name = name,
                    category = category,
                    description = description
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
     */
    val batchesWithCost: StateFlow<List<BatchWithCostUI>> = 
        combine(batches, unitConfig, repo.allProducts) { bs, cfg, _ ->
            if (cfg == null) return@combine emptyList()
            
            bs.mapIndexed { index, batch ->
                // 获取原料明细并计算总成本
                val ingredients = repo.getBatchIngredients(batch.id).first()
                val materialCost = ingredients.sumOf { it.totalCost }
                val totalCost = materialCost + batch.processingCost
                
                // 计算成本结果
                val result = CostCalculator.calculate(
                    sampleWeightGram = batch.sampleWeightGram,
                    materialCost = materialCost,
                    processingCost = batch.processingCost,
                    weightPerBoxGram = cfg.weightPerBoxGram,
                    packagesPerBox = cfg.packagesPerBox
                )
                
                // 计算上一批次差异
                val prev = bs.getOrNull(index + 1)
                val prevTonCost = if (prev != null) {
                    val prevIngredients = repo.getBatchIngredients(prev.id).first()
                    val prevMaterialCost = prevIngredients.sumOf { it.totalCost }
                    CostCalculator.calculate(
                        prev.sampleWeightGram,
                        prevMaterialCost,
                        prev.processingCost,
                        cfg.weightPerBoxGram,
                        cfg.packagesPerBox
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
    private val repo: CostRepository
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
            _ingredients.value = emptyList() // 清空
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
