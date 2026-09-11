package com.example.shisuan.domain.usecase

import com.example.shisuan.data.repository.CostRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 批次名生成用例：日期前缀 + 同日最大序号 + 1（序号两位零填充）。
 *
 * 并发兜底不在此：VM 保存时捕获 SQLiteConstraintException 后 maxSeq+1 重试 1 次，
 * 依赖 (productId, batchName) 唯一索引（v9 迁移）做最终仲裁。
 * // TODO: 下一步迁移到 UseCase —— VM 内的 generateBatchName 仅保留委托，
 * // 命名规则变更只改此处。
 */
class GenerateBatchNameUseCase @Inject constructor(
    private val repo: CostRepository
) {
    suspend operator fun invoke(productId: Long, date: String): String {
        val prefix = "$date-"
        val batches = repo.getBatchesByProduct(productId).first()
        val maxSeq = batches.asSequence()
            .map { it.batchName }
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.removePrefix(prefix).toIntOrNull() }
            .maxOrNull() ?: 0
        return "$prefix${(maxSeq + 1).toString().padStart(2, '0')}"
    }
}
