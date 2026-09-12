# ─────────────────────────────────────────────────────────────
#  食算 R8 / ProGuard 规则
# ─────────────────────────────────────────────────────────────

# ─────────── JNI / Rust 计算引擎 ───────────
# libshisuan_core.so 通过 JNI 回调 Kotlin 侧声明的 external fun。
# JNI 方法查找依赖「完整类名 + 方法名 + 签名」的精确匹配，
# 一旦被混淆或当作未使用代码移除，运行期会抛 UnsatisfiedLinkError，
# 且该崩溃只在 release 构建出现 —— 因此这里必须显式保留。
-keep class com.example.shisuan.core.ShisuanCore { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# ─────────── Room 数据库 ───────────
# Room 生成的 CostCalDatabase_Impl 由 RoomDatabase.Builder 按类名反射实例化。
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# 实体字段名保留：与快照格式无关（快照是行式文本，见 BatchSnapshotCodec），
# Room/KSP 生成的列名与访问代码均在编译期确定。此规则为防御性保留：
# 避免未来引入反射式序列化/调试工具时字段名被混淆，保证导出的明文库可读。
-keepclassmembers class com.example.shisuan.data.database.** {
    <fields>;
}

# ─────────── Kotlin 协程与序列化 ───────────
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ─────────── ML Kit / GMS（OCR）───────────
-dontwarn com.google.android.gms.**
-dontwarn com.google.mlkit.**

# ─────────── 崩溃诊断 ───────────
# 保留行号，线上堆栈才能定位到源码
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
