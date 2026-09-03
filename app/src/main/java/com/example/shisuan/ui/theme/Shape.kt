package com.example.shisuan.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// ─────────── 圆角 Token ───────────

/** 按钮圆角 8px */
val ButtonShape: CornerBasedShape = RoundedCornerShape(8.dp)

/** 卡片圆角 16px */
val CardShape: CornerBasedShape = RoundedCornerShape(16.dp)

/** 大卡片圆角 20px */
val CardShapeLarge: CornerBasedShape = RoundedCornerShape(20.dp)

/** 胶囊形 — OCR 按钮/搜索栏 */
val PillShape: Shape = CircleShape

val ShisuanShapes: Shapes = Shapes(
    small = ButtonShape,
    medium = CardShape,
    large = CardShapeLarge,
)
