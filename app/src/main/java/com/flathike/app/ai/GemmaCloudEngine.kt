package com.flathike.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client for invoking Gemma models hosted on remote API endpoints
 * (e.g. Groq, Google AI Studio, Ollama, OpenRouter).
 */
class GemmaCloudEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateResponse(
        config: GemmaConfig,
        prompt: String,
        systemPrompt: String = GemmaPromptBuilder.buildSystemPrompt()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (config.cloudEndpoint.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Не указан URL эндпоинта API"))
            }

            val isGoogleApi = config.cloudEndpoint.contains("generativelanguage.googleapis.com")
            val isOllamaGenerate = config.cloudEndpoint.endsWith("/api/generate")

            var targetUrl = config.cloudEndpoint
            if (isGoogleApi && !targetUrl.contains("key=") && config.cloudApiKey.isNotBlank()) {
                targetUrl = if (targetUrl.contains("?")) {
                    "$targetUrl&key=${config.cloudApiKey}"
                } else {
                    "$targetUrl?key=${config.cloudApiKey}"
                }
            }

            val requestBodyJson = buildRequestBody(config, prompt, systemPrompt, isGoogleApi, isOllamaGenerate)
            val requestBuilder = Request.Builder()
                .url(targetUrl)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))

            if (config.cloudApiKey.isNotBlank()) {
                if (isGoogleApi) {
                    requestBuilder.addHeader("x-goog-api-key", config.cloudApiKey)
                } else {
                    requestBuilder.addHeader("Authorization", "Bearer ${config.cloudApiKey}")
                }
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code}: $body")
                    )
                }

                val parsedText = parseResponse(body)
                if (parsedText.isNotBlank()) {
                    Result.success(parsedText)
                } else {
                    Result.failure(Exception("Пустой ответ от Gemma API"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildRequestBody(
        config: GemmaConfig,
        prompt: String,
        systemPrompt: String,
        isGoogleApi: Boolean,
        isOllamaGenerate: Boolean
    ): JSONObject {
        return when {
            isGoogleApi -> {
                // Official Google AI Studio (Gemini / Gemma) format
                val json = JSONObject()
                val contents = JSONArray()
                val userTurn = JSONObject()
                userTurn.put("role", "user")
                val parts = JSONArray()
                val part = JSONObject()
                part.put("text", "$systemPrompt\n\n$prompt")
                parts.put(part)
                userTurn.put("parts", parts)
                contents.put(userTurn)
                json.put("contents", contents)

                val genConfig = JSONObject()
                genConfig.put("temperature", config.temperature)
                json.put("generationConfig", genConfig)
                json
            }
            isOllamaGenerate -> {
                // Ollama native /api/generate format
                val json = JSONObject()
                json.put("model", config.cloudModelName.ifBlank { "gemma2" })
                json.put("prompt", prompt)
                json.put("system", systemPrompt)
                json.put("stream", false)
                json
            }
            else -> {
                // Standard OpenAI-compatible format (Groq, OpenRouter, Ollama /v1, Together)
                val json = JSONObject()
                json.put("model", config.cloudModelName.ifBlank { "gemma2-9b-it" })
                json.put("temperature", config.temperature)

                val messages = JSONArray()

                val sysMsg = JSONObject()
                sysMsg.put("role", "system")
                sysMsg.put("content", systemPrompt)
                messages.put(sysMsg)

                val userMsg = JSONObject()
                userMsg.put("role", "user")
                userMsg.put("content", prompt)
                messages.put(userMsg)

                json.put("messages", messages)
                json
            }
        }
    }

    private fun parseResponse(responseBody: String): String {
        return try {
            val root = JSONObject(responseBody)
            // OpenAI / Groq format: choices[0].message.content
            if (root.has("choices")) {
                val choices = root.getJSONArray("choices")
                if (choices.length() > 0) {
                    val first = choices.getJSONObject(0)
                    if (first.has("message")) {
                        return first.getJSONObject("message").optString("content", "")
                    } else if (first.has("text")) {
                        return first.optString("text", "")
                    }
                }
            }
            // Ollama standard response format: response
            if (root.has("response")) {
                return root.optString("response", "")
            }
            // Google Gemini/Gemma format: candidates[0].content.parts[0].text
            if (root.has("candidates")) {
                val candidates = root.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "")
                    }
                }
            }
            responseBody
        } catch (_: Exception) {
            responseBody
        }
    }
}
