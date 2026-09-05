package com.example.shisuan

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.shisuan.data.database.CostCalDatabase
import com.example.shisuan.data.database.MIGRATION_6_7
import com.example.shisuan.data.database.MIGRATION_7_8
import com.example.shisuan.data.database.MIGRATION_8_9
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Room 迁移测试（需在设备/模拟器上运行：connectedDebugAndroidTest）
 *
 * 覆盖 v6→v7 / v7→v8 / v8→v9。
 * 说明：早期迁移（v1→v6）开发时未开启 schema 导出，缺少历史 JSON 基线，
 * 暂无法在此框架下验证；自 v7 起每个新迁移都必须补充对应测试。
 * 注意：v9 迁移测试需要先执行一次构建，让 KSP 导出 app/schemas/9.json 后才能通过。
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CostCalDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun `migrate 6 to 7 - 新列取默认值且废弃表被删除`() {
        // 1. 按 v6 schema 建库并插入数据
        var db = helper.createDatabase(TEST_DB, 6)
        db.execSQL(
            "INSERT INTO product (name, category, description, isActive, " +
                "weightPerBoxGram, packagesPerBox, weightPerPackageGram, createdAt, updatedAt) " +
                "VALUES ('草莓酱', '果酱', '', 1, 5000.0, 20, 250.0, 0, 0)"
        )
        db.execSQL(
            "INSERT INTO batch_record (productId, batchName, sampleWeightGram, note, createdAt, updatedAt) " +
                "VALUES (1, '2026-09-03-01', 1000.0, '', 0, 0)"
        )
        db.close()

        // 2. 执行 v6 → v7 迁移并校验 schema
        db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        // 3. 加工费分项默认 0，出品率为 null（不折算）
        db.query(
            "SELECT packagingCost, laborCost, overheadCost, yieldRatePercent FROM batch_record"
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0.0, c.getDouble(0), 0.0)
            assertEquals(0.0, c.getDouble(1), 0.0)
            assertEquals(0.0, c.getDouble(2), 0.0)
            assertTrue(c.isNull(3))
        }

        // 4. 毛利率默认 0（不展示报价）
        db.query("SELECT targetMarginRate FROM product").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0.0, c.getDouble(0), 0.0)
        }

        // 5. 废弃表已删除
        db.query(
            "SELECT count(*) FROM sqlite_master WHERE type='table' " +
                "AND name IN ('unit_config','batch_material')"
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun `migrate 7 to 8 - digest 回填且同批次多条快照不冲突`() {
        // 1. 按 v7 schema 建库：同一批次两条快照（v7 无 digest 列）
        var db = helper.createDatabase(TEST_DB, 7)
        db.execSQL(
            "INSERT INTO product (name, category, description, isActive, " +
                "weightPerBoxGram, packagesPerBox, weightPerPackageGram, targetMarginRate, createdAt, updatedAt) " +
                "VALUES ('草莓酱', '果酱', '', 1, 5000.0, 20, 250.0, 0.0, 0, 0)"
        )
        db.execSQL(
            "INSERT INTO batch_record (productId, batchName, sampleWeightGram, " +
                "packagingCost, laborCost, overheadCost, note, createdAt, updatedAt) " +
                "VALUES (1, '2026-09-03-01', 1000.0, 0, 0, 0, '', 0, 0)"
        )
        db.execSQL(
            "INSERT INTO batch_snapshot (batchId, snapshotData, version, createdAt) VALUES (1, 'A', 1, 0)"
        )
        db.execSQL(
            "INSERT INTO batch_snapshot (batchId, snapshotData, version, createdAt) VALUES (1, 'B', 2, 0)"
        )
        db.close()

        // 2. 执行迁移（内部先回填 digest 再建唯一索引，否则同批次两条 '' 冲突）
        db = helper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)

        // 3. 回填值互不相同且非空（legacy-<id> 占位）
        db.query("SELECT digest FROM batch_snapshot ORDER BY id").use { c ->
            val digests = mutableListOf<String>()
            while (c.moveToNext()) digests.add(c.getString(0))
            assertEquals(2, digests.size)
            assertEquals(2, digests.toSet().size)
            assertTrue(digests.all { it.startsWith("legacy-") })
        }
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun `migrate 8 to 9 - 重名批次去重建唯一索引并删除问题表`() {
        // 1. 按 v8 schema 建库：同产品两条同名批次 + 一条 batch_problem
        var db = helper.createDatabase(TEST_DB, 8)
        db.execSQL(
            "INSERT INTO product (name, category, description, isActive, " +
                "weightPerBoxGram, packagesPerBox, weightPerPackageGram, targetMarginRate, createdAt, updatedAt) " +
                "VALUES ('草莓酱', '果酱', '', 1, 5000.0, 20, 250.0, 0.0, 0, 0)"
        )
        db.execSQL(
            "INSERT INTO batch_record (productId, batchName, sampleWeightGram, " +
                "packagingCost, laborCost, overheadCost, note, createdAt, updatedAt) " +
                "VALUES (1, '2026-09-01-01', 1000.0, 0, 0, 0, '', 0, 0)"
        )
        db.execSQL(
            "INSERT INTO batch_record (productId, batchName, sampleWeightGram, " +
                "packagingCost, laborCost, overheadCost, note, createdAt, updatedAt) " +
                "VALUES (1, '2026-09-01-01', 800.0, 0, 0, 0, '', 0, 0)"
        )
        db.execSQL(
            "INSERT INTO batch_problem (batchId, category, description, resolved, createdAt) " +
                "VALUES (1, '外观', '颜色偏深', 0, 0)"
        )
        db.close()

        // 2. 执行迁移
        db = helper.runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)

        // 3. 重名批次被重命名，唯一索引生效
        db.query("SELECT batchName FROM batch_record ORDER BY id").use { c ->
            val names = mutableListOf<String>()
            while (c.moveToNext()) names.add(c.getString(0))
            assertEquals(2, names.size)
            assertTrue(names[0] != names[1])
        }

        // 4. batch_problem 已删除
        db.query(
            "SELECT count(*) FROM sqlite_master WHERE type='table' AND name = 'batch_problem'"
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
