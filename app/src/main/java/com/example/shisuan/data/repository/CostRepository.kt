package com.example.shisuan.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.example.shisuan.core.Clock
import com.example.shisuan.core.SystemClock
import com.example.shisuan.data.database.*
import com.example.shisuan.utils.BatchSnapshotCodec
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据仓库接口 - 统一数据访问
 * 重构后支持 Product 产品管理；实现见 [RoomCostRepository]。
 */
interface CostRepository {

    // ============ Product 产品管理 ============

    val allProducts: Flow<List<Product>>

    fun getProductById(id: Long): Flow<Product?>

    suspend fun saveProduct(product: Product): Long

    suspend fun updateProduct(product: Product)

    suspend fun deleteProduct(product: Product)

    suspend fun deactivateProduct(id: Long)

    // ============ Batch 批次管理 ============

    fun getBatchesByProduct(productId: Long): Flow<List<BatchRecord>>

    /**
     * 一次取回批次及其配料明细（单查询替代 N 次配料查询，避免 N+1 放大）
     */
    fun getBatchesWithIngredients(productId: Long): Flow<List<BatchWithIngredients>>

    fun getBatchById(id: Long): Flow<BatchRecord?>

    suspend fun saveBatchWithIngredients(
        batch: BatchRecord,
        ingredients: List<BatchIngredient>
    ): Long

    suspend fun updateBatch(batch: BatchRecord)

    suspend fun updateBatchWithIngredients(
        batch: BatchRecord,
        ingredients: List<BatchIngredient>
    )

    suspend fun restoreSnapshot(snapshot: BatchSnapshot): Boolean

    suspend fun deleteBatch(batch: BatchRecord)

    // ============ BatchIngredient 原料明细 ============

    fun getBatchIngredients(batchId: Long): Flow<List<BatchIngredient>>

    // ============ Ingredient 原料库 ============

    /**
     * 全部活跃原料（含使用频次，按频次降序）。
     * 配料库列表展示「用于 N 个批次」，批次录入的原料选择器借此把常用原料前置。
     */
    val allIngredientsWithUseCount: Flow<List<IngredientWithUseCount>>

    suspend fun getIngredientById(id: Long): Ingredient?

    suspend fun saveIngredient(ingredient: Ingredient): Long

    suspend fun saveIngredientByNameAndBrand(
        name: String, brand: String, category: String, unitPricePerKg: Double
    ): Long

    suspend fun saveIngredients(upserts: List<IngredientUpsert>)

    suspend fun updateIngredient(ingredient: Ingredient)

    suspend fun deleteIngredient(ingredient: Ingredient)

    // ============ BatchResult 批次成果 ============

    fun getBatchResult(batchId: Long): Flow<BatchResult?>

    suspend fun saveOrUpdateResult(result: BatchResult)

    // ============ Snapshot 快照 ============

    fun getBatchSnapshots(batchId: Long): Flow<List<BatchSnapshot>>

    /**
     * 手动触发操作日志保留策略（默认实现每次写日志后已自动调用，
     * VM 层一般无需调用；日志量异常时可主动调用）。
     */
    suspend fun trimOperationLogs(keepLatest: Int = 500)

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
 * 数据仓库 Room 实现 - 统一数据访问
 * 重构后支持 Product 产品管理
 */
@Singleton
class RoomCostRepository @Inject constructor(
    private val db: CostCalDatabase,
    private val clock: Clock = SystemClock()
) : CostRepository {

    // ============ Product 产品管理 ============

    override val allProducts: Flow<List<Product>> = db.productDao().getAllActive()

    override fun getProductById(id: Long): Flow<Product?> = db.productDao().getById(id)

    override suspend fun saveProduct(product: Product): Long =
        db.productDao().insert(product)

    override suspend fun updateProduct(product: Product) =
        db.productDao().update(product)

    override suspend fun deleteProduct(product: Product) =
        db.productDao().delete(product)

    override suspend fun deactivateProduct(id: Long) =
        db.productDao().deactivate(id)

    // ============ Batch 批次管理 ============

    override fun getBatchesByProduct(productId: Long): Flow<List<BatchRecord>> =
        db.batchDao().getByProduct(productId)

    /**
     * 一次取回批次及其配料明细（单查询替代 N 次配料查询，避免 N+1 放大）
     */
    override fun getBatchesWithIngredients(productId: Long): Flow<List<BatchWithIngredients>> =
        db.batchDao().getBatchesWithIngredients(productId)

    override fun getBatchById(id: Long): Flow<BatchRecord?> =
        db.batchDao().getById(id)

    /**
     * 保存批次 + 原料明细（事务）
     *
     * 注意：批次号由 (productId, batchName) 唯一索引兜底并发冲突；
     * 调用方（VM 层）应捕获 [SQLiteConstraintException] 后重新生成批次名重试一次。
     */
    override suspend fun saveBatchWithIngredients(
        batch: BatchRecord,
        ingredients: List<BatchIngredient>
    ): Long = db.withTransaction {
        val batchId = db.batchDao().insert(batch)
        val savedIngredients = ingredients.map { it.copy(batchId = batchId) }
        db.batchIngredientDao().insertAll(savedIngredients)

        // git 式版本链：写入初始版本快照（同内容自动去重）
        captureSnapshotLocked(batch.copy(id = batchId), savedIngredients)

        // 记录操作日志（时间经 Clock 注入，便于单测伪造；Entities 默认值不动）
        db.logDao().insert(
            OperationLog(
                operationType = "CREATE_BATCH",
                targetType = "BatchRecord",
                targetId = batchId,
                details = "批次 ${batch.batchName}，${ingredients.size} 种原料",
                createdAt = clock.now()
            )
        )
        db.logDao().deleteOldLogs()
        return@withTransaction batchId
    }

    override suspend fun updateBatch(batch: BatchRecord) =
        db.batchDao().update(batch)

    /**
     * 原子化更新批次 + 全量替换原料明细（单事务）
     * 避免「先改批次再删插原料」被批次流读到中间状态，导致成本计算读到空/旧数据
     */
    override suspend fun updateBatchWithIngredients(
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
    override suspend fun restoreSnapshot(snapshot: BatchSnapshot): Boolean = db.withTransaction {
        val data = BatchSnapshotCodec.decode(snapshot.snapshotData)
            ?: return@withTransaction false
        // suspend 直查替代 getById(...).first()，事务内单次读取
        val batch = db.batchDao().getByIdOnce(snapshot.batchId)
            ?: return@withTransaction false

        db.batchDao().update(
            batch.copy(
                sampleWeightGram = data.sampleWeightGram,
                packagingCost = data.packagingCost,
                laborCost = data.laborCost,
                overheadCost = data.overheadCost,
                yieldRatePercent = data.yieldRatePercent,
                note = data.note,
                updatedAt = clock.now()
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
        db.logDao().deleteOldLogs()
        true
    }

    override suspend fun deleteBatch(batch: BatchRecord) = db.withTransaction {
        db.batchDao().delete(batch)
        db.logDao().insert(
            OperationLog(
                operationType = "DELETE_BATCH",
                targetType = "BatchRecord",
                targetId = batch.id,
                details = "删除批次 ${batch.batchName}"
            )
        )
        db.logDao().deleteOldLogs()
    }

    // ============ BatchIngredient 原料明细 ============

    override fun getBatchIngredients(batchId: Long): Flow<List<BatchIngredient>> =
        db.batchIngredientDao().getByBatch(batchId)

    // ============ Ingredient 原料库 ============

    /**
     * 全部活跃原料（含使用频次，按频次降序）。
     * 配料库列表展示「用于 N 个批次」，批次录入的原料选择器借此把常用原料前置。
     */
    override val allIngredientsWithUseCount: Flow<List<IngredientWithUseCount>> =
        db.ingredientDao().getAllActiveWithUseCount()

    override suspend fun getIngredientById(id: Long): Ingredient? =
        db.ingredientDao().getById(id)

    override suspend fun saveIngredient(ingredient: Ingredient): Long =
        db.ingredientDao().insert(ingredient)

    /**
     * 按名称+品牌保存原料（配料库去重）：
     * 同名同品牌已存在时更新其单价为最新值；同名不同品牌各自建档。
     * 保证同款原料的不同品牌（价格）可以共存，成本始终为最近一次录入的最新值。
     * @param brand 品牌/供应商，空串表示不区分品牌
     */
    override suspend fun saveIngredientByNameAndBrand(
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
                    unitPrice = CostRepository.preservedUnitPrice(unitPricePerKg, existing.unitPrice),
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
    override suspend fun saveIngredients(upserts: List<IngredientUpsert>) = db.withTransaction {
        if (upserts.isEmpty()) return@withTransaction
        upserts.forEach { item ->
            saveIngredientByNameAndBrand(
                name = item.name,
                brand = item.brand,
                category = item.category,
                unitPricePerKg = item.unitPricePerKg
            )
        }
    }

    override suspend fun updateIngredient(ingredient: Ingredient) =
        db.ingredientDao().update(ingredient)

    override suspend fun deleteIngredient(ingredient: Ingredient) =
        db.ingredientDao().delete(ingredient)

    // ============ BatchResult 批次成果 ============

    override fun getBatchResult(batchId: Long): Flow<BatchResult?> =
        db.batchResultDao().getByBatch(batchId)

    /**
     * 保存批次成果：存在则更新，否则新增（单事务 upsert）。
     * 口感/pH/糖度等试产结果与成本数据关联，用于「好且便宜」的配方复盘。
     */
    override suspend fun saveOrUpdateResult(result: BatchResult) = db.withTransaction {
        val existing = db.batchResultDao().getByBatchOnce(result.batchId)
        if (existing == null) {
            db.batchResultDao().insert(result)
        } else {
            db.batchResultDao().update(result.copy(id = existing.id))
        }
    }

    // ============ Snapshot 快照 ============

    override fun getBatchSnapshots(batchId: Long): Flow<List<BatchSnapshot>> =
        db.snapshotDao().getByBatch(batchId)

    override suspend fun trimOperationLogs(keepLatest: Int) {
        db.logDao().deleteOldLogs(keepLatest)
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
