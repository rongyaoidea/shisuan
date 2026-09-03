package com.example.shisuan.data.repository

import com.example.shisuan.data.database.*
import com.example.shisuan.domain.model.CostResult
import com.example.shisuan.utils.CostCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull

class CostRepository(
    private val batchDao: BatchDao,
    private val unitConfigDao: UnitConfigDao,
    private val ingredientDao: IngredientDao,
    private val batchMaterialDao: BatchMaterialDao,
    private val batchResultDao: BatchResultDao,
    private val batchProblemDao: BatchProblemDao,
    private val snapshotDao: SnapshotDao,
    private val logDao: LogDao
) {
    val allBatches: Flow<List<BatchRecord>> = batchDao.getAll()
    val allIngredients: Flow<List<Ingredient>> = ingredientDao.getAll()
    val unitConfig: Flow<UnitConfig?> = unitConfigDao.get()

    suspend fun insertBatch(batch: BatchRecord): Long {
        val id = batchDao.insert(batch)
        logDao.insert(OperationLog(
            operationType = "CREATE", entityType = "BATCH", entityId = id,
            entityName = "${batch.productName}-${batch.batchName}",
            after = batch.toString(), createdAt = System.currentTimeMillis()
        ))
        createSnapshot(id, batch)
        return id
    }

    suspend fun updateBatch(batch: BatchRecord) {
        batchDao.update(batch)
        logDao.insert(OperationLog(
            operationType = "UPDATE", entityType = "BATCH", entityId = batch.id,
            entityName = "${batch.productName}-${batch.batchName}",
            after = batch.toString(), createdAt = System.currentTimeMillis()
        ))
        createSnapshot(batch.id, batch)
    }

    suspend fun deleteBatch(batch: BatchRecord) = batchDao.delete(batch)

    suspend fun addMaterial(material: BatchMaterial) = batchMaterialDao.insert(material)
    suspend fun clearMaterials(batchId: Long) = batchMaterialDao.deleteByBatch(batchId)

    suspend fun saveResult(result: BatchResult) = batchResultDao.upsert(result)
    suspend fun getBatchResult(batchId: Long): BatchResult? = batchResultDao.getByBatch(batchId)

    suspend fun addProblem(problem: BatchProblem) = batchProblemDao.insert(problem)
    suspend fun updateProblem(problem: BatchProblem) = batchProblemDao.update(problem)
    fun getProblems(batchId: Long): Flow<List<BatchProblem>> = batchProblemDao.getByBatch(batchId)
    fun getAllProblems(resolved: Boolean): Flow<List<BatchProblem>> = batchProblemDao.getAll(resolved)

    suspend fun saveUnitConfig(config: UnitConfig) = unitConfigDao.save(config)

    suspend fun createSnapshot(batchId: Long, batch: BatchRecord) {
        val cfg = unitConfigDao.get().firstOrNull() ?: return
        val result = CostCalculator.calculate(
            sampleWeightGram = batch.sampleWeightGram,
            materialCost = batch.materialCost,
            processingCost = batch.processingCost,
            weightPerBoxGram = cfg.weightPerBoxGram,
            packagesPerBox = cfg.packagesPerBox
        )
        val number = snapshotDao.nextNumber(batchId)
        snapshotDao.insert(BatchSnapshot(
            batchId = batchId, snapshotNumber = number,
            productName = batch.productName, batchName = batch.batchName,
            sampleWeightGram = batch.sampleWeightGram,
            materialCost = batch.materialCost, processingCost = batch.processingCost,
            note = batch.note, createdAt = System.currentTimeMillis(),
            unitCostPerGram = result.unitCostPerGram,
            unitCostPerTon = result.unitCostPerTon,
            costPerBox = result.costPerBox, costPerPackage = result.costPerPackage
        ))
        snapshotDao.trimOld(batchId, 50)
    }

    fun getSnapshots(batchId: Long): Flow<List<BatchSnapshot>> = snapshotDao.getByBatch(batchId)

    fun getTrendPoints(productName: String, limit: Int = 20): Flow<List<TrendPoint>> {
        return combine(batchDao.getByProduct(productName), unitConfigDao.get()) { batches, cfg ->
            batches.take(limit).map { batch ->
                val c = cfg ?: UnitConfig(weightPerBoxGram = 5000.0, packagesPerBox = 10, weightPerPackageGram = 500.0, createdAt = 0L)
                val r = CostCalculator.calculate(
                    sampleWeightGram = batch.sampleWeightGram,
                    materialCost = batch.materialCost,
                    processingCost = batch.processingCost,
                    weightPerBoxGram = c.weightPerBoxGram,
                    packagesPerBox = c.packagesPerBox
                )
                TrendPoint(batch.createdAt, batch.productName, r.unitCostPerTon, r.costPerBox, batch.materialCost + batch.processingCost)
            }.reversed()
        }
    }

    data class TrendPoint(val date: Long, val productName: String, val unitCostPerTon: Double, val costPerBox: Double, val totalCost: Double)
}
