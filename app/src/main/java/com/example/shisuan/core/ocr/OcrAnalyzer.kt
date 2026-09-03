package com.example.shisuan.core.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 配料表 OCR 识别器
 *
 * 基于 Google ML Kit 中文文字识别（离线模型）。
 * 识别文本交给 [IngredientTextParser] 解析为配料名称列表。
 */
class OcrAnalyzer(private val context: Context) {

    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    /**
     * 识别图片中的配料名称列表。
     * @param uri 图片 Uri（相册或相机）
     */
    suspend fun recognizeIngredientNames(uri: Uri): List<String> = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeBitmap(uri)
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            IngredientTextParser.parse(result.text)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 将 GMS Task 转为挂起函数 */
    private suspend fun <T> Task<T>.await(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resumeWithException(it) }
        }

    private fun decodeBitmap(uri: Uri): Bitmap {
        var input: InputStream? = null
        try {
            input = context.contentResolver.openInputStream(uri)
            return BitmapFactory.decodeStream(input) ?: throw IllegalArgumentException("无法解码图片")
        } finally {
            input?.close()
        }
    }
}
