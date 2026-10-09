package com.flathike.app.ai

/**
 * Backend engine types for Gemma AI.
 */
enum class GemmaBackendType(val displayName: String, val description: String) {
    OFFLINE_INTELLIGENCE(
        displayName = "Gemma Встроенный AI (Офлайн)",
        description = "Работает мгновенно без интернета и без скачивания тяжелых файлов весов (2 ГБ). Экспертный расчет и анализ маршрута."
    ),
    ON_DEVICE_MEDIAPIPE(
        displayName = "Gemma On-Device (MediaPipe)",
        description = "Локальная нейросеть Gemma (2B/3B) на GPU/CPU через Google MediaPipe Tasks GenAI. Требует файл .bin/.task."
    ),
    CLOUD_API(
        displayName = "Gemma Cloud API (Онлайн)",
        description = "Подключение к API с моделью Gemma 2 (Google AI Studio, Groq, OpenRouter или локальная Ollama)."
    );

    fun getDisplayName(isRu: Boolean): String = when (this) {
        OFFLINE_INTELLIGENCE -> if (isRu) "Gemma Встроенный AI (Офлайн)" else "Gemma Built-in AI (Offline)"
        ON_DEVICE_MEDIAPIPE -> if (isRu) "Gemma On-Device (MediaPipe)" else "Gemma On-Device (MediaPipe)"
        CLOUD_API -> if (isRu) "Gemma Cloud API (Онлайн)" else "Gemma Cloud API (Online)"
    }

    fun getDescription(isRu: Boolean): String = when (this) {
        OFFLINE_INTELLIGENCE -> if (isRu) "Работает мгновенно без интернета и без скачивания тяжелых файлов весов (2 ГБ). Экспертный расчет и анализ маршрута."
                                else "Works instantly offline without downloading heavy model weights (2 GB). Expert trail analysis."
        ON_DEVICE_MEDIAPIPE -> if (isRu) "Локальная нейросеть Gemma (2B/3B) на GPU/CPU через Google MediaPipe Tasks GenAI. Требует файл .bin/.task."
                               else "Local Gemma (2B/3B) neural model on GPU/CPU via Google MediaPipe Tasks GenAI. Requires .bin/.task file."
        CLOUD_API -> if (isRu) "Подключение к API с моделью Gemma 2 (Google AI Studio, Groq, OpenRouter или локальная Ollama)."
                     else "API connection with Gemma 2 model (Google AI Studio, Groq, OpenRouter, or local Ollama)."
    }
}

/**
 * User configuration for Gemma AI.
 */
data class GemmaConfig(
    val backendType: GemmaBackendType = GemmaBackendType.OFFLINE_INTELLIGENCE,
    val modelFilePath: String = "",
    val cloudEndpoint: String = "https://api.groq.com/openai/v1/chat/completions",
    val cloudApiKey: String = "",
    val cloudModelName: String = "gemma2-9b-it",
    val temperature: Float = 0.7f
)
