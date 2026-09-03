package com.example.shisuan.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shisuan.data.repository.CostRepository
import com.example.shisuan.utils.CostCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class StatsViewModel(private val repo: CostRepository) : ViewModel() {
    val config: StateFlow<com.example.shisuan.data.database.UnitConfig?> = repo.unitConfig.stateIn(viewModelScope, SharingStarted.Lazily, null)

    data class ProductStat(val productName: String, val batchCount: Int, val avgTonCost: Double, val maxTonCost: Double, val minTonCost: Double, val variation: Double)

    val products: StateFlow<List<ProductStat>> = repo.allBatches.map { batches ->
        batches.groupBy { it.productName }.map { (name, bs) ->
            val cfg = repo.unitConfig.first()
            val results = bs.map { b ->
                CostCalculator.calculate(b.sampleWeightGram, b.materialCost, b.processingCost, cfg?.weightPerBoxGram ?: 5000.0, cfg?.packagesPerBox ?: 10).unitCostPerTon
            }
            ProductStat(name, bs.size, results.average(), results.maxOrNull() ?: 0.0, results.minOrNull() ?: 0.0, (results.maxOrNull() ?: 0.0) - (results.minOrNull() ?: 0.0))
        }.sortedByDescending { it.avgTonCost }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
