package com.example.shisuan

import android.app.Application
import com.example.shisuan.data.database.CostCalDatabase
import com.example.shisuan.data.repository.CostRepository

class ShisuanApplication : Application() {
    val db by lazy { CostCalDatabase.getInstance(this) }
    val repo by lazy {
        CostRepository(
            batchDao = db.batchDao(),
            unitConfigDao = db.unitConfigDao(),
            ingredientDao = db.ingredientDao(),
            batchMaterialDao = db.batchMaterialDao(),
            batchResultDao = db.batchResultDao(),
            batchProblemDao = db.batchProblemDao(),
            snapshotDao = db.snapshotDao(),
            logDao = db.logDao()
        )
    }
}
