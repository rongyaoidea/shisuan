package com.example.shisuan.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        UnitConfig::class,
        BatchRecord::class,
        Ingredient::class,
        BatchMaterial::class,
        BatchResult::class,
        BatchProblem::class,
        BatchSnapshot::class,
        OperationLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CostCalDatabase : RoomDatabase() {
    abstract fun batchDao(): BatchDao
    abstract fun unitConfigDao(): UnitConfigDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun batchMaterialDao(): BatchMaterialDao
    abstract fun batchResultDao(): BatchResultDao
    abstract fun batchProblemDao(): BatchProblemDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile private var INSTANCE: CostCalDatabase? = null
        fun getInstance(context: android.content.Context): CostCalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CostCalDatabase::class.java,
                    "costcal.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

@Dao
interface BatchDao {
    @Query("SELECT * FROM batch_record ORDER BY createdAt DESC")
    fun getAll(): Flow<List<BatchRecord>>

    @Query("SELECT * FROM batch_record WHERE productName = :productName ORDER BY createdAt DESC")
    fun getByProduct(productName: String): Flow<List<BatchRecord>>

    @Query("SELECT * FROM batch_record WHERE id = :id")
    suspend fun getById(id: Long): BatchRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: BatchRecord): Long

    @Update
    suspend fun update(batch: BatchRecord)

    @Delete
    suspend fun delete(batch: BatchRecord)

    @Query("SELECT COUNT(*) FROM batch_record WHERE productName = :productName")
    suspend fun countByProduct(productName: String): Int
}

@Dao
interface UnitConfigDao {
    @Query("SELECT * FROM unit_config LIMIT 1")
    fun get(): Flow<UnitConfig?>

    @Upsert
    suspend fun save(config: UnitConfig)
}

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredient ORDER BY name")
    fun getAll(): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredient WHERE category = :category ORDER BY name")
    fun getByCategory(category: String): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredient WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun search(query: String): Flow<List<Ingredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: Ingredient): Long

    @Update
    suspend fun update(ingredient: Ingredient)

    @Delete
    suspend fun delete(ingredient: Ingredient)
}

@Dao
interface BatchMaterialDao {
    @Query("SELECT * FROM batch_material WHERE batchId = :batchId ORDER BY id")
    fun getByBatch(batchId: Long): Flow<List<BatchMaterial>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(material: BatchMaterial): Long

    @Delete
    suspend fun delete(material: BatchMaterial)

    @Query("DELETE FROM batch_material WHERE batchId = :batchId")
    suspend fun deleteByBatch(batchId: Long)
}

@Dao
interface BatchResultDao {
    @Query("SELECT * FROM batch_result WHERE batchId = :batchId LIMIT 1")
    suspend fun getByBatch(batchId: Long): BatchResult?

    @Upsert
    suspend fun upsert(result: BatchResult)
}

@Dao
interface BatchProblemDao {
    @Query("SELECT * FROM batch_problem WHERE batchId = :batchId ORDER BY createdAt DESC")
    fun getByBatch(batchId: Long): Flow<List<BatchProblem>>

    @Query("SELECT * FROM batch_problem WHERE resolved = :resolved ORDER BY createdAt DESC")
    fun getAll(resolved: Boolean): Flow<List<BatchProblem>>

    @Insert
    suspend fun insert(problem: BatchProblem): Long

    @Update
    suspend fun update(problem: BatchProblem)
}

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM batch_snapshot WHERE batchId = :batchId ORDER BY createdAt DESC")
    fun getByBatch(batchId: Long): Flow<List<BatchSnapshot>>

    @Query("SELECT COALESCE(MAX(snapshotNumber), 0) + 1 FROM batch_snapshot WHERE batchId = :batchId")
    suspend fun nextNumber(batchId: Long): Int

    @Insert
    suspend fun insert(snapshot: BatchSnapshot): Long

    @Delete
    suspend fun delete(snapshot: BatchSnapshot)

    @Query("DELETE FROM batch_snapshot WHERE batchId = :batchId AND id NOT IN (SELECT id FROM batch_snapshot WHERE batchId = :batchId ORDER BY createdAt DESC LIMIT :keep)")
    suspend fun trimOld(batchId: Long, keep: Int)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM operation_log ORDER BY createdAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<OperationLog>>

    @Insert
    suspend fun insert(log: OperationLog): Long
}
