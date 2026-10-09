package com.flathike.app.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File

/**
 * On-device Gemma inference engine using Google MediaPipe Tasks GenAI.
 */
class GemmaOnDeviceEngine(private val context: Context) {

    private var llmInference: LlmInference? = null
    private var currentModelPath: String? = null

    val isModelLoaded: Boolean
        get() = llmInference != null

    fun loadModel(modelPath: String): Result<Unit> {
        return try {
            val file = File(modelPath)
            if (!file.exists()) {
                return Result.failure(IllegalArgumentException("Файл модели не найден по пути: $modelPath"))
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .build()

            llmInference?.close()
            llmInference = LlmInference.createFromOptions(context, options)
            currentModelPath = modelPath
            Result.success(Unit)
        } catch (t: Throwable) {
            llmInference = null
            Result.failure(t)
        }
    }

    fun generateResponse(prompt: String): Result<String> {
        val inference = llmInference
            ?: return Result.failure(IllegalStateException("Gemma модель не загружена. Укажите файл .bin/.task в настройках."))

        return try {
            val response = inference.generateResponse(prompt)
            Result.success(response)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun close() {
        try {
            llmInference?.close()
            llmInference = null
        } catch (_: Exception) {}
    }
}
