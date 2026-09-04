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

# 实体字段：批次快照（BatchSnapshot.snapshotData）以 JSON 文本持久化历史版本，
# 字段名被混淆后旧快照将无法解析，这里保留 data.database 下所有字段名。
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
