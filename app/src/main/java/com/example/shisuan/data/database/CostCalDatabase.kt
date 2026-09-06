package com.example.shisuan.data.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据库 - 版本 9
 * v2: 新增 Product 表和 BatchIngredient 表
 * v3: Product 增加包装规格字段（weightPerBoxGram/packagesPerBox/weightPerPackageGram）
 * v4: BatchRecord 移除 processingCost（纯物料成本，无加工费）
 * v5: BatchIngredient 增加 ratioPercent（比例输入原值，null=按克重）
 * v6: BatchIngredient 增加 ingredientSupplier（品牌冗余字段）
 * v7: BatchRecord 恢复加工费分项（packaging/labor/overhead）与出品率 yieldRatePercent；
 *     Product 增加 targetMarginRate；删除废弃表 unit_config / batch_material
 * v8: BatchSnapshot 增加 digest（唯一变更编号）与 (batchId, digest) 唯一索引 —— git 式版本链
 * v9: batch_record 增加 (productId, batchName) 唯一索引（批次号并发兜底）；
 *     删除自始至终无 UI 入口的 batch_problem 表
 */
@Database(
    entities = [
        Product::class,
        BatchRecord::class,
        BatchIngredient::class,
        Ingredient::class,
        BatchResult::class,
        BatchSnapshot::class,
        OperationLog::class
    ],
    version = 9,
    // 导出 schema 以支持迁移测试：schema JSON 位于 app/schemas/
    exportSchema = true
)
abstract class CostCalDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun batchDao(): BatchDao
    abstract fun batchIngredientDao(): BatchIngredientDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun batchResultDao(): BatchResultDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun logDao(): LogDao

    companion object {
        /** 当前应用支持的 schema 版本；恢复备份时用于拒绝来自更高版本的文件 */
        const val SUPPORTED_DB_VERSION = 9

        @Volatile private var INSTANCE: CostCalDatabase? = null
        fun getInstance(context: android.content.Context): CostCalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CostCalDatabase::class.java,
                    "costcal.db"
                )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                    MIGRATION_7_8, MIGRATION_8_9
                )
                .build().also { INSTANCE = it }
            }
        }
    }
}

// ============ DAO 层 ============

@Dao
interface ProductDao {
    @Query("SELECT * FROM product WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getAllActive(): Flow<List<Product>>

    @Query("SELECT * FROM product WHERE id = :id")
    fun getById(id: Long): Flow<Product?>

    // insert 用 ABORT（默认）而非 REPLACE：REPLACE 对已有 id 会「先删后插」，
    // 触发 product 的 ON DELETE CASCADE 级联清空该产品全部批次 —— 数据安全红线
    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("UPDATE product SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)
}

/**
 * 批次 + 其原料明细（Room 关联查询，一次性取回）
 *
 * 供产品详情页使用：避免在 Flow 变换中对每个批次单独查询配料（N+1 查询放大）。
 * Room 会监听 batch_ingredient 表，配料增删时自动重新发射。
 */
data class BatchWithIngredients(
    @Embedded val batch: BatchRecord,
    @Relation(parentColumn = "id", entityColumn = "batchId")
    val ingredients: List<BatchIngredient>
)

@Dao
interface BatchDao {
    @Query("SELECT * FROM batch_record WHERE productId = :productId ORDER BY createdAt DESC")
    fun getByProduct(productId: Long): Flow<List<BatchRecord>>

    /**
     * 一次查出某产品的所有批次及其配料明细（单查询替代 N 次配料查询）
     */
    @Transaction
    @Query("SELECT * FROM batch_record WHERE productId = :productId ORDER BY createdAt DESC")
    fun getBatchesWithIngredients(productId: Long): Flow<List<BatchWithIngredients>>

    @Query("SELECT * FROM batch_record WHERE id = :id")
    fun getById(id: Long): Flow<BatchRecord?>

    // ABORT 语义见 ProductDao.insert 注释；批次号冲突由 (productId, batchName) 唯一索引拦截
    @Insert
    suspend fun insert(batch: BatchRecord): Long

    @Update
    suspend fun update(batch: BatchRecord)

    @Delete
    suspend fun delete(batch: BatchRecord)
}

@Dao
interface BatchIngredientDao {
    @Query("SELECT * FROM batch_ingredient WHERE batchId = :batchId")
    fun getByBatch(batchId: Long): Flow<List<BatchIngredient>>

    @Insert
    suspend fun insertAll(ingredients: List<BatchIngredient>)

    @Update
    suspend fun update(ingredient: BatchIngredient)

    @Delete
    suspend fun delete(ingredient: BatchIngredient)

    @Query("DELETE FROM batch_ingredient WHERE batchId = :batchId")
    suspend fun deleteByBatch(batchId: Long)
}

/**
 * 原料 + 使用频次（Room 关联查询，一次性取回）
 *
 * 按 (名称, 品牌) 匹配而非 ingredientId：批次明细的 ingredientId 为可空关联，
 * 而名称与品牌是录入时必带的冗余字段，统计口径更完整。
 */
data class IngredientWithUseCount(
    @Embedded val ingredient: Ingredient,
    /** 使用过该原料的批次数（同批次多次使用只计一次） */
    val useCount: Int
)

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredient WHERE isActive = 1 ORDER BY name")
    fun getAllActive(): Flow<List<Ingredient>>

    /**
     * 全部活跃原料，按使用频次降序（常用前置），频次同值按名称升序。
     * 配料库列表与批次录入的原料选择器共用，让高频原料唾手可得。
     */
    @Query("""
        SELECT i.*, (
            SELECT COUNT(DISTINCT bi.batchId) FROM batch_ingredient bi
            WHERE bi.ingredientName = i.name AND bi.ingredientSupplier = i.supplier
        ) AS useCount
        FROM ingredient i
        WHERE i.isActive = 1
        ORDER BY useCount DESC, i.name
    """)
    fun getAllActiveWithUseCount(): Flow<List<IngredientWithUseCount>>

    @Query("SELECT * FROM ingredient WHERE id = :id")
    suspend fun getById(id: Long): Ingredient?

    /** 按名称+品牌查活跃原料（配料库去重键：同名同品牌共享一条记录，成本更新最新值） */
    @Query("SELECT * FROM ingredient WHERE name = :name AND supplier = :brand AND isActive = 1 LIMIT 1")
    suspend fun getByNameAndBrand(name: String, brand: String): Ingredient?

    @Insert
    suspend fun insert(ingredient: Ingredient): Long

    @Update
    suspend fun update(ingredient: Ingredient)

    @Delete
    suspend fun delete(ingredient: Ingredient)
}

@Dao
interface BatchResultDao {
    @Query("SELECT * FROM batch_result WHERE batchId = :batchId")
    fun getByBatch(batchId: Long): Flow<BatchResult?>

    @Query("SELECT * FROM batch_result WHERE batchId = :batchId LIMIT 1")
    suspend fun getByBatchOnce(batchId: Long): BatchResult?

    @Insert
    suspend fun insert(result: BatchResult)

    @Update
    suspend fun update(result: BatchResult)
}

@Dao
interface SnapshotDao {
    /** 按版本号倒序的完整历史时间线 */
    @Query("SELECT * FROM batch_snapshot WHERE batchId = :batchId ORDER BY version DESC")
    fun getByBatch(batchId: Long): Flow<List<BatchSnapshot>>

    /** 内容寻址查重：同一批次同内容只存一份（git 语义） */
    @Query("SELECT * FROM batch_snapshot WHERE batchId = :batchId AND digest = :digest LIMIT 1")
    suspend fun getByDigest(batchId: Long, digest: String): BatchSnapshot?

    /** 该批次已存版本数，用于生成下一个递增 version */
    @Query("SELECT MAX(version) FROM batch_snapshot WHERE batchId = :batchId")
    suspend fun maxVersion(batchId: Long): Int?

    @Insert
    suspend fun insert(snapshot: BatchSnapshot)
}

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: OperationLog)
}

/**
 * 数据库迁移：v1 → v2
 * - 新增 Product 表
 * - 修改 BatchRecord：添加 productId，迁移 productName → Product
 * - 新增 BatchIngredient 表
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 创建 Product 表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS product (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                category TEXT NOT NULL DEFAULT '',
                description TEXT NOT NULL DEFAULT '',
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())

        // 2. 从 batch_record 提取唯一产品名并插入 Product
        database.execSQL("""
            INSERT INTO product (name, createdAt, updatedAt)
            SELECT DISTINCT productName, MIN(createdAt), MAX(updatedAt)
            FROM batch_record
            GROUP BY productName
        """.trimIndent())

        // 3. 创建 BatchIngredient 表。
        //    注意顺序：必须在此之前创建并完成第 4 步旧 materialCost 数据迁移 ——
        //    一旦执行完第 7 步的 DROP/RENAME，materialCost 列就不复存在，
        //    再引用它会让迁移在语句预编译期直接失败（v1 老用户升级即崩溃的修复）。
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS batch_ingredient (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                batchId INTEGER NOT NULL,
                ingredientName TEXT NOT NULL,
                ingredientId INTEGER,
                weight REAL NOT NULL,
                unitPrice REAL NOT NULL,
                priceUnit TEXT NOT NULL DEFAULT '元/kg',
                totalCost REAL NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(batchId) REFERENCES batch_record(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_batch_ingredient_batchId ON batch_ingredient(batchId)")

        // 4. 将旧的 materialCost 转为单条原料记录（兼容处理）。
        //    过滤 sampleWeightGram <= 0 的脏数据，避免除零产生 NULL 违反 NOT NULL 约束。
        database.execSQL("""
            INSERT INTO batch_ingredient (batchId, ingredientName, weight, unitPrice, totalCost)
            SELECT id, '原料总成本(迁移)', sampleWeightGram, materialCost / sampleWeightGram, materialCost
            FROM batch_record WHERE materialCost > 0 AND sampleWeightGram > 0
        """.trimIndent())

        // 5. 创建新的 batch_record 表结构
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS batch_record_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                productId INTEGER NOT NULL,
                batchName TEXT NOT NULL,
                sampleWeightGram REAL NOT NULL,
                processingCost REAL NOT NULL DEFAULT 0.0,
                note TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(productId) REFERENCES product(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_batch_record_productId ON batch_record_new(productId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_batch_record_createdAt ON batch_record_new(createdAt)")

        // 6. 迁移数据：batch_record → batch_record_new，关联 productId
        database.execSQL("""
            INSERT INTO batch_record_new (id, productId, batchName, sampleWeightGram, processingCost, note, createdAt, updatedAt)
            SELECT br.id, p.id, br.batchName, br.sampleWeightGram, br.processingCost, br.note, br.createdAt, br.updatedAt
            FROM batch_record br
            INNER JOIN product p ON br.productName = p.name
        """.trimIndent())

        // 7. 删除旧表，重命名新表
        database.execSQL("DROP TABLE batch_record")
        database.execSQL("ALTER TABLE batch_record_new RENAME TO batch_record")
    }
}

/**
 * 数据库迁移：v2 → v3
 * Product 增加包装规格字段（每箱克数/每箱包数/每包克数）
 * 从 unit_config 取最新记录填充，无记录时用默认值
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 增加三个包装规格列
        database.execSQL("ALTER TABLE product ADD COLUMN weightPerBoxGram REAL NOT NULL DEFAULT 5000.0")
        database.execSQL("ALTER TABLE product ADD COLUMN packagesPerBox INTEGER NOT NULL DEFAULT 20")
        database.execSQL("ALTER TABLE product ADD COLUMN weightPerPackageGram REAL NOT NULL DEFAULT 250.0")

        // 2. 从 unit_config 取最新记录更新所有产品的包装规格
        database.execSQL("""
            UPDATE product SET
                weightPerBoxGram = COALESCE(
                    (SELECT weightPerBoxGram FROM unit_config ORDER BY createdAt DESC LIMIT 1),
                    5000.0
                ),
                packagesPerBox = COALESCE(
                    (SELECT packagesPerBox FROM unit_config ORDER BY createdAt DESC LIMIT 1),
                    20
                ),
                weightPerPackageGram = COALESCE(
                    (SELECT weightPerPackageGram FROM unit_config ORDER BY createdAt DESC LIMIT 1),
                    250.0
                )
        """.trimIndent())
    }
}

/**
 * 数据库迁移：v3 → v4
 * BatchRecord 移除 processingCost 列（纯物料成本，无加工费）
 * SQLite 不支持直接删列，采用「建新表-拷贝-删旧表-重命名」模式
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 创建无 processingCost 的新表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS batch_record_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                productId INTEGER NOT NULL,
                batchName TEXT NOT NULL,
                sampleWeightGram REAL NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(productId) REFERENCES product(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // 2. 拷贝数据（去掉 processingCost 列）
        database.execSQL("""
            INSERT INTO batch_record_new (id, productId, batchName, sampleWeightGram, note, createdAt, updatedAt)
            SELECT id, productId, batchName, sampleWeightGram, note, createdAt, updatedAt
            FROM batch_record
        """.trimIndent())

        // 3. 删除旧表，重命名新表
        database.execSQL("DROP TABLE batch_record")
        database.execSQL("ALTER TABLE batch_record_new RENAME TO batch_record")

        // 4. 重建索引（Room 按 index_batch_record_* 校验，必须在重命名后重建）
        database.execSQL("CREATE INDEX IF NOT EXISTS index_batch_record_productId ON batch_record(productId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_batch_record_createdAt ON batch_record(createdAt)")
    }
}

/**
 * 数据库迁移：v4 → v5
 * BatchIngredient 增加 ratioPercent 列（比例输入原值，null=按克重输入）
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE batch_ingredient ADD COLUMN ratioPercent REAL")
    }
}

/**
 * 数据库迁移：v5 → v6
 * BatchIngredient 增加 ingredientSupplier 列（品牌冗余，区分同款不同品牌）
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE batch_ingredient ADD COLUMN ingredientSupplier TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * 数据库迁移：v6 → v7
 *
 * 1. Product 增加 targetMarginRate（目标毛利率，用于建议出厂价）
 * 2. BatchRecord 恢复加工费分项（packagingCost / laborCost / overheadCost）
 *    与出品率 yieldRatePercent（成品/投料折算）
 * 3. 删除废弃表 unit_config 与 batch_material（v3 起产品级绑定包装规格，
 *    batch_material 由 batch_ingredient 替代）
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE product ADD COLUMN targetMarginRate REAL NOT NULL DEFAULT 0.0")

        database.execSQL("ALTER TABLE batch_record ADD COLUMN packagingCost REAL NOT NULL DEFAULT 0.0")
        database.execSQL("ALTER TABLE batch_record ADD COLUMN laborCost REAL NOT NULL DEFAULT 0.0")
        database.execSQL("ALTER TABLE batch_record ADD COLUMN overheadCost REAL NOT NULL DEFAULT 0.0")
        database.execSQL("ALTER TABLE batch_record ADD COLUMN yieldRatePercent REAL")

        database.execSQL("DROP TABLE IF EXISTS unit_config")
        database.execSQL("DROP TABLE IF EXISTS batch_material")
    }
}

/**
 * 数据库迁移：v7 → v8
 * BatchSnapshot 增加 digest（唯一变更编号）列与 (batchId, digest) 唯一索引，
 * 支撑 git 式版本链：内容寻址去重、按编号定位、可回溯恢复。
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE batch_snapshot ADD COLUMN digest TEXT NOT NULL DEFAULT ''")
        // v7 存量快照没有内容指纹：先回填基于行 id 的唯一占位值再建唯一索引。
        // 否则同一批次的多条快照 digest 同为 ''，CREATE UNIQUE INDEX 直接失败。
        database.execSQL("UPDATE batch_snapshot SET digest = 'legacy-' || id")
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_batch_snapshot_batchId_digest " +
                "ON batch_snapshot(batchId, digest)"
        )
    }
}

/**
 * 数据库迁移：v8 → v9
 * 1. batch_record 增加 (productId, batchName) 唯一索引，为批次号「先查后插」
 *    的并发窗口兜底；建索引前把历史重名批次保留最早一条、其余追加 id 后缀
 * 2. 删除 batch_problem 表 —— 自 v1 起无任何 UI 入口，从未产生用户数据
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 重名批次去重：NOT IN 每组最小 id 即「组内除第一条外的重复行」
        database.execSQL(
            "UPDATE batch_record SET batchName = batchName || '#' || id " +
                "WHERE id NOT IN " +
                "(SELECT MIN(id) FROM batch_record GROUP BY productId, batchName)"
        )
        // 2. 唯一索引（与 BatchRecord 实体声明一致，Room 校验 schema 用）
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_batch_record_productId_batchName " +
                "ON batch_record(productId, batchName)"
        )
        // 3. 移除无 UI 的表
        database.execSQL("DROP TABLE IF EXISTS batch_problem")
    }
}
