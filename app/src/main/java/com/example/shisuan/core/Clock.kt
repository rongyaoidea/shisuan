package com.example.shisuan.core

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 可注入的时间源：替代散落的 System.currentTimeMillis()，便于单测伪造时间。
 *
 * Entities 默认值保持 System.currentTimeMillis() 不动（避免 Room 迁移爆炸），
 * 仅 Repository/VM 新增记录时优先用 clock.now()（示范见 CostRepository）。
 */
interface Clock {
    fun now(): Long
}

/** 生产实现：系统时钟 */
@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun now(): Long = System.currentTimeMillis()
}
