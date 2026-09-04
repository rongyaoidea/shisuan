package com.example.shisuan.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 自绘线条图标集
 *
 * 规范（Material Design outlined icon）：
 * - Viewport 24×24，内容控制在 20×20 安全区
 * - 笔画 2dp，圆头端点（Round cap），圆角连接（Round join）
 * - 线条由 moveTo/lineTo 构成，stroke 渲染，负空间传达形状
 *
 * 不使用 material-icons 扩展包、不使用 emoji。
 */

// ─────────── 基础线条图标 ───────────

/** 返回箭头 */
val ArrowBack: ImageVector
    get() {
        if (_arrowBack != null) return _arrowBack!!
        _arrowBack = ImageVector.Builder(
            name = "ShisuanArrowBack", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 12f)
                horizontalLineTo(5f)
                moveTo(11f, 5f)
                lineTo(4f, 12f)
                lineTo(11f, 19f)
            }
        }.build()
        return _arrowBack!!
    }
private var _arrowBack: ImageVector? = null

/** 加号 */
val Add: ImageVector
    get() {
        if (_add != null) return _add!!
        _add = ImageVector.Builder(
            name = "ShisuanAdd", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 5f)
                verticalLineTo(19f)
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }.build()
        return _add!!
    }
private var _add: ImageVector? = null

/** 删除（垃圾桶） */
val Delete: ImageVector
    get() {
        if (_delete != null) return _delete!!
        _delete = ImageVector.Builder(
            name = "ShisuanDelete", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 7f)
                horizontalLineTo(19f)
                moveTo(9f, 7f)
                verticalLineTo(5f)
                horizontalLineTo(15f)
                verticalLineTo(7f)
                moveTo(7f, 7f)
                lineTo(8f, 19f)
                horizontalLineTo(16f)
                lineTo(17f, 7f)
                moveTo(10f, 11f)
                verticalLineTo(15f)
                moveTo(14f, 11f)
                verticalLineTo(15f)
            }
        }.build()
        return _delete!!
    }
private var _delete: ImageVector? = null

/** 编辑（铅笔） */
val Edit: ImageVector
    get() {
        if (_edit != null) return _edit!!
        _edit = ImageVector.Builder(
            name = "ShisuanEdit", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 20f)
                horizontalLineTo(8f)
                lineTo(20f, 8f)
                lineTo(16f, 4f)
                lineTo(4f, 16f)
                close()
                moveTo(13f, 7f)
                lineTo(17f, 11f)
            }
        }.build()
        return _edit!!
    }
private var _edit: ImageVector? = null

/** 相机 */
val Camera: ImageVector
    get() {
        if (_camera != null) return _camera!!
        _camera = ImageVector.Builder(
            name = "ShisuanCamera", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 4f)
                horizontalLineTo(15f)
                lineTo(16.5f, 6f)
                horizontalLineTo(20f)
                curveTo(21.1f, 6f, 22f, 6.9f, 22f, 8f)
                verticalLineTo(18f)
                curveTo(22f, 19.1f, 21.1f, 20f, 20f, 20f)
                horizontalLineTo(4f)
                curveTo(2.9f, 20f, 2f, 19.1f, 2f, 18f)
                verticalLineTo(8f)
                curveTo(2f, 6.9f, 2.9f, 6f, 4f, 6f)
                horizontalLineTo(7.5f)
                close()
                moveTo(12f, 17f)
                curveTo(14.21f, 17f, 16f, 15.21f, 16f, 13f)
                curveTo(16f, 10.79f, 14.21f, 9f, 12f, 9f)
                curveTo(9.79f, 9f, 8f, 10.79f, 8f, 13f)
                curveTo(8f, 15.21f, 9.79f, 17f, 12f, 17f)
                close()
            }
        }.build()
        return _camera!!
    }
private var _camera: ImageVector? = null

/** 相册/图片 */
val Gallery: ImageVector
    get() {
        if (_gallery != null) return _gallery!!
        _gallery = ImageVector.Builder(
            name = "ShisuanGallery", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 5f)
                curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
                horizontalLineTo(5f)
                curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                verticalLineTo(19f)
                curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                close()
                moveTo(3f, 16f)
                lineTo(8f, 11f)
                lineTo(21f, 20f)
                moveTo(8.5f, 9f)
                curveTo(9.33f, 9f, 10f, 8.33f, 10f, 7.5f)
                curveTo(10f, 6.67f, 9.33f, 6f, 8.5f, 6f)
                curveTo(7.67f, 6f, 7f, 6.67f, 7f, 7.5f)
                curveTo(7f, 8.33f, 7.67f, 9f, 8.5f, 9f)
                close()
            }
        }.build()
        return _gallery!!
    }
private var _gallery: ImageVector? = null

/** 包装箱（产品/批次空状态） */
val Package: ImageVector
    get() {
        if (_package != null) return _package!!
        _package = ImageVector.Builder(
            name = "ShisuanPackage", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 8f)
                lineTo(12f, 3f)
                lineTo(3f, 8f)
                verticalLineTo(16f)
                lineTo(12f, 21f)
                lineTo(21f, 16f)
                close()
                moveTo(3f, 8f)
                lineTo(12f, 13f)
                lineTo(21f, 8f)
                moveTo(12f, 13f)
                verticalLineTo(21f)
            }
        }.build()
        return _package!!
    }
private var _package: ImageVector? = null

/** 烧瓶（配料空状态） */
val Flask: ImageVector
    get() {
        if (_flask != null) return _flask!!
        _flask = ImageVector.Builder(
            name = "ShisuanFlask", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 2f)
                horizontalLineTo(18f)
                moveTo(9f, 2f)
                verticalLineTo(7f)
                lineTo(3.5f, 19.5f)
                curveTo(3f, 20.5f, 3.7f, 22f, 4.9f, 22f)
                horizontalLineTo(19.1f)
                curveTo(20.3f, 22f, 21f, 20.5f, 20.5f, 19.5f)
                lineTo(15f, 7f)
                verticalLineTo(2f)
                moveTo(6f, 14f)
                horizontalLineTo(18f)
            }
        }.build()
        return _flask!!
    }
private var _flask: ImageVector? = null

/** 天平（成本计算） */
val Scale: ImageVector
    get() {
        if (_scale != null) return _scale!!
        _scale = ImageVector.Builder(
            name = "ShisuanScale", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3f)
                verticalLineTo(21f)
                moveTo(7f, 21f)
                horizontalLineTo(17f)
                moveTo(12f, 21f)
                verticalLineTo(9f)
                moveTo(9f, 9f)
                horizontalLineTo(15f)
                moveTo(5f, 11f)
                lineTo(2f, 16f)
                curveTo(2.6f, 17.5f, 4f, 18.5f, 5.5f, 18.5f)
                curveTo(7f, 18.5f, 8.4f, 17.5f, 9f, 16f)
                close()
                moveTo(19f, 11f)
                lineTo(16f, 16f)
                curveTo(16.6f, 17.5f, 18f, 18.5f, 19.5f, 18.5f)
                curveTo(21f, 18.5f, 22.4f, 17.5f, 23f, 16f)
                close()
            }
        }.build()
        return _scale!!
    }
private var _scale: ImageVector? = null

/** 产品罐（产品卡片图标） */
val Jar: ImageVector
    get() {
        if (_jar != null) return _jar!!
        _jar = ImageVector.Builder(
            name = "ShisuanJar", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 3f)
                horizontalLineTo(17f)
                verticalLineTo(6f)
                curveTo(17f, 8f, 18f, 9f, 18f, 11f)
                verticalLineTo(17f)
                curveTo(18f, 18.7f, 16.7f, 20f, 15f, 20f)
                horizontalLineTo(9f)
                curveTo(7.3f, 20f, 6f, 18.7f, 6f, 17f)
                verticalLineTo(11f)
                curveTo(6f, 9f, 7f, 8f, 7f, 6f)
                close()
                moveTo(6f, 12f)
                horizontalLineTo(18f)
                moveTo(9f, 6f)
                verticalLineTo(12f)
                moveTo(12f, 6f)
                verticalLineTo(12f)
                moveTo(15f, 6f)
                verticalLineTo(12f)
            }
        }.build()
        return _jar!!
    }
private var _jar: ImageVector? = null

/** 奶油（裱花袋造型） */
val Cream: ImageVector
    get() {
        if (_cream != null) return _cream!!
        _cream = ImageVector.Builder(
            name = "ShisuanCream", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 袋口弧 + 袋体 + 尖端
                moveTo(4f, 6f)
                curveTo(7f, 9f, 17f, 9f, 20f, 6f)
                lineTo(12f, 21f)
                close()
                // 袋身装饰弧
                moveTo(7f, 8.5f)
                curveTo(9f, 10.5f, 15f, 10.5f, 17f, 8.5f)
            }
        }.build()
        return _cream!!
    }
private var _cream: ImageVector? = null

/** 面包（拱顶 + 切面） */
val Bread: ImageVector
    get() {
        if (_bread != null) return _bread!!
        _bread = ImageVector.Builder(
            name = "ShisuanBread", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 拱顶 + 左右 + 底
                moveTo(4f, 11f)
                curveTo(4f, 5f, 20f, 5f, 20f, 11f)
                verticalLineTo(18f)
                horizontalLineTo(4f)
                close()
                // 切面线
                moveTo(4f, 14f)
                horizontalLineTo(20f)
            }
        }.build()
        return _bread!!
    }
private var _bread: ImageVector? = null

/** 蛋挞皮（碗形塔皮 + 蛋液面） */
val EggTart: ImageVector
    get() {
        if (_eggTart != null) return _eggTart!!
        _eggTart = ImageVector.Builder(
            name = "ShisuanEggTart", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 塔口（微弧）+ 两侧 + 底
                moveTo(4f, 9f)
                curveTo(4f, 7f, 20f, 7f, 20f, 9f)
                lineTo(17f, 19f)
                lineTo(7f, 19f)
                close()
                // 蛋液面
                moveTo(6f, 9.5f)
                curveTo(6f, 8f, 18f, 8f, 18f, 9.5f)
            }
        }.build()
        return _eggTart!!
    }
private var _eggTart: ImageVector? = null

/** 蛋糕（糕体 + 蜡烛 + 火焰） */
val Cake: ImageVector
    get() {
        if (_cake != null) return _cake!!
        _cake = ImageVector.Builder(
            name = "ShisuanCake", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 托盘
                moveTo(3f, 20f)
                horizontalLineTo(21f)
                // 糕体
                moveTo(5f, 20f)
                verticalLineTo(13f)
                horizontalLineTo(19f)
                verticalLineTo(20f)
                // 顶部奶油装饰弧
                moveTo(5f, 13f)
                curveTo(7f, 11.5f, 9f, 14.5f, 11f, 13f)
                curveTo(13f, 11.5f, 15f, 14.5f, 17f, 13f)
                curveTo(18f, 12.4f, 19f, 12.6f, 19f, 13f)
                // 蜡烛
                moveTo(12f, 13f)
                verticalLineTo(7f)
                // 火焰
                moveTo(12f, 7f)
                lineTo(10f, 4f)
                curveTo(10f, 2.5f, 14f, 2.5f, 14f, 4f)
                close()
            }
        }.build()
        return _cake!!
    }
private var _cake: ImageVector? = null

/** 黄油（块 + 分块切痕） */
val Butter: ImageVector
    get() {
        if (_butter != null) return _butter!!
        _butter = ImageVector.Builder(
            name = "ShisuanButter", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 黄油块
                moveTo(5f, 8f)
                horizontalLineTo(19f)
                verticalLineTo(19f)
                horizontalLineTo(5f)
                close()
                // 顶部厚度
                moveTo(5f, 8f)
                lineTo(7f, 5f)
                horizontalLineTo(17f)
                lineTo(19f, 8f)
                // 分块切痕
                moveTo(12f, 8f)
                verticalLineTo(19f)
            }
        }.build()
        return _butter!!
    }
private var _butter: ImageVector? = null

/** 饼干（圆饼 + 巧克力碎） */
val Cookie: ImageVector
    get() {
        if (_cookie != null) return _cookie!!
        _cookie = ImageVector.Builder(
            name = "ShisuanCookie", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 圆饼
                moveTo(5f, 12f)
                curveTo(5f, 7.5f, 7.5f, 5f, 12f, 5f)
                curveTo(16.5f, 5f, 19f, 7.5f, 19f, 12f)
                curveTo(19f, 16.5f, 16.5f, 19f, 12f, 19f)
                curveTo(7.5f, 19f, 5f, 16.5f, 5f, 12f)
                // 巧克力碎（小圆点）
                moveTo(9f, 9.5f)
                curveTo(9.4f, 9.5f, 9.4f, 9.9f, 9f, 9.9f)
                moveTo(15f, 10.5f)
                curveTo(15.4f, 10.5f, 15.4f, 10.9f, 15f, 10.9f)
                moveTo(12f, 14.5f)
                curveTo(12.4f, 14.5f, 12.4f, 14.9f, 12f, 14.9f)
            }
        }.build()
        return _cookie!!
    }
private var _cookie: ImageVector? = null

/** 日历（批次日期选择） */
val Calendar: ImageVector
    get() {
        if (_calendar != null) return _calendar!!
        _calendar = ImageVector.Builder(
            name = "ShisuanCalendar", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 6f)
                horizontalLineTo(20f)
                verticalLineTo(20f)
                horizontalLineTo(4f)
                close()
                moveTo(7f, 3f)
                verticalLineTo(8f)
                moveTo(17f, 3f)
                verticalLineTo(8f)
                moveTo(4f, 11f)
                horizontalLineTo(20f)
                moveTo(8f, 15f)
                verticalLineTo(17f)
                moveTo(12f, 15f)
                verticalLineTo(17f)
                moveTo(16f, 15f)
                verticalLineTo(17f)
            }
        }.build()
        return _calendar!!
    }
private var _calendar: ImageVector? = null

/**
 * 按分类关键词匹配产品/原料图标（烘焙行业）
 * 未命中时回退到通用罐图标
 */
fun categoryIcon(category: String): ImageVector {
    val c = category.lowercase()
    return when {
        c.contains("蛋挞") || c.contains("tart") -> EggTart
        c.contains("蛋糕") || c.contains("cake") -> Cake
        c.contains("面包") || c.contains("bread") || c.contains("吐司") -> Bread
        c.contains("黄油") || c.contains("butter") -> Butter
        c.contains("奶油") || c.contains("cream") || c.contains("淡奶油") -> Cream
        c.contains("饼干") || c.contains("cookie") || c.contains("曲奇") -> Cookie
        else -> Jar
    }
}
