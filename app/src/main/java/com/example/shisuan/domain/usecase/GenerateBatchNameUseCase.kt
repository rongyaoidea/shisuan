package com.example.shisuan.domain.usecase

import com.example.shisuan.data.repository.CostRepository
import javax.inject.Inject

/**
 * 批次名生成用例：日期前缀 + 同日最大序号 + 1（序号两位零填充）。
 *
 * 并发兜底不在此：VM 保存时捕获 SQLiteConstraintException 后 maxSeq+1 重试 1 次，
 * 依赖 (productId, batchName) 唯一索引（v9 迁移）做最终仲裁。
 * 命名规则变更只需改此处。
 */
class GenerateBatchNameUseCase @Inject constructor(
    private val repo: CostRepository
) {
    suspend operator fun invoke(productId: Long, date: String): String {
        val prefix = "$date-"
        // 直查批次名一列：生成序号无需读取全量批次对象
        val maxSeq = repo.getBatchNames(productId).asSequence()
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.removePrefix(prefix).toIntOrNull() }
            .maxOrNull() ?: 0
        return "$prefix${(maxSeq + 1).toString().padStart(2, '0')}"
    }
}
