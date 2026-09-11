package com.example.shisuan.domain.usecase

import com.example.shisuan.data.database.BatchRecord
import com.example.shisuan.domain.model.CostResult
import com.example.shisuan.utils.CostCalculator
import javax.inject.Inject

/**
 * 批次成本计算用例：CostCalculator.calculate 的薄委托层。
 *
 * VM 保留原调用语义，仅把 JNI/回退细节收敛到此，供单测直接覆盖公式分支。
 * // TODO: 下一步迁移到 UseCase —— 将 ProductDetailViewModel 内联的
 * // materialCost sumOf + suggestedTonPrice/differential 也收拢到此。
 */
class CalculateBatchCostUseCase @Inject constructor() {
    operator fun invoke(
        batch: BatchRecord,
        materialCost: Double,
        weightPerBoxGram: Double,
        packagesPerBox: Int
    ): CostResult = CostCalculator.calculate(
        sampleWeightGram = batch.sampleWeightGram,
        materialCost = materialCost,
        processingCost = batch.processingCost,
        weightPerBoxGram = weightPerBoxGram,
        packagesPerBox = packagesPerBox,
        yieldRatePercent = batch.yieldRatePercent
    )
}
