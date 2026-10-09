package com.flathike.app.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Palette
import com.flathike.app.model.AppLanguage
import com.flathike.app.model.ThemeMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flathike.app.ai.GemmaBackendType
import com.flathike.app.ai.GemmaConfig
import com.flathike.app.model.HikingPreset
import java.util.Locale

/**
 * Unified Settings Dialog:
 * Tab 0: Elevation coefficients, base distance, speed, and track sensors (time, temperature).
 * Tab 1: AI Gemma configuration (Offline, MediaPipe On-Device, Cloud API).
 */
@Composable
fun AppSettingsDialog(
    currentPreset: HikingPreset,
    currentCustomCoeff: Double,
    currentApplyToTrackLength: Boolean,
    currentWalkingSpeed: Double,
    currentShowRecordedTimeSpeed: Boolean = true,
    currentShowRecordedTemperature: Boolean = true,
    currentGemmaConfig: GemmaConfig,
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    currentAppLanguage: AppLanguage = AppLanguage.SYSTEM,
    onDismiss: () -> Unit,
    onSave: (
        preset: HikingPreset,
        customCoeff: Double,
        applyToTrackLength: Boolean,
        walkingSpeed: Double,
        showRecordedTimeSpeed: Boolean,
        showRecordedTemperature: Boolean,
        gemmaConfig: GemmaConfig,
        themeMode: ThemeMode,
        appLanguage: AppLanguage
    ) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Tab 0 state: Coefficients & Hiking & Track Data
    var selectedPreset by remember { mutableStateOf(currentPreset) }
    var customCoeff by remember { mutableDoubleStateOf(currentCustomCoeff) }
    var applyToTrackLength by remember { mutableStateOf(currentApplyToTrackLength) }
    var walkingSpeed by remember { mutableDoubleStateOf(currentWalkingSpeed) }
    var showRecordedTimeSpeed by remember { mutableStateOf(currentShowRecordedTimeSpeed) }
    var showRecordedTemperature by remember { mutableStateOf(currentShowRecordedTemperature) }

    // Tab 1 state: Appearance & Language
    var selectedThemeMode by remember { mutableStateOf(currentThemeMode) }
    var selectedAppLanguage by remember { mutableStateOf(currentAppLanguage) }
    val strings = AppStrings(selectedAppLanguage)

    // Tab 2 state: AI Gemma
    var gemmaBackendType by remember { mutableStateOf(currentGemmaConfig.backendType) }
    var modelPath by remember { mutableStateOf(currentGemmaConfig.modelFilePath) }
    var cloudEndpoint by remember { mutableStateOf(currentGemmaConfig.cloudEndpoint) }
    var cloudApiKey by remember { mutableStateOf(currentGemmaConfig.cloudApiKey) }
    var cloudModelName by remember { mutableStateOf(currentGemmaConfig.cloudModelName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.settings,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(strings.tabCoefficients) },
                        icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(strings.tabAppearance) },
                        icon = { Icon(Icons.Default.Palette, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(strings.tabAiGemma) },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // TAB 0: Elevation coefficients & walking speed
                    Text(
                        text = "Стандарт коэффициента подъема:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Определяет, сколько метров набора высоты эквивалентно 1 км ходьбы по равнине:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    HikingPreset.entries.forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedPreset = preset },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedPreset = preset }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Custom slider if CUSTOM selected
                    AnimatedVisibility(visible = selectedPreset == HikingPreset.CUSTOM) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Пользовательский k: ${String.format(Locale.US, "%.1f", customCoeff)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "+${String.format(Locale.US, "%.1f", customCoeff / 10.0)} км за 100м набора",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = customCoeff.toFloat(),
                                onValueChange = { customCoeff = it.toDouble() },
                                valueRange = 0.0f..20.0f,
                                steps = 39
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Base distance selection
                    Text(
                        text = "Базовая дистанция для расчета:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyToTrackLength = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = applyToTrackLength,
                            onClick = { applyToTrackLength = true }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "К фактической длине трека (Рекомендуется)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (applyToTrackLength) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                text = "Длина тропы по GPS + надбавка за подъем",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyToTrackLength = false }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !applyToTrackLength,
                            onClick = { applyToTrackLength = false }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "К расстоянию по прямой на уровне моря",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (!applyToTrackLength) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                text = "Прямая линия между точками + надбавка за подъем",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Default speed
                    Text(
                        text = "Скорость ходьбы по умолчанию: ${String.format(Locale.US, "%.1f", walkingSpeed)} км/ч",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Используется для начального расчета времени пути",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = walkingSpeed.toFloat(),
                        onValueChange = { walkingSpeed = it.toDouble() },
                        valueRange = 2.0f..8.0f,
                        steps = 11
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ДАННЫЕ ТРЕКА И ДАТЧИКИ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Отображение фактических параметров при их наличии в файле трека:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Switch 1: Recorded time and speed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRecordedTimeSpeed = !showRecordedTimeSpeed }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = showRecordedTimeSpeed,
                            onCheckedChange = { showRecordedTimeSpeed = it }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Фактическое время и реальная скорость",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Показывать время движения, среднюю и макс. скорость и темп по GPS-меткам",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Switch 2: Recorded temperature
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRecordedTemperature = !showRecordedTemperature }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = showRecordedTemperature,
                            onCheckedChange = { showRecordedTemperature = it }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Температура воздуха",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Показывать среднюю и мин/макс температуру из точек трека (Garmin / atemp)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (selectedTab == 1) {
                    // TAB 1: Theme & Language
                    Text(
                        text = strings.themeSectionTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ThemeMode.entries.forEach { mode ->
                        val isSelected = selectedThemeMode == mode
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedThemeMode = mode },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedThemeMode = mode }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (strings.isRu) mode.titleRu else mode.titleEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = strings.languageSectionTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AppLanguage.entries.forEach { lang ->
                        val isSelected = selectedAppLanguage == lang
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { selectedAppLanguage = lang },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedAppLanguage = lang }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (strings.isRu) lang.titleRu else lang.titleEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                } else {
                    // TAB 2: AI Gemma configuration
                    Text(
                        text = "Выберите режим работы нейросети Gemma:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GemmaBackendType.entries.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { gemmaBackendType = type }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (gemmaBackendType == type),
                                onClick = { gemmaBackendType = type }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = type.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (gemmaBackendType == type) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    text = type.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (gemmaBackendType) {
                        GemmaBackendType.OFFLINE_INTELLIGENCE -> {
                            Text(
                                text = "Встроенный офлайн-эксперт готов к работе сразу. Рассчитывает биомеханику, коэффициенты и рельеф автономно без интернета.",
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
                                    label = { Text("Groq", style = MaterialTheme.typography.labelSmall) }
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedGemma = currentGemmaConfig.copy(
                        backendType = gemmaBackendType,
                        modelFilePath = modelPath.trim(),
                        cloudEndpoint = cloudEndpoint.trim(),
                        cloudApiKey = cloudApiKey.trim(),
                        cloudModelName = cloudModelName.trim()
                    )
                    onSave(
                        selectedPreset,
                        customCoeff,
                        applyToTrackLength,
                        walkingSpeed,
                        showRecordedTimeSpeed,
                        showRecordedTemperature,
                        updatedGemma,
                        selectedThemeMode,
                        selectedAppLanguage
                    )
                }
            ) {
                Text(strings.save, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
