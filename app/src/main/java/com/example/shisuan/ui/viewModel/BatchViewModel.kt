package com.example.shisuan.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shisuan.data.database.*
import com.example.shisuan.data.repository.CostRepository
import com.example.shisuan.domain.model.CostResult
import com.example.shisuan.utils.CostCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class BatchViewModel(private val repo: CostRepository) : ViewModel() {
    private val _selectedBatchId = MutableStateFlow<Long?>(null)
    val selectedBatchId = _selectedBatchId.asStateFlow()

    val batches: StateFlow<List<BatchRecord>> = repo.allBatches.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val config: StateFlow<UnitConfig?> = repo.unitConfig.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val currentCostResult: StateFlow<CostResult?> = combine(repo.unitConfig, _selectedBatchId.flatMapLatest { id ->
        if (id == null) flow { emit(null) } else repo.allBatches.map { batches -> batches.find { it.id == id } }
    }) { cfg, batch ->
        if (batch == null || cfg == null) return@combine null
        CostCalculator.calculate(batch.sampleWeightGram, batch.materialCost, batch.processingCost, cfg.weightPerBoxGram, cfg.packagesPerBox)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val previousTonCost: StateFlow<Double?> = combine(repo.unitConfig, _selectedBatchId.flatMapLatest { id ->
        if (id == null) flow { emit(null) } else repo.allBatches.map { batches -> batches.find { it.id == id } }
    }) { cfg, batch ->
        if (batch == null || cfg == null) return@combine null
        CostCalculator.calculate(batch.sampleWeightGram, batch.materialCost, batch.processingCost, cfg.weightPerBoxGram, cfg.packagesPerBox).unitCostPerTon
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun selectBatch(id: Long) { _selectedBatchId.value = id }
    fun clearSelection() { _selectedBatchId.value = null }

    fun saveBatch(productName: String, batchName: String, sampleWeightGram: String, materialCost: String, processingCost: String = "0", note: String = "") {
        viewModelScope.launch {
            val current = _selectedBatchId.value
            val now = System.currentTimeMillis()
            if (current != null) {
                val existing = batches.first().firstOrNull { it.id == current }
                if (existing != null) {
                    repo.updateBatch(existing.copy(productName = productName, batchName = batchName,
                        sampleWeightGram = sampleWeightGram.toDoubleOrNull() ?: 0.0,
                        materialCost = materialCost.toDoubleOrNull() ?: 0.0,
                        processingCost = processingCost.toDoubleOrNull() ?: 0.0, note = note, updatedAt = now))
                    return@launch
                }
            }
            repo.insertBatch(BatchRecord(productName = productName, batchName = batchName,
                sampleWeightGram = sampleWeightGram.toDoubleOrNull() ?: 0.0,
                materialCost = materialCost.toDoubleOrNull() ?: 0.0,
                processingCost = processingCost.toDoubleOrNull() ?: 0.0, note = note, createdAt = now, updatedAt = now))
        }
    }

    fun deleteBatch(batch: BatchRecord) { viewModelScope.launch { repo.deleteBatch(batch) } }

    val snapshots: StateFlow<List<BatchSnapshot>> = _selectedBatchId.flatMapLatest { id ->
        if (id == null) flow { emit(emptyList()) } else repo.getSnapshots(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val problems: StateFlow<List<BatchProblem>> = _selectedBatchId.flatMapLatest { id ->
        if (id == null) flow { emit(emptyList()) } else repo.getProblems(id)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addProblem(batchId: Long, category: String, severity: String, description: String, cause: String? = null, solution: String? = null) {
        viewModelScope.launch { repo.addProblem(BatchProblem(batchId = batchId, category = category, severity = severity, description = description, cause = cause, solution = solution, resolved = false, createdAt = System.currentTimeMillis())) }
    }

    fun toggleProblem(problem: BatchProblem, resolved: Boolean) {
        viewModelScope.launch { repo.updateProblem(problem.copy(resolved = resolved, resolvedAt = if (resolved) System.currentTimeMillis() else null)) }
    }

    fun saveResult(batchId: Long, texture: String?, color: String?, aroma: String?, taste: String?,
                   appearance: String?, pHValue: Double?, brixDegree: Double?, yieldRate: Double?,
                   packagingResult: String?, overallRating: Int?) {
        viewModelScope.launch { repo.saveResult(BatchResult(batchId = batchId, texture = texture, color = color, aroma = aroma,
            taste = taste, appearance = appearance, pHValue = pHValue, brixDegree = brixDegree, yieldRate = yieldRate,
            packagingResult = packagingResult, overallRating = overallRating, recordedAt = System.currentTimeMillis())) }
    }
}
