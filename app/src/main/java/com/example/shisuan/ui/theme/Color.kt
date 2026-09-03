package com.example.shisuan.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─────────── 品牌色板 ───────────

/** 主色 Rausch — 仅用于主行动按钮（FAB、保存、入库、添加） */
val Rausch = Color(0xFFFF385C)

/** 主色激活态 — 按下状态 */
val RauschPressed = Color(0xFFE00B41)

/** 主色禁用态 — 禁用按钮背景 */
val RauschDisabled = Color(0xFFFFD1DA)

// ─────────── 文字色 ───────────

/** Ink — 主标题 */
val Ink = Color(0xFF222222)

/** Body — 正文 */
val Body = Color(0xFF3F3F3F)

/** Foggy — 辅助文字 */
val Foggy = Color(0xFF767676)

// ─────────── 背景色 ───────────

/** Canvas — 页面背景 */
val Canvas = Color(0xFFFFFFFF)

/** SoftBg — 卡片/区域背景 */
val SoftBg = Color(0xFFF7F7F7)

// ─────────── 语义色 ───────────

/** 成本下降 */
val SuccessGreen = Color(0xFF0A8754)

/** 成本上升 */
val WarningOrange = Color(0xFFE8830A)

/** 删除/危险 */
val DangerRed = Color(0xFFD00000)

// ─────────── MaterialTheme ColorScheme ───────────

val ShisuanColorScheme = lightColorScheme(
    primary = Rausch,
    onPrimary = Color.White,
    primaryContainer = RauschDisabled,
    onPrimaryContainer = RauschPressed,
    secondary = Rausch,
    onSecondary = Color.White,
    surface = Canvas,
    onSurface = Ink,
    surfaceVariant = SoftBg,
    onSurfaceVariant = Foggy,
    background = Canvas,
    onBackground = Ink,
    outline = SoftBg,
    error = DangerRed,
    onError = Color.White,
)
