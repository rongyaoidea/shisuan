package com.example.shisuan.ui.viewModel

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.shisuan.data.repository.CostRepository

val LocalBatchViewModel = staticCompositionLocalOf<BatchViewModel> { error("No BatchViewModel provided") }
val LocalStatsViewModel = staticCompositionLocalOf<StatsViewModel> { error("No StatsViewModel provided") }
val LocalConfigViewModel = staticCompositionLocalOf<UnitConfigViewModel> { error("No UnitConfigViewModel provided") }
