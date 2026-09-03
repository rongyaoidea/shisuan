package com.example.shisuan.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batch_record")
data class BatchRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productName: String,
    val batchName: String,
    val sampleWeightGram: Double,
    val materialCost: Double,
    val processingCost: Double = 0.0,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
