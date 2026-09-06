package com.example.shisuan.data.repository

import androidx.room.withTransaction
import com.example.shisuan.data.database.*
import com.example.shisuan.utils.BatchSnapshotCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * 数据仓库层 - 统一数据访问
 * 重构后支持 Product 产品管理
 */
class CostRepository(private val db: CostCalDatabase) {
    
    // ============ Product 产品管理 ============
    
    val allProducts: Flow<List<Product>> = db.productDao().getAllActive()

    fun getProductById(id: Long): Flow<Product?> = db.productDao().getById(id)

    suspend fun saveProduct(product: Product): Long =
        db.productDao().insert(product)
    
    suspend fun updateProduct(product: Product) = 
        db.productDao().update(product)
    
    suspend fun deleteProduct(product: Product) = 
        db.productDao().delete(product)
    
    suspend fun deactivateProduct(id: Long) = 
        db.productDao().deactivate(id)
    
    // ============ Batch 批次管理 ============

    fun getBatchesByProduct(productId: Long): Flow<List<BatchRecord>> =
        db.batchDao().getByProduct(productId)
    
    /**
     * 一次取回批次及其配料明细（单查询替代 N 次配料查询，避免 N+1 放大）
     */
    fun getBatchesWithIngredients(productId: Long): Flow<List<BatchWithIngredients>> =
        db.batchDao().getBatchesWithIngredients(productId)
    
    fun getBatchById(id: Long): Flow<BatchRecord?> = 
        db.batchDao().getById(id)
    
    /**
     * 保存批次 + 原料明细（事务）
     */
    suspend fun saveBatchWithIngredients(
        batch: BatchRecord,
        ingredients: List<BatchIngredient>
    ): Long = db.withTransaction {
        val batchId = db.batchDao().insert(batch)
        val savedIngredients = ingredients.map { it.copy(batchId = batchId) }
        db.batchIngredientDao().insertAll(savedIngredients)

        // git 式版本链：写入初始版本快照（同内容自动去重）
        captureSnapshotLocked(batch.copy(id = batchId), savedIngredients)

        // 记录操作日志
        db.logDao().insert(
            OperationLog(
                operationType = "CREATE_BATCH",
                targetType = "BatchRecord",
                targetId = batchId,
                details = "批次 ${batch.batchName}，${ingredients.size} 种原料"
            )
        )
        return@withTransaction batchId
    }
    
    suspend fun updateBatch(batch: BatchRecord) = 
        db.batchDao().update(batch)

    /**
     * 原子化更新批次 + 全量替换原料明细（单事务）
     * 避免「先改批次再删插原料」被批次流读到中间状态，导致成本计算读到空/旧数据
     */
    suspend fun updateBatchWithIngredients(
        batch: BatchRecord,
        ingredients: List<BatchIngredient>
    ) = db.withTransaction {
        db.batchDao().update(batch)
        db.batchIngredientDao().deleteByBatch(batch.id)
        db.batchIngredientDao().insertAll(
            ingredients.map { it.copy(batchId = batch.id, id = 0) }
        )
        // git 式版本链：每次有效变更生成新版本（内容指纹去重）
        captureSnapshotLocked(batch, ingredients)
    }

    /**
     * git 式版本链核心：编码当前内容 → 计算指纹 → 查重后入库。
     *
     * - 指纹相同（内容未变，如「保存但没改任何东西」）→ 跳过，时间线不产生噪音提交
     * - version 在同批次内递增，用于时间线排序与「当前版本」定位
     * 仅供事务内部调用（方法名以 Locked 结尾以示提醒）。
     */
    private suspend fun captureSnapshotLocked(
        batch: BatchRecord,
        ingredients: List<BatchIngredient>
    ) {
        val encoded = BatchSnapshotCodec.encode(batch, ingredients)
        val digest = BatchSnapshotCodec.digestOf(encoded)
        if (db.snapshotDao().getByDigest(batch.id, digest) != null) return
        db.snapshotDao().insert(
            BatchSnapshot(
                batchId = batch.id,
                snapshotData = encoded,
                digest = digest,
                version = (db.snapshotDao().maxVersion(batch.id) ?: 0) + 1
            )
        )
    }

    /**
     * 恢复到历史版本：解码快照并原子写回批次与配料。
     *
     * - 批次号保留当前值（改过日期的批次不回退编号）
     * - 恢复后内容与目标版本一致 → 指纹相同 → 下次保存不会产生重复提交
     * - 恢复动作写入操作日志（审计可追溯「谁在何时回到过哪个版本」）
     *
     * @return false 表示快照数据损坏或批次已不存在
     */
    suspend fun restoreSnapshot(snapshot: BatchSnapshot): Boolean = db.withTransaction {
        val data = BatchSnapshotCodec.decode(snapshot.snapshotData)
            ?: return@withTransaction false
        val batch = db.batchDao().getById(snapshot.batchId).first()
            ?: return@withTransaction false

        db.batchDao().update(
            batch.copy(
                sampleWeightGram = data.sampleWeightGram,
                packagingCost = data.packagingCost,
                laborCost = data.laborCost,
                overheadCost = data.overheadCost,
                yieldRatePercent = data.yieldRatePercent,
                note = data.note,
                updatedAt = System.currentTimeMillis()
            )
        )
        db.batchIngredientDao().deleteByBatch(snapshot.batchId)
        db.batchIngredientDao().insertAll(
            data.ingredients.map { it.copy(batchId = snapshot.batchId, id = 0) }
        )
        db.logDao().insert(
            OperationLog(
                operationType = "RESTORE_SNAPSHOT",
                targetType = "BatchRecord",
                targetId = snapshot.batchId,
                details = "批次恢复到版本 #${snapshot.digest}"
            )
        )
        true
    }
    
    suspend fun deleteBatch(batch: BatchRecord) = db.withTransaction {
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

    // ============ Ingredient 原料库 ============

    /**
     * 全部活跃原料（含使用频次，按频次降序）。
     * 配料库列表展示「用于 N 个批次」，批次录入的原料选择器借此把常用原料前置。
     */
    val allIngredientsWithUseCount: Flow<List<IngredientWithUseCount>> =
        db.ingredientDao().getAllActiveWithUseCount()

    suspend fun getIngredientById(id: Long): Ingredient? = 
        db.ingredientDao().getById(id)
    
    suspend fun saveIngredient(ingredient: Ingredient): Long = 
        db.ingredientDao().insert(ingredient)
    
    /**
     * 按名称+品牌保存原料（配料库去重）：
     * 同名同品牌已存在时更新其单价为最新值；同名不同品牌各自建档。
     * 保证同款原料的不同品牌（价格）可以共存，成本始终为最近一次录入的最新值。
     * @param brand 品牌/供应商，空串表示不区分品牌
     */
    suspend fun saveIngredientByNameAndBrand(
        name: String, brand: String, category: String, unitPricePerKg: Double
    ): Long {
        val trimmed = name.trim()
        val brandTrimmed = brand.trim()
        val existing = db.ingredientDao().getByNameAndBrand(trimmed, brandTrimmed)
        return if (existing != null) {
            db.ingredientDao().update(
                existing.copy(
                    category = category.ifEmpty { existing.category },
                    // 关键：0 表示「本次未填写/OCR 未识别到价格」，绝不能覆盖已有单价，
                    // 否则 OCR 重复识别会把原料价格静默清零，成本随之算错
                    unitPrice = preservedUnitPrice(unitPricePerKg, existing.unitPrice),
                    updatedAt = System.currentTimeMillis()
                )
            )
            existing.id
        } else {
            db.ingredientDao().insert(
                Ingredient(
                    name = trimmed,
                    supplier = brandTrimmed,
                    category = category,
                    unitPrice = unitPricePerKg,
                    priceUnit = "元/kg"
                )
            )
        }
    }
    
    /**
     * 批量按名称+品牌保存原料（单事务）
     *
     * OCR 一次可能识别出几十种配料，逐条写入会产生同等数量的独立事务，
     * 这里合并为一次事务提交。
     */
    suspend fun saveIngredients(upserts: List<IngredientUpsert>) = db.withTransaction {
        upserts.forEach { item ->
            saveIngredientByNameAndBrand(
                name = item.name,
                brand = item.brand,
                category = item.category,
                unitPricePerKg = item.unitPricePerKg
            )
        }
    }

    suspend fun updateIngredient(ingredient: Ingredient) = 
        db.ingredientDao().update(ingredient)
    
    suspend fun deleteIngredient(ingredient: Ingredient) = 
        db.ingredientDao().delete(ingredient)
    
    // ============ BatchResult 批次成果 ============

    fun getBatchResult(batchId: Long): Flow<BatchResult?> =
        db.batchResultDao().getByBatch(batchId)

    /**
     * 保存批次成果：存在则更新，否则新增（单事务 upsert）。
     * 口感/pH/糖度等试产结果与成本数据关联，用于「好且便宜」的配方复盘。
     */
    suspend fun saveOrUpdateResult(result: BatchResult) = db.withTransaction {
        val existing = db.batchResultDao().getByBatchOnce(result.batchId)
        if (existing == null) {
            db.batchResultDao().insert(result)
        } else {
            db.batchResultDao().update(result.copy(id = existing.id))
        }
    }

    // ============ Snapshot 快照 ============
    
    fun getBatchSnapshots(batchId: Long): Flow<List<BatchSnapshot>> =
        db.snapshotDao().getByBatch(batchId)

    companion object {
        /**
         * 名称+品牌 upsert 时的单价取舍：
         * 新价 > 0 视为本次录入的最新价；新价 = 0（未填写/OCR 未识别）保留旧价。
         * 纯函数，便于单元测试防止回归（见 IngredientPriceTest）。
         */
        fun preservedUnitPrice(incomingPrice: Double, existingPrice: Double): Double =
            if (incomingPrice > 0.0) incomingPrice else existingPrice
    }
}

/**
 * 原料批量入库请求（按名称+品牌去重）
 *
 * @param brand 品牌/供应商，空串表示不区分品牌
 * @param unitPricePerKg 参考单价，0 表示暂不设置（如 OCR 只识别出名称）
 */
data class IngredientUpsert(
    val name: String,
    val brand: String = "",
    val category: String = "",
    val unitPricePerKg: Double = 0.0
)
