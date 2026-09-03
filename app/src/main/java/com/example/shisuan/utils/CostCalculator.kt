package com.example.shisuan.utils

import com.example.shisuan.domain.model.CostResult

object CostCalculator {
    fun calculate(
        sampleWeightGram: Double,
        materialCost: Double,
        processingCost: Double = 0.0,
        weightPerBoxGram: Double,
        packagesPerBox: Int
    ): CostResult {
        if (sampleWeightGram <= 0.0 || weightPerBoxGram <= 0.0 || packagesPerBox <= 0) {
            return CostResult(0.0, 0.0, 0.0, 0.0, 0.0)
        }
        val totalCost = materialCost + processingCost
        val unitCostPerGram = totalCost / sampleWeightGram
        val unitCostPerTon = unitCostPerGram * 1_000_000.0
        val boxesPerTon = 1_000_000.0 / weightPerBoxGram
        val costPerBox = unitCostPerGram * weightPerBoxGram
        val costPerPackage = costPerBox / packagesPerBox.toDouble()
        return CostResult(unitCostPerGram, unitCostPerTon, boxesPerTon, costPerBox, costPerPackage)
    }

    fun calcDifferential(current: Double, previous: Double?): Double? {
        return previous?.let { prev ->
            if (prev > 0.0) {
                val pct = (current - prev) / prev * 100.0
                val factor = 10.0 * 10.0  // 10^2
                kotlin.math.round(pct * factor) / factor
            } else null
        }
    }
}
