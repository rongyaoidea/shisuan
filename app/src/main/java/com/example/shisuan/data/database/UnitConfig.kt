package com.example.shisuan.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unit_config")
data class UnitConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val weightPerBoxGram: Double,
    val packagesPerBox: Int,
    val weightPerPackageGram: Double,
    val createdAt: Long
)
