package com.flathike.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flathike.app.ai.GemmaBackendType
import com.flathike.app.ai.GemmaConfig

@Composable
fun GemmaSettingsDialog(
    initialConfig: GemmaConfig,
    onDismiss: () -> Unit,
    onSave: (GemmaConfig) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialConfig.backendType) }
    var modelPath by remember { mutableStateOf(initialConfig.modelFilePath) }
    var cloudEndpoint by remember { mutableStateOf(initialConfig.cloudEndpoint) }
    var cloudApiKey by remember { mutableStateOf(initialConfig.cloudApiKey) }
    var cloudModelName by remember { mutableStateOf(initialConfig.cloudModelName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Настройки AI Gemma", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Выберите режим работы нейросети Gemma:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                GemmaBackendType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedType == type),
                            onClick = { selectedType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = type.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedType) {
                    GemmaBackendType.OFFLINE_INTELLIGENCE -> {
                        Text(
                            text = "Встроенный офлайн-движок готов к работе сразу. Рассчитывает биомеханику, высотные коэффициенты и геодезию в автономном режиме без интернета.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    GemmaBackendType.ON_DEVICE_MEDIAPIPE -> {
                        Text(
                            text = "Укажите абсолютный путь к файлу модели Gemma (.bin или .task) на вашем устройстве:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = modelPath,
                            onValueChange = { modelPath = it },
                            label = { Text("Путь к модели (.bin / .task)") },
                            placeholder = { Text("/sdcard/Download/gemma-2b-it-gpu-int4.bin") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    GemmaBackendType.CLOUD_API -> {
                        Text(
                            text = "Быстрые шаблоны провайдеров Gemma:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = cloudEndpoint.contains("generativelanguage.googleapis.com"),
                                onClick = {
                                    cloudEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemma-2-9b-it:generateContent"
                                    cloudModelName = "gemma-2-9b-it"
                                },
                                label = { Text("Google AI Studio (Офиц.)", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = cloudEndpoint.contains("groq.com"),
                                onClick = {
                                    cloudEndpoint = "https://api.groq.com/openai/v1/chat/completions"
                                    cloudModelName = "gemma2-9b-it"
                                },
                                label = { Text("Groq (Бесплатно)", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = cloudEndpoint.contains("openrouter.ai"),
                                onClick = {
                                    cloudEndpoint = "https://openrouter.ai/api/v1/chat/completions"
                                    cloudModelName = "google/gemma-2-9b-it"
                                },
                                label = { Text("OpenRouter", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = cloudEndpoint.contains("11434"),
                                onClick = {
                                    cloudEndpoint = "http://10.0.2.2:11434/api/generate"
                                    cloudModelName = "gemma2"
                                },
                                label = { Text("Ollama", style = MaterialTheme.typography.labelSmall) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = cloudEndpoint,
                            onValueChange = { cloudEndpoint = it },
                            label = { Text("URL эндпоинта") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cloudApiKey,
                            onValueChange = { cloudApiKey = it },
                            label = { Text(if (cloudEndpoint.contains("generativelanguage.googleapis.com")) "Google AI API Key" else "API Ключ (Bearer token)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cloudModelName,
                            onValueChange = { cloudModelName = it },
                            label = { Text("Имя модели") },
                            placeholder = { Text("gemma-2-9b-it") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initialConfig.copy(
                            backendType = selectedType,
                            modelFilePath = modelPath.trim(),
                            cloudEndpoint = cloudEndpoint.trim(),
                            cloudApiKey = cloudApiKey.trim(),
                            cloudModelName = cloudModelName.trim()
                        )
                    )
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
