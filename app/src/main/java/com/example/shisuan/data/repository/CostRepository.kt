package com.example.shisuan.data.repository

import com.example.shisuan.data.database.*
import kotlinx.coroutines.flow.Flow

/**
 * 数据仓库层 - 统一数据访问
 * 重构后支持 Product 产品管理
 */
class CostRepository(private val db: CostCalDatabase) {
    
    // ============ Product 产品管理 ============
    
    val allProducts: Flow<List<Product>> = db.productDao().getAllActive()
    
    fun getProductById(id: Long): Flow<Product?> = db.productDao().getById(id)
    
    fun getProductsByCategory(category: String): Flow<List<Product>> = 
        db.productDao().getByCategory(category)
    
    suspend fun saveProduct(product: Product): Long = 
        db.productDao().insert(product)
    
    suspend fun updateProduct(product: Product) = 
        db.productDao().update(product)
    
    suspend fun deleteProduct(product: Product) = 
        db.productDao().delete(product)
    
    suspend fun deactivateProduct(id: Long) = 
        db.productDao().deactivate(id)
    
    // ============ Batch 批次管理 ============
    
    val allBatches: Flow<List<BatchRecord>> = db.batchDao().getAll()
    
    fun getBatchesByProduct(productId: Long): Flow<List<BatchRecord>> = 
        db.batchDao().getByProduct(productId)
    
    fun getBatchById(id: Long): Flow<BatchRecord?> = 
        db.batchDao().getById(id)
    
    /**
     * 保存批次 + 原料明细（事务）
     */
    suspend fun saveBatchWithIngredients(
        batch: BatchRecord,
        ingredients: List<BatchIngredient>
    ): Long {
        val batchId = db.batchDao().insert(batch)
        val ingredientsWithBatchId = ingredients.map { it.copy(batchId = batchId) }
        db.batchIngredientDao().insertAll(ingredientsWithBatchId)
        
        // 记录操作日志
        db.logDao().insert(
            OperationLog(
                operationType = "CREATE_BATCH",
                targetType = "BatchRecord",
                targetId = batchId,
                details = "批次 ${batch.batchName}，${ingredients.size} 种原料"
            )
        )
        return batchId
    }
    
    suspend fun updateBatch(batch: BatchRecord) = 
        db.batchDao().update(batch)
    
    suspend fun deleteBatch(batch: BatchRecord) {
        db.batchDao().delete(batch)
        db.logDao().insert(
            OperationLog(
                operationType = "DELETE_BATCH",
                targetType = "BatchRecord",
                targetId = batch.id,
                details = "删除批次 ${batch.batchName}"
            )
        )
    }
    
    // ============ BatchIngredient 原料明细 ============
    
    fun getBatchIngredients(batchId: Long): Flow<List<BatchIngredient>> = 
        db.batchIngredientDao().getByBatch(batchId)
    
    suspend fun getBatchMaterialCost(batchId: Long): Double = 
        db.batchIngredientDao().getTotalCost(batchId) ?: 0.0
    
    suspend fun addIngredientToBatch(ingredient: BatchIngredient): Long = 
        db.batchIngredientDao().insert(ingredient)
    
    suspend fun updateBatchIngredient(ingredient: BatchIngredient) = 
        db.batchIngredientDao().update(ingredient)
    
    suspend fun deleteBatchIngredient(ingredient: BatchIngredient) =
        db.batchIngredientDao().delete(ingredient)

    suspend fun deleteBatchIngredientsByBatchId(batchId: Long) =
        db.batchIngredientDao().deleteByBatch(batchId)
    
    // ============ Ingredient 原料库 ============
    
    val allIngredients: Flow<List<Ingredient>> = db.ingredientDao().getAllActive()
    
    fun getIngredientsByCategory(category: String): Flow<List<Ingredient>> = 
        db.ingredientDao().getByCategory(category)
    
    suspend fun getIngredientById(id: Long): Ingredient? = 
        db.ingredientDao().getById(id)
    
    suspend fun saveIngredient(ingredient: Ingredient): Long = 
        db.ingredientDao().insert(ingredient)
    
    suspend fun updateIngredient(ingredient: Ingredient) = 
        db.ingredientDao().update(ingredient)
    
    suspend fun deleteIngredient(ingredient: Ingredient) = 
        db.ingredientDao().delete(ingredient)
    
    // ============ UnitConfig 换算配置 ============
    
    val unitConfig: Flow<UnitConfig?> = db.unitConfigDao().getLatest()
    
    suspend fun saveUnitConfig(config: UnitConfig) = 
        db.unitConfigDao().insert(config)
    
    // ============ BatchResult 批次成果 ============
    
    fun getBatchResult(batchId: Long): Flow<BatchResult?> = 
        db.batchResultDao().getByBatch(batchId)
    
    suspend fun saveResult(result: BatchResult) = 
        db.batchResultDao().insert(result)
    
    suspend fun updateResult(result: BatchResult) = 
        db.batchResultDao().update(result)
    
    // ============ BatchProblem 问题记录 ============
    
    fun getBatchProblems(batchId: Long): Flow<List<BatchProblem>> = 
        db.batchProblemDao().getByBatch(batchId)
    
    suspend fun saveProblem(problem: BatchProblem) = 
        db.batchProblemDao().insert(problem)
    
    suspend fun updateProblem(problem: BatchProblem) = 
        db.batchProblemDao().update(problem)
    
    suspend fun deleteProblem(problem: BatchProblem) = 
        db.batchProblemDao().delete(problem)
    
    // ============ Snapshot 快照 ============
    
    fun getBatchSnapshots(batchId: Long): Flow<List<BatchSnapshot>> = 
        db.snapshotDao().getByBatch(batchId)
    
    suspend fun saveSnapshot(snapshot: BatchSnapshot) = 
        db.snapshotDao().insert(snapshot)
    
    // ============ OperationLog 操作日志 ============
    
    val recentLogs: Flow<List<OperationLog>> = db.logDao().getRecent()
    
    suspend fun log(operationType: String, targetType: String, targetId: Long, details: String) {
        db.logDao().insert(
            OperationLog(
                operationType = operationType,
                targetType = targetType,
                targetId = targetId,
                details = details
            )
        )
    }
}

/**
 * 数据类 - 批次完整信息（含成本计算）
 */
data class BatchWithCost(
    val batch: BatchRecord,
    val product: Product,
    val ingredients: List<BatchIngredient>,
    val materialCost: Double, // 原料总成本
    val totalCost: Double, // 总成本 = 原料 + 加工费
    val result: com.example.shisuan.domain.model.CostResult? = null,
    val previousTonCost: Double? = null
)

/**
 * 数据类 - 产品汇总信息
 */
data class ProductSummary(
    val product: Product,
    val batchCount: Int,
    val latestBatchDate: Long?,
    val avgTonCost: Double?,
    val minTonCost: Double?,
    val maxTonCost: Double?
)
