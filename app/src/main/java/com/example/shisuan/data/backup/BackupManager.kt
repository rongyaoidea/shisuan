package com.example.shisuan.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
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
            try {
                context.contentResolver.openInputStream(uri)?.use { inp ->
                    staging.outputStream().use { inp.copyTo(it) }
                } ?: throw IOException("无法读取所选文件")

                validateBackup(staging)

                // 2. 记录路径后关闭当前连接（关闭后再取 path 会重新打开库）
                val dbPath = writableDb().path ?: throw IOException("无法定位数据库文件")
                db.close()

                // 3. 替换主库文件：先删除旧 -wal / -shm，再用 rename 原子替换。
                //    cacheDir 与 databases 同属应用私有存储同一分区，rename(2) 是原子操作——
                //    即使进程在替换瞬间被杀，留下的也是完整的旧库或完整的新库。
                //    修复：原实现「先 delete 主库再 copyTo」，拷贝中途失败（IO 错误/被杀）
                //    会导致新旧两份都不可用，用户全部数据丢失。
                File("$dbPath-wal").delete()
                File("$dbPath-shm").delete()
                if (!staging.renameTo(File(dbPath))) {
                    throw IOException("恢复数据失败，请重试")
                }
                require(File(dbPath).length() > 0) { "恢复数据失败，请重试" }

                // 4. 重启进程：Hilt 单例、Room 连接、内存中的 Flow 全部重建
                restartApp()
            } finally {
                staging.delete()
            }
        }
    }

    /** 关闭前可用的可写库连接（只在此类内部短期使用） */
    private fun writableDb(): SupportSQLiteDatabase = db.openHelper.writableDatabase

    /**
     * 校验备份文件：SQLite 魔数 + schema 版本不超过当前应用支持的版本。
     * 版本过高的备份在旧版应用上打开会直接崩溃，必须在这里拦下。
     */
    private fun validateBackup(file: File) {
        require(file.length() >= sqliteHeader.size.toLong()) { "所选文件为空或已损坏" }

        val header = ByteArray(sqliteHeader.size)
        file.inputStream().use { inp ->
            require(inp.read(header) == header.size) { "无法读取所选文件" }
        }
        require(header.contentEquals(sqliteHeader)) { "所选文件不是食算的数据库备份" }

        val version = SQLiteDatabase.openDatabase(
            file.absolutePath, null, SQLiteDatabase.OPEN_READONLY
        ).use { it.version }
        require(version <= SUPPORTED_DB_VERSION) {
            "该备份由更新版本的食算导出，请先升级应用再恢复"
        }
    }

    /** 重启应用进程：先拉起启动页，再结束当前进程 */
    private fun restartApp() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (launchIntent != null) context.startActivity(launchIntent)
        Process.killProcess(Process.myPid())
        // killProcess 后理论不可达；兜底确保进程退出
        Runtime.getRuntime().exit(0)
    }
}
