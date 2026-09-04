package com.example.shisuan.ui.viewModel

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * 所有 ViewModel 的公共基类：统一错误处理
 *
 * viewModelScope.launch 中未捕获的异常会直接导致应用崩溃；
 * [launchSafe] 把非取消类异常转为用户可读的提示，由 UI 以 Snackbar 呈现。
 *
 * CancellationException 必须原样重新抛出，否则会破坏协程的结构化取消语义
 * （页面退出、流被替换等正常取消会被误判为错误）。
 */
abstract class BaseViewModel : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    protected fun launchSafe(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _error.value = e.userMessage()
            }
        }
    }

    /** 子类主动上报错误（如业务校验失败），同样经 UI Snackbar 呈现 */
    protected fun showError(message: String) {
        _error.value = message
    }

    /** UI 展示完错误提示后调用，避免重复弹出 */
    fun consumeError() {
        _error.value = null
    }
}

/**
 * 把底层异常翻译成一线人员能看懂的提示。
 * 不暴露技术细节（表名/SQL），只说明「发生了什么、该做什么」。
 */
fun Throwable.userMessage(): String = when (this) {
    is SQLiteConstraintException -> "数据冲突：该记录已存在或被其他数据引用"
    is IOException -> "存储空间不足或文件不可访问，请检查后重试"
    is IllegalArgumentException -> message ?: "输入内容无效，请检查后重试"
    else -> message?.takeIf { it.isNotBlank() } ?: "操作失败，请重试"
}
