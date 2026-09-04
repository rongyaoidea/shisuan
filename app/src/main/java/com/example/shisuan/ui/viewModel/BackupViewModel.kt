package com.example.shisuan.ui.viewModel

import android.net.Uri
import com.example.shisuan.data.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 备份与恢复 ViewModel
 *
 * 独立于业务 ViewModel：备份入口在产品列表页顶栏，
 * 与产品数据流没有状态交集，单独成类职责更清晰。
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : BaseViewModel() {

    /** 面向用户的操作结果提示（成功/失败共用 Snackbar 通道） */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() {
        _message.value = null
    }

    /** 导出数据库到用户选择的位置 */
    fun exportTo(uri: Uri) {
        launchSafe {
            backupManager.exportTo(uri)
                .onSuccess { bytes ->
                    _message.value = "备份已导出（%.0f KB）".format(bytes / 1024.0)
                }
                .onFailure { showError(it.userMessage()) }
        }
    }

    /**
     * 从备份恢复数据库。
     * 成功时 [BackupManager] 会重启进程，应用回到启动页；
     * 失败时现有数据不受影响，仅提示错误。
     */
    fun importFrom(uri: Uri) {
        launchSafe {
            backupManager.importFrom(uri)
                .onFailure { showError(it.userMessage()) }
        }
    }
}
