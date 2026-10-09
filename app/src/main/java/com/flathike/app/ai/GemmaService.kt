package com.flathike.app.ai

import android.content.Context
import com.flathike.app.model.HikingPreset
import com.flathike.app.model.TrackCalculationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unified Gemma AI service managing On-Device, Cloud API, and Offline Intelligence backends.
 */
class GemmaService(private val context: Context) {

    private val onDeviceEngine = GemmaOnDeviceEngine(context)
    private val cloudEngine = GemmaCloudEngine()

    var config = GemmaConfig()
        private set

    suspend fun updateConfig(newConfig: GemmaConfig): Result<Unit> = withContext(Dispatchers.IO) {
        config = newConfig
        if (newConfig.backendType == GemmaBackendType.ON_DEVICE_MEDIAPIPE) {
            if (newConfig.modelFilePath.isNotBlank()) {
                onDeviceEngine.loadModel(newConfig.modelFilePath)
            } else {
                Result.success(Unit)
            }
        } else {
            onDeviceEngine.close()
            Result.success(Unit)
        }
    }

    suspend fun generateTrailReport(
        result: TrackCalculationResult,
        preset: HikingPreset,
        effectiveCoeff: Double
    ): String = withContext(Dispatchers.Default) {
        val prompt = GemmaPromptBuilder.buildTrailAnalysisPrompt(result, preset, effectiveCoeff)

        when (config.backendType) {
            GemmaBackendType.OFFLINE_INTELLIGENCE -> {
                GemmaTrailIntelligenceEngine.generateTrailReport(result, preset, effectiveCoeff)
            }
            GemmaBackendType.ON_DEVICE_MEDIAPIPE -> {
                if (!onDeviceEngine.isModelLoaded && config.modelFilePath.isNotBlank()) {
                    onDeviceEngine.loadModel(config.modelFilePath)
                }

                val onDeviceResult = onDeviceEngine.generateResponse(prompt)
                onDeviceResult.getOrElse { error ->
                    // Graceful fallback to offline trail engine with notification
                    val offlineText = GemmaTrailIntelligenceEngine.generateTrailReport(result, preset, effectiveCoeff)
                    "> **Уведомление:** [Локальная Gemma недоступна: ${error.localizedMessage ?: "ошибка"}]. Переключено на резервный офлайн-движок:\n\n$offlineText"
                }
            }
            GemmaBackendType.CLOUD_API -> {
                val cloudResult = cloudEngine.generateResponse(config, prompt)
                cloudResult.getOrElse { error ->
                    val offlineText = GemmaTrailIntelligenceEngine.generateTrailReport(result, preset, effectiveCoeff)
                    "> **Уведомление:** [Сбой запроса к Gemma API: ${error.localizedMessage ?: "ошибка"}]. Переключено на резервный офлайн-движок:\n\n$offlineText"
                }
            }
        }
    }

    suspend fun answerQuestion(
        question: String,
        result: TrackCalculationResult,
        preset: HikingPreset,
        effectiveCoeff: Double
    ): String = withContext(Dispatchers.Default) {
        val prompt = GemmaPromptBuilder.buildQuestionPrompt(question, result, preset, effectiveCoeff)

        when (config.backendType) {
            GemmaBackendType.OFFLINE_INTELLIGENCE -> {
                GemmaTrailIntelligenceEngine.answerQuestion(question, result, preset, effectiveCoeff)
            }
            GemmaBackendType.ON_DEVICE_MEDIAPIPE -> {
                if (!onDeviceEngine.isModelLoaded && config.modelFilePath.isNotBlank()) {
                    onDeviceEngine.loadModel(config.modelFilePath)
                }

                val onDeviceResult = onDeviceEngine.generateResponse(prompt)
                onDeviceResult.getOrElse { error ->
                    val offlineText = GemmaTrailIntelligenceEngine.answerQuestion(question, result, preset, effectiveCoeff)
                    "> **Уведомление:** [Локальная Gemma: ${error.localizedMessage}]. Ответ резервного офлайн-движка:\n\n$offlineText"
                }
            }
            GemmaBackendType.CLOUD_API -> {
                val cloudResult = cloudEngine.generateResponse(config, prompt)
                cloudResult.getOrElse { error ->
                    val offlineText = GemmaTrailIntelligenceEngine.answerQuestion(question, result, preset, effectiveCoeff)
                    "> **Уведомление:** [Сбой Gemma API: ${error.localizedMessage}]. Ответ резервного офлайн-движка:\n\n$offlineText"
                }
            }
        }
    }

    fun isLocalModelReady(): Boolean = onDeviceEngine.isModelLoaded
}
