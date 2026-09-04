package com.example.shisuan

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.shisuan.data.database.CostCalDatabase
import com.example.shisuan.data.database.MIGRATION_6_7
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Room 迁移测试（需在设备/模拟器上运行：connectedDebugAndroidTest）
 *
 * 重点验证 v6 → v7 —— 这是功能升级的关键迁移：
 * - BatchRecord 恢复加工费分项（packagingCost/laborCost/overheadCost）与出品率列
 * - Product 增加 targetMarginRate 列
 * - 废弃表 unit_config / batch_material 被删除
 *
 * 说明：早期迁移（v1→v6）开发时未开启 schema 导出，缺少历史 JSON 基线，
 * 暂无法在此框架下验证；自 v7 起每个新迁移都必须补充对应测试。
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

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
