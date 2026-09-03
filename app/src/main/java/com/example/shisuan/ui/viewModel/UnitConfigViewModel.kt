package com.example.shisuan.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shisuan.data.database.UnitConfig
import com.example.shisuan.data.repository.CostRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UnitConfigViewModel(private val repo: CostRepository) : ViewModel() {
    val config: StateFlow<UnitConfig?> = repo.unitConfig.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun save(weightPerBoxGram: Double, packagesPerBox: Int) {
        viewModelScope.launch {
            val existing = repo.unitConfig.first()
            if (existing != null) {
                repo.saveUnitConfig(existing.copy(weightPerBoxGram = weightPerBoxGram, packagesPerBox = packagesPerBox, weightPerPackageGram = weightPerBoxGram / packagesPerBox.toDouble()))
            } else {
                repo.saveUnitConfig(UnitConfig(weightPerBoxGram = weightPerBoxGram, packagesPerBox = packagesPerBox, weightPerPackageGram = weightPerBoxGram / packagesPerBox.toDouble(), createdAt = System.currentTimeMillis()))
            }
        }
    }
}
