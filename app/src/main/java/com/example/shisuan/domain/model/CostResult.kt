package com.example.shisuan.domain.model

data class CostResult(
    val unitCostPerGram: Double,
    val unitCostPerTon: Double,
    val boxesPerTon: Double,
    val costPerBox: Double,
    val costPerPackage: Double
)
