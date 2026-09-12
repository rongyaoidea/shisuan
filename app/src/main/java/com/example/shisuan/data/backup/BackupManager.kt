package com.example.shisuan.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.shisuan.data.database.CostCalDatabase
import com.example.shisuan.data.database.CostCalDatabase.Companion.SUPPORTED_DB_VERSION
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据备份与恢复
 *
 * 本应用完全离线、数据只存本机 —— 手机丢失/损坏/误卸载即意味着
 * 全部历史批次记录永久消失。这里提供基于 SAF（存储访问框架）的
 * 导出/导入能力，不需要任何存储权限。
 *
 * 导出：先做 WAL checkpoint 把临时日志合并回主库，再整体拷贝；
 * 导入：先落到缓存文件做完整性校验（SQLite 头 + schema 版本），
 *       再关闭现有连接、替换主库文件，最后重启进程让所有连接失效。
 *
 * 注意：备份文件为明文 SQLite 数据库，包含全部批次与成本数据；
 * 请勿经不可信渠道转发，妥善保管导出文件。
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: CostCalDatabase
) {

    /** SQLite 文件固定魔数，用于快速识别所选文件是不是 SQLite 数据库 */
    private val sqliteHeader: ByteArray = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    /**
     * 导出当前数据库到用户选择的位置（SAF Uri）。
     * @return 导出的字节数
     */
    suspend fun exportTo(uri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            // WAL checkpoint：把 -wal 临时日志合并回主库文件。
            // 跳过这步导出的主文件可能缺最近一次写入的数据。
            writableDb().query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }

            val dbFile = File(writableDb().path ?: throw IOException("无法定位数据库文件"))
            require(dbFile.exists()) { "数据库文件不存在" }

            val bytes = context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                dbFile.inputStream().use { inp -> inp.copyTo(out) }
            } ?: throw IOException("无法写入所选位置")
            bytes
        }
    }

    /**
     * 从备份文件恢复数据库。
     *
     * 恢复会整体替换现有数据 —— 调用方必须先向用户确认。
     * 成功返回后进程会被重启，所有内存态（ViewModel、DAO 缓存）随之失效。
     */
    suspend fun importFrom(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. 先拷贝到缓存文件并校验，任何一步失败都不触碰现有数据
            val staging = File(context.cacheDir, "import_staging_${System.currentTimeMillis()}.db")
            val rollbackBackup = File(context.cacheDir, "import_backup_${System.currentTimeMillis()}.db.bak")
            try {
                context.contentResolver.openInputStream(uri)?.use { inp ->
                    staging.outputStream().use { inp.copyTo(it) }
                } ?: throw IOException("无法读取所选文件")

                validateBackup(staging)

                // 2. 记录路径后关闭当前连接（关闭后再取 path 会重新打开库）。
                //    先记录 dbPath，再做 checkpoint + 备份当前主库，失败可回滚。
                val dbPath = writableDb().path ?: throw IOException("无法定位数据库文件")
                writableDb().query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
                File(dbPath).copyTo(rollbackBackup, overwrite = true)
                db.close()

                // 3. 替换主库文件：先删除旧 -wal / -shm，再用 rename 原子替换。
                //    cacheDir 与 databases 同属应用私有存储同一分区，rename(2) 是原子操作——
                //    即使进程在替换瞬间被杀，留下的也是完整的旧库或完整的新库。
                //    修复：原实现「先 delete 主库再 copyTo」，拷贝中途失败（IO 错误/被杀）
                //    会导致新旧两份都不可用，用户全部数据丢失。
                File("$dbPath-wal").delete()
                File("$dbPath-shm").delete()
                if (!staging.renameTo(File(dbPath))) {
                    // 回滚：把替换前的 .bak 拷回主库位置。
                    // 主库连接已关闭，继续运行会让 Room 持有已关闭的连接，
                    // 因此回滚后立即重启（数据与恢复前一致），再抛错仅作兜底。
                    runCatching {
                        rollbackBackup.copyTo(File(dbPath), overwrite = true)
                    }
                    restartApp()
                    throw IOException("恢复数据失败，已回滚到恢复前的数据，请重试")
                }
                // 注：staging 已在 validateBackup 通过魔数 + integrity_check 校验，
                // 此处不再重复检查文件长度（避免关库后再抛错导致连接未重建）。
                // 成功后删除回滚备份
                rollbackBackup.delete()

                // 4. 重启进程：Hilt 单例、Room 连接、内存中的 Flow 全部重建。
                //    restartApp 前同步删除 staging（双保险：finally 还会再删一次，
                //    但重启后 finally 是否执行不可依赖）。
                staging.delete()
                restartApp()
            } finally {
                staging.delete()
            }
        }
    }

    /** 关闭前可用的可写库连接（只在此类内部短期使用） */
    private fun writableDb(): SupportSQLiteDatabase = db.openHelper.writableDatabase

    /**
     * 校验备份文件：SQLite 魔数 + schema 版本不超过当前应用支持的版本
     * + PRAGMA integrity_check 完整性检查（首行须为 "ok"）。
     * 版本过高的备份在旧版应用上打开会直接崩溃，必须在这里拦下。
     */
    private fun validateBackup(file: File) {
        require(file.length() >= sqliteHeader.size.toLong()) { "所选文件为空或已损坏" }

        val header = ByteArray(sqliteHeader.size)
        file.inputStream().use { inp ->
            require(inp.read(header) == header.size) { "无法读取所选文件" }
        }
        require(header.contentEquals(sqliteHeader)) { "所选文件不是食算的数据库备份" }

        val db = SQLiteDatabase.openDatabase(
            file.absolutePath, null, SQLiteDatabase.OPEN_READONLY
        )
        try {
            require(db.version <= SUPPORTED_DB_VERSION) {
                "该备份由更新版本的食算导出，请先升级应用再恢复"
            }
            db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                val ok = cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)
                require(ok) { "备份文件完整性校验失败，可能已损坏" }
            }
        } finally {
            db.close()
        }
    }

    /**
     * 重启应用进程：先拉起启动页，延迟片刻再结束当前进程。
     *
     * 原实现 startActivity 后立即 killProcess，部分 ROM 上来不及拉起新进程，
     * 表现为「恢复成功后应用直接退出且不再启动」。这里留出系统调度时间。
     */
    private fun restartApp() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (launchIntent != null) context.startActivity(launchIntent)
        Handler(Looper.getMainLooper()).postDelayed({
            Process.killProcess(Process.myPid())
            // killProcess 后理论不可达；兜底确保进程退出
            Runtime.getRuntime().exit(0)
        }, RESTART_DELAY_MS)
    }

    companion object {
        /** 重启前留出系统拉起启动页的时间（毫秒） */
        private const val RESTART_DELAY_MS = 800L
    }
}
