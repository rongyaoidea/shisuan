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

// ─────────── 食品品类图标 ───────────
//
// 绘制规范在基础规范之上补充（调研 Lucide 1.41 / Material Symbols 线性图标所得）：
// - 光学补偿：圆形、蛋形、斜向元素视觉上比同尺寸方形小，允许略微超出 20×20 安全区
//   （如 Egg 上下顶到 3/21），方形直边元素则内收
// - 特征线定物：同形不同物靠 1-2 条点睛线区分（Milk 的波浪液面、Can 的顶面拉环、
//   Jam 的盖布结+果粒 vs Jar 的竖条纹）
// - 简笔约束：单图标 2~5 个子路径，轮廓 + 特征线；圆点用「0.01 短线段 + Round cap」表达
// - 参考：Lucide（ISC License）milk/egg/cup-soda/candy/wheat 等图标的结构比例

/** 果酱罐（盖布罩口 + 蝶结 + 果粒） */
val Jam: ImageVector
    get() {
        if (_jam != null) return _jam!!
        _jam = ImageVector.Builder(
            name = "ShisuanJam", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 罐身（圆底）
                moveTo(7f, 9f)
                verticalLineTo(17.5f)
                curveTo(7f, 19.4f, 8.6f, 20.5f, 10.5f, 20.5f)
                horizontalLineTo(13.5f)
                curveTo(15.4f, 20.5f, 17f, 19.4f, 17f, 17.5f)
                verticalLineTo(9f)
                // 盖布（罩口拱形）
                moveTo(5.5f, 9f)
                curveTo(5.5f, 6.3f, 8f, 4.8f, 12f, 4.8f)
                curveTo(16f, 4.8f, 18.5f, 6.3f, 18.5f, 9f)
                // 蝶结
                moveTo(12f, 4.8f)
                lineTo(11f, 3f)
                lineTo(13f, 3f)
                close()
                // 果粒
                moveTo(10f, 14f)
                lineTo(10f, 14.01f)
                moveTo(14f, 16f)
                lineTo(14f, 16.01f)
            }
        }.build()
        return _jam!!
    }
private var _jam: ImageVector? = null

/** 酱料瓶（尖盖挤压瓶 + 标签线） */
val Sauce: ImageVector
    get() {
        if (_sauce != null) return _sauce!!
        _sauce = ImageVector.Builder(
            name = "ShisuanSauce", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 尖盖
                moveTo(10f, 2f)
                horizontalLineTo(14f)
                moveTo(10f, 2f)
                verticalLineTo(5f)
                moveTo(14f, 2f)
                verticalLineTo(5f)
                // 瓶肩台
                moveTo(8f, 5f)
                horizontalLineTo(16f)
                // 圆肩瓶身
                moveTo(8f, 5f)
                curveTo(5.5f, 7f, 5f, 9f, 5f, 11f)
                verticalLineTo(18f)
                curveTo(5f, 19.7f, 6.3f, 21f, 8f, 21f)
                horizontalLineTo(16f)
                curveTo(17.7f, 21f, 19f, 19.7f, 19f, 18f)
                verticalLineTo(11f)
                curveTo(19f, 9f, 18.5f, 7f, 16f, 5f)
                // 标签线
                moveTo(5f, 14f)
                horizontalLineTo(19f)
            }
        }.build()
        return _sauce!!
    }
private var _sauce: ImageVector? = null

/** 调味罐（撒孔盖） */
val Seasoning: ImageVector
    get() {
        if (_seasoning != null) return _seasoning!!
        _seasoning = ImageVector.Builder(
            name = "ShisuanSeasoning", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 盖（圆角台）
                moveTo(8.5f, 3f)
                horizontalLineTo(15.5f)
                curveTo(16.6f, 3f, 17.5f, 3.9f, 17.5f, 5f)
                verticalLineTo(7f)
                horizontalLineTo(6.5f)
                verticalLineTo(5f)
                curveTo(6.5f, 3.9f, 7.4f, 3f, 8.5f, 3f)
                close()
                // 罐身
                moveTo(7f, 7f)
                verticalLineTo(18f)
                curveTo(7f, 19.7f, 8.3f, 21f, 10f, 21f)
                horizontalLineTo(14f)
                curveTo(15.7f, 21f, 17f, 19.7f, 17f, 18f)
                verticalLineTo(7f)
                // 撒孔
                moveTo(10f, 5f)
                lineTo(10f, 5.01f)
                moveTo(12f, 5f)
                lineTo(12f, 5.01f)
                moveTo(14f, 5f)
                lineTo(14f, 5.01f)
            }
        }.build()
        return _seasoning!!
    }
private var _seasoning: ImageVector? = null

/** 罐头（顶面椭圆 + 拉环） */
val Can: ImageVector
    get() {
        if (_can != null) return _can!!
        _can = ImageVector.Builder(
            name = "ShisuanCan", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 顶面椭圆
                moveTo(5f, 8f)
                curveTo(5f, 6.3f, 8.1f, 5f, 12f, 5f)
                curveTo(15.9f, 5f, 19f, 6.3f, 19f, 8f)
                curveTo(19f, 9.7f, 15.9f, 11f, 12f, 11f)
                curveTo(8.1f, 11f, 5f, 9.7f, 5f, 8f)
                close()
                // 侧壁 + 底
                moveTo(5f, 8f)
                verticalLineTo(16f)
                curveTo(5f, 17.7f, 8.1f, 19f, 12f, 19f)
                curveTo(15.9f, 19f, 19f, 17.7f, 19f, 16f)
                verticalLineTo(8f)
                // 拉环
                moveTo(10.5f, 7.2f)
                curveTo(10.5f, 6.4f, 13.5f, 6.4f, 13.5f, 7.2f)
            }
        }.build()
        return _can!!
    }
private var _can: ImageVector? = null

/** 饮料杯（梯形杯 + 液面波 + 吸管） */
val Soda: ImageVector
    get() {
        if (_soda != null) return _soda!!
        _soda = ImageVector.Builder(
            name = "ShisuanSoda", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 杯身（圆角底梯形）
                moveTo(6f, 8f)
                lineTo(7.7f, 20.2f)
                curveTo(7.8f, 20.9f, 8.4f, 21.5f, 9.1f, 21.5f)
                horizontalLineTo(14.9f)
                curveTo(15.6f, 21.5f, 16.2f, 20.9f, 16.3f, 20.2f)
                lineTo(18f, 8f)
                // 杯口
                moveTo(5f, 8f)
                horizontalLineTo(19f)
                // 液面波
                moveTo(7f, 15f)
                curveTo(8.5f, 13.7f, 10.5f, 13.7f, 12f, 15f)
                curveTo(13.5f, 16.3f, 15.5f, 16.3f, 17f, 15f)
                // 吸管
                moveTo(12f, 8f)
                lineTo(13f, 2f)
                horizontalLineTo(15f)
            }
        }.build()
        return _soda!!
    }
private var _soda: ImageVector? = null

/** 牛奶瓶（瓶肩收窄 + 液面波） */
val Milk: ImageVector
    get() {
        if (_milk != null) return _milk!!
        _milk = ImageVector.Builder(
            name = "ShisuanMilk", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 瓶口
                moveTo(9f, 2f)
                horizontalLineTo(15f)
                // 瓶身（肩部收窄）
                moveTo(9f, 2f)
                verticalLineTo(4f)
                curveTo(9f, 6.2f, 7f, 8f, 7f, 10.5f)
                verticalLineTo(18.5f)
                curveTo(7f, 19.9f, 8.1f, 21f, 9.5f, 21f)
                horizontalLineTo(14.5f)
                curveTo(15.9f, 21f, 17f, 19.9f, 17f, 18.5f)
                verticalLineTo(10.5f)
                curveTo(17f, 8f, 15f, 6.2f, 15f, 4f)
                verticalLineTo(2f)
                // 液面波
                moveTo(7f, 15f)
                curveTo(8.5f, 13.7f, 10.5f, 13.7f, 12f, 15f)
                curveTo(13.5f, 16.3f, 15.5f, 16.3f, 17f, 15f)
            }
        }.build()
        return _milk!!
    }
private var _milk: ImageVector? = null

/** 糖果（椭圆体 + 条纹 + 两端包装翼） */
val Candy: ImageVector
    get() {
        if (_candy != null) return _candy!!
        _candy = ImageVector.Builder(
            name = "ShisuanCandy", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 糖体
                moveTo(7.5f, 12f)
                curveTo(7.5f, 9.5f, 9.5f, 8f, 12f, 8f)
                curveTo(14.5f, 8f, 16.5f, 9.5f, 16.5f, 12f)
                curveTo(16.5f, 14.5f, 14.5f, 16f, 12f, 16f)
                curveTo(9.5f, 16f, 7.5f, 14.5f, 7.5f, 12f)
                close()
                // 条纹
                moveTo(10.5f, 8.3f)
                verticalLineTo(15.7f)
                moveTo(13.5f, 8.3f)
                verticalLineTo(15.7f)
                // 左包装翼
                moveTo(7.5f, 10.5f)
                lineTo(4f, 8.5f)
                lineTo(5.3f, 12f)
                lineTo(4f, 15.5f)
                lineTo(7.5f, 13.5f)
                // 右包装翼
                moveTo(16.5f, 10.5f)
                lineTo(20f, 8.5f)
                lineTo(18.7f, 12f)
                lineTo(20f, 15.5f)
                lineTo(16.5f, 13.5f)
            }
        }.build()
        return _candy!!
    }
private var _candy: ImageVector? = null

/** 巧克力排块（3×3 分格） */
val Chocolate: ImageVector
    get() {
        if (_chocolate != null) return _chocolate!!
        _chocolate = ImageVector.Builder(
            name = "ShisuanChocolate", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 外框
                moveTo(5f, 4f)
                horizontalLineTo(19f)
                verticalLineTo(20f)
                horizontalLineTo(5f)
                close()
                // 竖分格
                moveTo(9.67f, 4f)
                verticalLineTo(20f)
                moveTo(14.33f, 4f)
                verticalLineTo(20f)
                // 横分格
                moveTo(5f, 9.33f)
                horizontalLineTo(19f)
                moveTo(5f, 14.67f)
                horizontalLineTo(19f)
            }
        }.build()
        return _chocolate!!
    }
private var _chocolate: ImageVector? = null

/** 水果（苹果 + 梗 + 叶） */
val Apple: ImageVector
    get() {
        if (_apple != null) return _apple!!
        _apple = ImageVector.Builder(
            name = "ShisuanApple", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 果身（双肩心形底）
                moveTo(12f, 7.5f)
                curveTo(9.5f, 5.8f, 6f, 6.8f, 5.3f, 10f)
                curveTo(4.6f, 13.3f, 6.5f, 17.5f, 9f, 19.8f)
                curveTo(10.3f, 21f, 11.3f, 20.3f, 12f, 19.8f)
                curveTo(12.7f, 20.3f, 13.7f, 21f, 15f, 19.8f)
                curveTo(17.5f, 17.5f, 19.4f, 13.3f, 18.7f, 10f)
                curveTo(18f, 6.8f, 14.5f, 5.8f, 12f, 7.5f)
                close()
                // 梗
                moveTo(12f, 7f)
                verticalLineTo(3.5f)
                // 叶
                moveTo(12f, 4.5f)
                curveTo(12.8f, 3f, 14.5f, 2.8f, 15.5f, 3.8f)
                curveTo(15f, 5.3f, 13.3f, 5.5f, 12f, 4.5f)
                close()
            }
        }.build()
        return _apple!!
    }
private var _apple: ImageVector? = null

/** 蔬菜（胡萝卜 + 缨叶） */
val Carrot: ImageVector
    get() {
        if (_carrot != null) return _carrot!!
        _carrot = ImageVector.Builder(
            name = "ShisuanCarrot", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 根身（圆肩锥形，尖端左下）
                moveTo(13.2f, 7.8f)
                curveTo(15.5f, 7f, 17f, 8.5f, 16.2f, 10.8f)
                lineTo(4.5f, 20.6f)
                close()
                // 缨叶三根
                moveTo(14.5f, 7.2f)
                lineTo(17f, 4.5f)
                moveTo(13.5f, 7.3f)
                lineTo(13f, 3f)
                moveTo(16.3f, 9.5f)
                lineTo(20f, 7.5f)
            }
        }.build()
        return _carrot!!
    }
private var _carrot: ImageVector? = null

/** 蛋（蛋形 + 高光弧） */
val Egg: ImageVector
    get() {
        if (_egg != null) return _egg!!
        _egg = ImageVector.Builder(
            name = "ShisuanEgg", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 蛋形（上窄下宽）
                moveTo(12f, 3f)
                curveTo(8.7f, 3f, 5.5f, 8.3f, 5.5f, 13.5f)
                curveTo(5.5f, 17.6f, 8.4f, 21f, 12f, 21f)
                curveTo(15.6f, 21f, 18.5f, 17.6f, 18.5f, 13.5f)
                curveTo(18.5f, 8.3f, 15.3f, 3f, 12f, 3f)
                close()
                // 高光弧
                moveTo(8.5f, 11f)
                curveTo(8.5f, 12.8f, 9f, 14.3f, 10f, 15.6f)
            }
        }.build()
        return _egg!!
    }
private var _egg: ImageVector? = null

/** 粮油（麦穗：茎 + 两对麦粒 + 顶芒） */
val Grain: ImageVector
    get() {
        if (_grain != null) return _grain!!
        _grain = ImageVector.Builder(
            name = "ShisuanGrain", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 茎
                moveTo(12f, 21f)
                verticalLineTo(7f)
                // 上部左粒
                moveTo(12f, 11f)
                curveTo(10f, 10.6f, 8.5f, 8.7f, 8.5f, 6.2f)
                curveTo(10.7f, 6.8f, 12f, 8.6f, 12f, 11f)
                close()
                // 上部右粒
                moveTo(12f, 11f)
                curveTo(14f, 10.6f, 15.5f, 8.7f, 15.5f, 6.2f)
                curveTo(13.3f, 6.8f, 12f, 8.6f, 12f, 11f)
                close()
                // 中部左粒
                moveTo(12f, 16f)
                curveTo(10f, 15.6f, 8.9f, 14.2f, 8.9f, 12.2f)
                curveTo(10.7f, 12.7f, 12f, 14.1f, 12f, 16f)
                close()
                // 中部右粒
                moveTo(12f, 16f)
                curveTo(14f, 15.6f, 15.1f, 14.2f, 15.1f, 12.2f)
                curveTo(13.3f, 12.7f, 12f, 14.1f, 12f, 16f)
                close()
                // 顶芒
                moveTo(12f, 7f)
                verticalLineTo(2.5f)
            }
        }.build()
        return _grain!!
    }
private var _grain: ImageVector? = null

/** 水产（鱼 + 鳃线 + 眼点） */
val Fish: ImageVector
    get() {
        if (_fish != null) return _fish!!
        _fish = ImageVector.Builder(
            name = "ShisuanFish", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 鱼身（杏仁形）
                moveTo(6.5f, 12f)
                curveTo(7.5f, 9.2f, 10f, 7.2f, 13f, 7.2f)
                curveTo(15.8f, 7.2f, 18.2f, 9.2f, 19.3f, 12f)
                curveTo(18.2f, 14.8f, 15.8f, 16.8f, 13f, 16.8f)
                curveTo(10f, 16.8f, 7.5f, 14.8f, 6.5f, 12f)
                close()
                // 尾（分叉三角）
                moveTo(6.8f, 12f)
                lineTo(3f, 9.5f)
                lineTo(3.9f, 12f)
                lineTo(3f, 14.5f)
                close()
                // 鳃线
                moveTo(15.5f, 8.8f)
                curveTo(14.3f, 10.5f, 14.3f, 13.5f, 15.5f, 15.2f)
                // 眼
                moveTo(17.5f, 11.5f)
                lineTo(17.5f, 11.51f)
            }
        }.build()
        return _fish!!
    }
private var _fish: ImageVector? = null

/** 速冻（六向雪花 + 上下枝杈） */
val Frozen: ImageVector
    get() {
        if (_frozen != null) return _frozen!!
        _frozen = ImageVector.Builder(
            name = "ShisuanFrozen", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 三主轴（竖 + 两条 60° 斜线）
                moveTo(12f, 3f)
                verticalLineTo(21f)
                moveTo(4.2f, 7.5f)
                lineTo(19.8f, 16.5f)
                moveTo(19.8f, 7.5f)
                lineTo(4.2f, 16.5f)
                // 上 / 下枝杈
                moveTo(9.8f, 5.2f)
                lineTo(12f, 7.2f)
                lineTo(14.2f, 5.2f)
                moveTo(9.8f, 18.8f)
                lineTo(12f, 16.8f)
                lineTo(14.2f, 18.8f)
            }
        }.build()
        return _frozen!!
    }
private var _frozen: ImageVector? = null

/** 茶饮（碗形杯 + 把手 + 双蒸汽） */
val Cup: ImageVector
    get() {
        if (_cup != null) return _cup!!
        _cup = ImageVector.Builder(
            name = "ShisuanCup", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f
        ).apply {
            path(
                fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
            ) {
                // 杯体
                moveTo(5f, 9f)
                lineTo(6.6f, 19.4f)
                curveTo(6.8f, 20.4f, 7.7f, 21.2f, 8.8f, 21.2f)
                horizontalLineTo(15.2f)
                curveTo(16.3f, 21.2f, 17.2f, 20.4f, 17.4f, 19.4f)
                lineTo(19f, 9f)
                // 杯口
                moveTo(4f, 9f)
                horizontalLineTo(20f)
                // 把手
                moveTo(19f, 10.5f)
                curveTo(20.9f, 10.5f, 22f, 11.7f, 22f, 13.2f)
                curveTo(22f, 14.7f, 20.9f, 15.9f, 19f, 15.9f)
                // 蒸汽
                moveTo(9.5f, 2f)
                verticalLineTo(4.5f)
                moveTo(14.5f, 2f)
                verticalLineTo(4.5f)
            }
        }.build()
        return _cup!!
    }
private var _cup: ImageVector? = null

/**
 * 按分类关键词匹配产品/原料图标（食品工厂全品类）
 * 匹配顺序从具体到宽泛（如「果酱」先于「果」、「蛋糕」先于「蛋」），
 * 未命中时回退到通用罐图标
 */
fun categoryIcon(category: String): ImageVector {
    val c = category.lowercase()
    return when {
        // 烘焙
        c.contains("蛋挞") || c.contains("tart") -> EggTart
        c.contains("蛋糕") || c.contains("cake") -> Cake
        c.contains("面包") || c.contains("bread") || c.contains("吐司") -> Bread
        c.contains("烘焙") || c.contains("点心") || c.contains("糕点") -> Bread
        c.contains("黄油") || c.contains("butter") -> Butter
        c.contains("奶油") || c.contains("cream") || c.contains("淡奶油") -> Cream
        c.contains("饼干") || c.contains("cookie") || c.contains("曲奇") -> Cookie
        // 甜品零食
        c.contains("巧克力") || c.contains("chocolate") -> Chocolate
        c.contains("糖果") || c.contains("糖") || c.contains("candy") || c.contains("零食") || c.contains("甜品") -> Candy
        // 果酱酱料调味（「果酱」须先于「果」、「酱」）
        c.contains("果酱") || c.contains("jam") -> Jam
        c.contains("酱") || c.contains("sauce") || c.contains("辣") -> Sauce
        c.contains("粮") || c.contains("米") || c.contains("面粉") || c.contains("淀粉") || c.contains("麦") || c.contains("grain") || c.contains("rice") || c.contains("flour") -> Grain
        c.contains("调味") || c.contains("酱油") || c.contains("醋") || c.contains("盐") || c.contains("油") || c.contains("香料") || c.contains("seasoning") || c.contains("oil") -> Seasoning
        // 罐头与饮品
        c.contains("罐头") || c.contains("can") -> Can
        c.contains("饮料") || c.contains("饮品") || c.contains("果汁") || c.contains("汽水") || c.contains("drink") || c.contains("juice") -> Soda
        c.contains("茶") || c.contains("咖啡") || c.contains("tea") || c.contains("coffee") -> Cup
        // 乳制品
        c.contains("牛奶") || c.contains("乳") || c.contains("奶") || c.contains("dairy") || c.contains("milk") -> Milk
        // 生鲜农产品
        c.contains("水果") || c.contains("果") || c.contains("fruit") || c.contains("apple") -> Apple
        c.contains("蔬菜") || c.contains("菜") || c.contains("胡萝卜") || c.contains("veg") || c.contains("carrot") -> Carrot
        c.contains("水产") || c.contains("鱼") || c.contains("海鲜") || c.contains("fish") -> Fish
        c.contains("蛋") || c.contains("egg") -> Egg
        // 冷冻
        c.contains("冷冻") || c.contains("速冻") || c.contains("冰") || c.contains("frozen") || c.contains("ice") -> Frozen
        else -> Jar
    }
}
