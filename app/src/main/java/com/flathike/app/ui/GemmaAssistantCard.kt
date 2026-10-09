package com.flathike.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flathike.app.ai.GemmaBackendType
import com.flathike.app.ai.GemmaConfig

@Composable
fun GemmaAssistantCard(
    config: GemmaConfig,
    reportText: String?,
    answerText: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onRequestReport: () -> Unit,
    onAskQuestion: (String) -> Unit,
    onOpenSettings: () -> Unit,
    appStrings: AppStrings? = null,
    modifier: Modifier = Modifier
) {
    var customQuestion by remember { mutableStateOf("") }
    val isRu = appStrings?.isRu ?: true

    val quickQuestions = if (isRu) listOf(
        "Зачем коэффициенты подъема?",
        "Что такое прямая на уровне моря?",
        "Оценка сложности для новичка",
        "Сколько воды и еды взять?",
        "Рекомендации по экипировке"
    ) else listOf(
        "Why climb coefficients?",
        "What is straight-line at sea level?",
        "Difficulty rating for beginners",
        "How much food and water to bring?",
        "Gear and clothing advice"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Gemma",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Gemma Trail Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (config.backendType) {
                            GemmaBackendType.OFFLINE_INTELLIGENCE -> if (isRu) "Встроенный офлайн AI • Готов" else "Built-in Offline AI • Ready"
                            GemmaBackendType.ON_DEVICE_MEDIAPIPE -> if (isRu) "Локальная Gemma (MediaPipe) • Активна" else "On-Device Gemma (MediaPipe) • Active"
                            GemmaBackendType.CLOUD_API -> if (isRu) "Gemma 2 Cloud API • Активна" else "Gemma 2 Cloud API • Active"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = if (isRu) "Настройки AI" else "AI Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Action Button
            Button(
                onClick = onRequestReport,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRu) "Gemma анализирует маршрут..." else "Gemma is analyzing route...")
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRu) "Сформировать полный отчет Gemma AI" else "Generate Full Gemma AI Report")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Questions Horizontal Scroll
            Text(
                text = if (isRu) "Быстрые вопросы:" else "Quick questions:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickQuestions.forEach { q ->
                    FilterChip(
                        selected = false,
                        onClick = { onAskQuestion(q) },
                        label = { Text(q, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom Question Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customQuestion,
                    onValueChange = { customQuestion = it },
                    placeholder = { Text(appStrings?.gemmaPromptPlaceholder ?: if (isRu) "Задайте вопрос Gemma о маршруте..." else "Ask Gemma about this trail...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (customQuestion.isNotBlank()) {
                            onAskQuestion(customQuestion)
                            customQuestion = ""
                        }
                    },
                    enabled = customQuestion.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .background(
                            if (customQuestion.isNotBlank()) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = if (isRu) "Отправить" else "Send",
                        tint = if (customQuestion.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Error Display
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // AI Answer or Report Display
            val displayText = answerText ?: reportText
            AnimatedVisibility(visible = displayText != null) {
                if (displayText != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp)
                    ) {
                        SelectionContainer {
                            FormattedMarkdown(
                                text = displayText,
                                textColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
