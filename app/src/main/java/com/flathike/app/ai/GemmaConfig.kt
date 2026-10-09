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
    )
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
