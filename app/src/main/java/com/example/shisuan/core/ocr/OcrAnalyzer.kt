package com.example.shisuan.core.ocr

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 配料表 OCR 识别器
 *
 * 基于 Google ML Kit 中文文字识别（离线模型）。
 * 识别文本交给 [IngredientTextParser] 解析为配料名称列表。
 *
 * 生命周期：Hilt Singleton 常驻应用进程，recognizer 无需逐次创建；
 * 进程退出/测试 teardown 时调 [close] 释放原生资源。
 */
class OcrAnalyzer(private val context: Context) : Closeable {

    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    /**
     * 识别图片中的配料名称列表。
     *
     * 返回 [Result] 而非吞掉异常：调用方必须区分
     * 「识别失败（应提示重试）」与「识别成功但没有配料文字」两种情况，
     * 否则用户扫码后毫无反馈，无法判断下一步该做什么。
     *
     * @param uri 图片 Uri（相册或相机）
     */
    suspend fun recognizeIngredientNames(uri: Uri): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                // fromFilePath 自动读取 EXIF 方向并按需降采样。
                // 旧实现 BitmapFactory 全尺寸解码 + 写死旋转 0°：
                // 相机直拍照片既可能 OOM，也会因方向错误导致识别率骤降。
                val image = InputImage.fromFilePath(context, uri)
                val result = recognizer.process(image).await()
                Result.success(IngredientTextParser.parse(result.text))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** 将 GMS Task 转为挂起函数 */
    private suspend fun <T> Task<T>.await(): T =
        suspendCancellableCoroutine { cont ->
            val task = this@await
            // 协程取消时同步取消底层 GMS Task，避免后台识别继续占用模型线程
            cont.invokeOnCancellation { task.cancel() }
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resumeWithException(it) }
        }

    /** 释放 ML Kit recognizer 的原生资源（Hilt Singleton 常驻，进程退出/测试时调用） */
    override fun close() {
        recognizer.close()
    }
}
