package com.flathike.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flathike.app.data.DemoTracks
import com.flathike.app.model.EffectiveDistanceResult
import com.flathike.app.model.HikingPreset
import com.flathike.app.model.RecordedTimeMetrics
import com.flathike.app.model.RecordedTemperatureMetrics
import com.flathike.app.model.TrackCalculationResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FlatHikeMainScreen(
    viewModel: FlatHikeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val strings = remember(uiState.appLanguage) { AppStrings(uiState.appLanguage) }

    var showDemoDialog by remember { mutableStateOf(false) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(false) }

    // System File Picker for GPX, KML, GeoJSON, TXT, CSV
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            handleFileUri(context, uri, viewModel)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FlatHike",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "*/*",
                                "application/gpx+xml",
                                "application/vnd.google-earth.kml+xml",
                                "application/geo+json",
                                "text/plain",
                                "text/csv"
                            )
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Загрузить трек",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { showDemoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Landscape,
                            contentDescription = strings.demoTracks,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { viewModel.openSettings() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = strings.settings,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Меню")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.pasteText) },
                            leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showPasteDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.fetchDemElevations) },
                            leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.fetchElevationProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.settings) },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.openSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (strings.isRu) "Справка: коэффициенты подъема" else "Help: climb coefficients") },
                            leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showExplanationDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Track Info Header
            val track = uiState.currentTrack
            val result = uiState.calculationResult

            if (track != null && result != null) {
                // Route title card
                RouteHeaderCard(
                    trackName = track.name,
                    trackDesc = track.description,
                    pointCount = result.pointCount,
                    startEle = result.startPoint.elevation,
                    endEle = result.endPoint.elevation,
                    isClosedLoop = result.isClosedLoop,
                    maxEle = result.maxElevationMeters
                )

                Spacer(modifier = Modifier.height(12.dp))

                // DEM Elevation Missing / Loading Alert Card
                if (result.isElevationMissing || uiState.isFetchingElevation || uiState.elevationFetchError != null) {
                    ElevationMissingCard(
                        isElevationMissing = result.isElevationMissing,
                        isFetching = uiState.isFetchingElevation,
                        progress = uiState.elevationFetchProgress,
                        errorMessage = uiState.elevationFetchError,
                        onFetchClick = { viewModel.fetchElevationProfile() },
                        onDismissError = { viewModel.dismissElevationError() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Card 1: Hero Card - Flat Surface Equivalent & Walking Time
                FlatEffortAndTimeHeroCard(
                    effectiveResult = uiState.effectiveDistanceResult,
                    selectedPreset = uiState.selectedPreset,
                    effectiveCoeff = uiState.effectiveCoefficient,
                    walkingSpeed = uiState.walkingSpeedKmH,
                    estimatedMinutes = uiState.estimatedHikingMinutes,
                    applyToTrackLength = uiState.applyToTrackLength,
                    totalAscentMeters = result.totalAscentMeters,
                    onSpeedChange = { viewModel.setWalkingSpeed(it) },
                    onOpenSettings = { viewModel.openSettings() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card 1.5: Recorded Time, Speed & Temperature (if present and enabled)
                val showRecordedBlock = (result.hasRecordedTime && uiState.showRecordedTimeSpeed) ||
                        (result.hasRecordedTemperature && uiState.showRecordedTemperature)
                if (showRecordedBlock) {
                    RecordedTrackPerformanceCard(
                        timeMetrics = result.recordedTimeMetrics,
                        tempMetrics = result.recordedTemperatureMetrics,
                        showTimeSpeed = uiState.showRecordedTimeSpeed,
                        showTemp = uiState.showRecordedTemperature
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Card 2: Key Route Metrics & Optional Geodetic Details
                RouteKeyMetricsCard(
                    result = result,
                    showAdvancedGeodetic = uiState.showAdvancedGeodetic,
                    onToggleAdvanced = { viewModel.toggleAdvancedGeodetic() },
                    onInfoClick = { showExplanationDialog = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card 3: Elevation Profile Chart with Waypoints
                ElevationProfileChart(
                    points = result.elevationProfile,
                    waypoints = result.waypoints,
                    selectedWaypoint = uiState.selectedWaypoint,
                    onWaypointSelected = { viewModel.selectWaypoint(it) },
                    appStrings = strings
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card 4: Track Segments & Speed Analysis Card
                TrackSegmentsCard(
                    calculationResult = result,
                    currentSplitMode = uiState.trackSplitMode,
                    onSplitModeChange = { viewModel.setSplitMode(it) },
                    onAddWaypointClick = { viewModel.openAddWaypointDialog() },
                    appStrings = strings
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card 5: AI Gemma Assistant Card
                GemmaAssistantCard(
                    config = uiState.gemmaConfig,
                    reportText = uiState.gemmaReport,
                    answerText = uiState.gemmaAnswer,
                    isLoading = uiState.isGemmaLoading,
                    errorMessage = uiState.gemmaError,
                    onRequestReport = { viewModel.requestGemmaReport() },
                    onAskQuestion = { viewModel.askGemmaQuestion(it) },
                    onOpenSettings = { viewModel.openSettings() }
                )
            } else {
                EmptyStateCard(
                    onSelectDemo = { showDemoDialog = true },
                    onOpenFilePicker = {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Demo Tracks Selection Dialog
    if (showDemoDialog) {
        DemoTracksDialog(
            onDismiss = { showDemoDialog = false },
            onTrackSelected = {
                viewModel.loadTrack(it)
                showDemoDialog = false
            }
        )
    }

    // Paste Track Dialog
    if (showPasteDialog) {
        PasteTrackDialog(
            onDismiss = { showPasteDialog = false },
            onApply = {
                viewModel.loadTrackFromText(it)
                showPasteDialog = false
            }
        )
    }

    // App Settings Dialog (Coefficients, Appearance & Gemma)
    if (uiState.isSettingsOpen) {
        AppSettingsDialog(
            currentPreset = uiState.selectedPreset,
            currentCustomCoeff = uiState.customCoefficient,
            currentApplyToTrackLength = uiState.applyToTrackLength,
            currentWalkingSpeed = uiState.walkingSpeedKmH,
            currentShowRecordedTimeSpeed = uiState.showRecordedTimeSpeed,
            currentShowRecordedTemperature = uiState.showRecordedTemperature,
            currentGemmaConfig = uiState.gemmaConfig,
            currentThemeMode = uiState.themeMode,
            currentAppLanguage = uiState.appLanguage,
            onDismiss = { viewModel.closeSettings() },
            onSave = { preset, customCoeff, applyToTrack, speed, showTimeSpeed, showTemp, gemmaConfig, themeMode, appLanguage ->
                viewModel.saveSettings(preset, customCoeff, applyToTrack, speed, showTimeSpeed, showTemp, gemmaConfig, themeMode, appLanguage)
            }
        )
    }

    // Add Waypoint Dialog
    if (uiState.isAddWaypointDialogOpen) {
        AddWaypointDialog(
            maxDistanceKm = uiState.calculationResult?.trackLengthSeaLevelKm ?: 10.0,
            onDismiss = { viewModel.closeAddWaypointDialog() },
            onSave = { name, distKm, desc ->
                viewModel.addWaypoint(name, distKm, desc)
            },
            appStrings = strings
        )
    }

    // Explanation Dialog
    if (showExplanationDialog) {
        ExplanationDialog(onDismiss = { showExplanationDialog = false })
    }
}

private fun handleFileUri(context: Context, uri: Uri, viewModel: FlatHikeViewModel) {
    try {
        val contentResolver = context.contentResolver
        var filename = "imported_track.gpx"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                filename = cursor.getString(nameIndex) ?: filename
            }
        }
        contentResolver.openInputStream(uri)?.use { stream ->
            viewModel.loadTrackFromStream(stream, filename)
        }
    } catch (e: Exception) {
        viewModel.setTrackError("Не удалось открыть файл: ${e.localizedMessage ?: "ошибка чтения"}")
    }
}

@Composable
fun ElevationMissingCard(
    isElevationMissing: Boolean,
    isFetching: Boolean,
    progress: Float,
    errorMessage: String?,
    onFetchClick: () -> Unit,
    onDismissError: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (errorMessage != null) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (errorMessage != null) Icons.Default.WarningAmber else Icons.Default.Terrain,
                    contentDescription = null,
                    tint = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isElevationMissing) "Профиль высоты отсутствует (2D трек)" else "Загрузка высот DEM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isElevationMissing) {
                            "Все точки имеют высоту 0 м. Набор высоты, 3D дистанция и расчет времени не могут быть вычислены без данных рельефа."
                        } else {
                            "Обновление профиля высот из открытой цифровой модели рельефа DEM (SRTM/Copernicus)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (errorMessage != null && onDismissError != null) {
                    IconButton(
                        onClick = onDismissError,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isFetching) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Запрос открытой модели рельефа DEM (SRTM/Copernicus)... ${(progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.tertiary
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onFetchClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (errorMessage != null) "Повторить загрузку высот (DEM)" else "Загрузить высоты с DEM (SRTM/Copernicus)",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun RouteHeaderCard(
    trackName: String,
    trackDesc: String?,
    pointCount: Int,
    startEle: Double,
    endEle: Double,
    isClosedLoop: Boolean = false,
    maxEle: Double = 0.0
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isClosedLoop) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Кольцевой маршрут (старт ≈ финиш)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (!trackDesc.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trackDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row with dedicated spacing so point count and elevation never collide
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Точек: $pointCount",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Высоты: ${startEle.toInt()} → ${endEle.toInt()} м (пик ${maxEle.toInt()} м)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun FlatEffortAndTimeHeroCard(
    effectiveResult: EffectiveDistanceResult?,
    selectedPreset: HikingPreset,
    effectiveCoeff: Double,
    walkingSpeed: Double,
    estimatedMinutes: Double,
    applyToTrackLength: Boolean,
    totalAscentMeters: Double,
    onSpeedChange: (Double) -> Unit,
    onOpenSettings: () -> Unit
) {
    if (effectiveResult == null) return

    val totalMinutes = estimatedMinutes.roundToInt()
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    val timeFormatted = if (hours > 0) "$hours ч $mins мин" else "$mins мин"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: Title + Settings shortcut badge (never squishes vertically)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ПО РОВНОЙ ПОВЕРХНОСТИ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Preset Badge (guaranteed single-line, clickable)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.clickable { onOpenSettings() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedPreset.title.substringBefore(' ')} k=${String.format(Locale.US, "%.0f", effectiveCoeff)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки коэффициента",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two-column Hero Display: Left = Equivalent Distance, Right = Estimated Time (symmetrically aligned)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Column 1: Equivalent distance on flat surface
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ДИСТАНЦИЯ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.2f", effectiveResult.effectiveDistanceKm),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "км",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (applyToTrackLength) {
                                "База ${String.format(Locale.US, "%.1f", effectiveResult.baseDistanceKm)} + ${String.format(Locale.US, "%.1f", effectiveResult.ascentBonusKm)} км"
                            } else {
                                "Прямая ${String.format(Locale.US, "%.1f", effectiveResult.baseDistanceKm)} + ${String.format(Locale.US, "%.1f", effectiveResult.ascentBonusKm)} км"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Column 2: Estimated hiking time
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ВРЕМЯ ХОДА",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "при ${String.format(Locale.US, "%.1f", walkingSpeed)} км/ч",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Speed Control Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Средняя скорость ходьбы:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", walkingSpeed)} км/ч",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Continuous Slider (compact and clean)
            Slider(
                value = walkingSpeed.toFloat(),
                onValueChange = { onSpeedChange(it.toDouble()) },
                valueRange = 1.5f..8.0f,
                steps = 12,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1.5 км/ч",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "8.0 км/ч",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun RecordedTrackPerformanceCard(
    timeMetrics: RecordedTimeMetrics?,
    tempMetrics: RecordedTemperatureMetrics?,
    showTimeSpeed: Boolean,
    showTemp: Boolean
) {
    val showTime = showTimeSpeed && timeMetrics != null
    val showTemperature = showTemp && tempMetrics != null

    if (!showTime && !showTemperature) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showTime) Icons.Default.Speed else Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "ФАКТИЧЕСКИЕ ДАННЫЕ ЗАПИСИ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (showTime && showTemperature) {
                            "Хронометраж, реальная скорость и температура воздуха"
                        } else if (showTime) {
                            "Хронометраж и реальная скорость по меткам трека"
                        } else {
                            "Температурный профиль по датчикам трека"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (timeMetrics != null && showTimeSpeed) {
                Spacer(modifier = Modifier.height(14.dp))

                fun formatSec(sec: Long): String {
                    val h = sec / 3600
                    val m = (sec % 3600) / 60
                    val s = sec % 60
                    return if (h > 0) "${h} ч ${m} мин" else "${m} мин ${s} с"
                }

                fun formatClock(millis: Long): String {
                    val sdf = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                    return sdf.format(java.util.Date(millis))
                }

                val paceMin = timeMetrics.paceMinutesPerKm.toInt()
                val paceSec = ((timeMetrics.paceMinutesPerKm - paceMin) * 60).roundToInt()
                val paceStr = if (timeMetrics.paceMinutesPerKm in 0.5..120.0) {
                    String.format(Locale.US, "%d:%02d /км", paceMin, paceSec)
                } else "—"

                // Row 1: Скорость в движении & Время в движении
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyMetricBox(
                        title = "Скорость в движении",
                        value = "${String.format(Locale.US, "%.1f", timeMetrics.avgMovingSpeedKmH)} км/ч",
                        subtitle = "макс. ${String.format(Locale.US, "%.1f", timeMetrics.maxSpeedKmH)} км/ч",
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )

                    KeyMetricBox(
                        title = "Время в движении",
                        value = formatSec(timeMetrics.movingDurationSeconds),
                        subtitle = "остановки: ${formatSec(timeMetrics.stoppedDurationSeconds)}",
                        icon = Icons.Default.Timer,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Темп движения & Интервал времени
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyMetricBox(
                        title = "Темп движения",
                        value = paceStr,
                        subtitle = "общая ср.: ${String.format(Locale.US, "%.1f", timeMetrics.avgSpeedKmH)} км/ч",
                        icon = Icons.Default.DirectionsWalk,
                        modifier = Modifier.weight(1f)
                    )

                    KeyMetricBox(
                        title = "Время записи",
                        value = "${formatClock(timeMetrics.startTimeMillis)} — ${formatClock(timeMetrics.endTimeMillis)}",
                        subtitle = "всего: ${formatSec(timeMetrics.totalDurationSeconds)}",
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (tempMetrics != null && showTemp) {
                if (showTime) {
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Thermostat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Температура воздуха",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(Locale.US, "ср. %+.1f°C", tempMetrics.avgCelsius),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Диапазон: ${String.format(Locale.US, "%+.1f°C", tempMetrics.minCelsius)} … ${String.format(Locale.US, "%+.1f°C", tempMetrics.maxCelsius)}   •   Старт / финиш: ${String.format(Locale.US, "%+.0f°", tempMetrics.startCelsius)} / ${String.format(Locale.US, "%+.0f°C", tempMetrics.endCelsius)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RouteKeyMetricsCard(
    result: TrackCalculationResult,
    showAdvancedGeodetic: Boolean,
    onToggleAdvanced: () -> Unit,
    onInfoClick: () -> Unit
) {
    val straightKm = if (result.isClosedLoop) result.maxDistanceFromStartKm else result.straightLineSeaLevelKm
    val tortuosity = if (straightKm > 0.001) result.trackLengthSeaLevelKm / straightKm else 1.0
    val reductionRatio = if (result.trackLength3dKm > 0.0) {
        (result.geodeticReductionMeters / (result.trackLength3dKm * 1000.0))
    } else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Straighten,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "КЛЮЧЕВЫЕ ПАРАМЕТРЫ ТРЕКА",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "О геодезической редукции",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2x2 Grid of Key Metrics (symmetrically aligned)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeyMetricBox(
                    title = "По прямой",
                    value = "${String.format(Locale.US, "%.2f", straightKm)} км",
                    subtitle = if (result.isClosedLoop) "макс. удаление" else "старт → финиш",
                    icon = Icons.Default.Straighten,
                    modifier = Modifier.weight(1f)
                )

                KeyMetricBox(
                    title = "Путь по треку",
                    value = "${String.format(Locale.US, "%.2f", result.trackLengthSeaLevelKm)} км",
                    subtitle = "${result.pointCount} GPS-точек",
                    icon = Icons.Default.AltRoute,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeyMetricBox(
                    title = "Набор / Сброс",
                    value = "+${result.totalAscentMeters.toInt()} / -${result.totalDescentMeters.toInt()} м",
                    subtitle = "уклон: ср. ${String.format(Locale.US, "%.1f", result.avgSlopePercent)}%",
                    icon = Icons.Default.Terrain,
                    modifier = Modifier.weight(1f)
                )

                KeyMetricBox(
                    title = "Высоты трека",
                    value = "${result.minElevationMeters.toInt()} – ${result.maxElevationMeters.toInt()} м",
                    subtitle = "перепад: ${result.elevationDifferenceMeters.toInt()} м",
                    icon = Icons.Default.Landscape,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable button for advanced geodesy
            TextButton(
                onClick = onToggleAdvanced,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (showAdvancedGeodetic) {
                        "▲ Скрыть геодезические детали"
                    } else {
                        "▼ Подробная геодезия (WGS-84, 3D хорда, уклоны)"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Advanced Geodetic Details spoiler
            AnimatedVisibility(visible = showAdvancedGeodetic) {
                val toblerTotal = result.toblerHikingTimeMinutes.roundToInt()
                val toblerH = toblerTotal / 60
                val toblerM = toblerTotal % 60
                val naismithTotal = result.naismithHikingTimeMinutes.roundToInt()
                val naismithH = naismithTotal / 60
                val naismithM = naismithTotal % 60
                val maxSlopeDegrees = (kotlin.math.atan(result.maxSlopePercent / 100.0) * 180.0 / Math.PI).roundToInt()
                val terrainExtensionPercent = if (result.trackLengthSeaLevelKm > 0.0) {
                    (result.terrainExtensionDifferenceMeters / (result.trackLengthSeaLevelKm * 1000.0)) * 100.0
                } else 0.0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Геодезические параметры WGS-84",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Редукция эллипсоида, пространственные хорды и модели ходьбы",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Group 1: Кривизна Земли
                    Text(
                        text = "КРИВИЗНА ЗЕМЛИ И ЭЛЛИПСОИД WGS-84",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )

                    GeodeticParameterCard(
                        title = "Редукция высоты на эллипсоид",
                        value = "-${String.format(Locale.US, "%.1f", result.geodeticReductionMeters)} м",
                        valueBadge = "-${String.format(Locale.US, "%.4f", reductionRatio * 100)}%",
                        description = "Поправка за счет средней высоты маршрута ${result.avgElevationMeters.toInt()} м над геоидом. Из-за радиуса Земли (6371 км) реальный путь на высоте физически длиннее своей проекции на уровень моря.",
                        icon = Icons.Default.Public
                    )

                    GeodeticParameterCard(
                        title = if (result.isClosedLoop) "Хорда замыкания кольца (Старт ↔ Финиш)" else "3D Хорда сквозь геоид (Старт → Финиш)",
                        value = if (result.isClosedLoop) {
                            "${String.format(Locale.US, "%.3f", result.straightLineSeaLevelChordKm)} км (${(result.straightLineSeaLevelChordKm * 1000).roundToInt()} м)"
                        } else {
                            "${String.format(Locale.US, "%.3f", result.straightLine3dKm)} км"
                        },
                        valueBadge = if (result.isClosedLoop) "Невязка кольца" else "Хорда ECEF",
                        description = if (result.isClosedLoop) {
                            "Геометрическое расстояние между точками старта и завершения трека (точность сведения кольцевого маршрута)."
                        } else {
                            "Кратчайший евклидов отрезок в трехмерном декартовом пространстве ECEF сквозь тело Земли в обход кривизны эллипсоида."
                        },
                        icon = Icons.Default.LinearScale
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Group 2: Геометрия пути и рельеф
                    Text(
                        text = "ГЕОМЕТРИЯ ПУТИ И МИКРОРЕЛЬЕФ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )

                    GeodeticParameterCard(
                        title = "Коэффициент извилистости (Track / Direct)",
                        value = "${String.format(Locale.US, "%.2f", tortuosity)}×",
                        description = if (result.isClosedLoop) {
                            "Отношение длины пути (${String.format(Locale.US, "%.2f", result.trackLengthSeaLevelKm)} км) к максимальному радиальному удалению (${String.format(Locale.US, "%.2f", straightKm)} км). Показывает, насколько сильно маршрут петляет."
                        } else {
                            "Отношение длины пути (${String.format(Locale.US, "%.2f", result.trackLengthSeaLevelKm)} км) к прямому расстоянию старт-финиш (${String.format(Locale.US, "%.2f", straightKm)} км)."
                        },
                        icon = Icons.Default.AltRoute
                    )

                    GeodeticParameterCard(
                        title = "Истинная 3D-длина по рельефу",
                        value = "${String.format(Locale.US, "%.2f", result.trackLength3dKm)} км",
                        valueBadge = "+${result.terrainExtensionDifferenceMeters.toInt()} м (+${String.format(Locale.US, "%.2f", terrainExtensionPercent)}%)",
                        description = "Реальное физическое трехмерное расстояние с учетом всех подъемов, спусков и кривизны склонов по отношению к горизонтальной проекции.",
                        icon = Icons.Default.Terrain
                    )

                    GeodeticParameterCard(
                        title = "Максимальный уклон склона",
                        value = "${String.format(Locale.US, "%.1f", result.maxSlopePercent)}%",
                        valueBadge = "~$maxSlopeDegrees°",
                        description = "Крутизна самого крутого сегмента пути с фильтрацией шумов GPS-высотомера (средний уклон по всему маршруту: ${String.format(Locale.US, "%.1f", result.avgSlopePercent)}%).",
                        icon = Icons.Default.Landscape
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Group 3: Физические модели времени ходьбы
                    Text(
                        text = "ФИЗИЧЕСКИЕ МОДЕЛИ ВРЕМЕНИ В ПУТИ (БЕЗ ПРИВАЛОВ)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HikingModelCard(
                            title = "Функция Тоблера",
                            value = "${toblerH}ч ${toblerM}м",
                            formula = "W = 6 · e^(-3.5·|s+0.05|)",
                            description = "Учитывает уклон каждого отрезка трека (замедляет на крутых склонах, ускоряет на спуске -5%).",
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )

                        HikingModelCard(
                            title = "Правило Найсмита",
                            value = "${naismithH}ч ${naismithM}м",
                            formula = "5 км/ч + 1ч / 600м набора",
                            description = "Классический швейцарский альпинистский норматив SAC (1892 г.) для непрерывного движения.",
                            icon = Icons.Default.Schedule,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Group 4: Справка по формуле
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Functions,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Формула редукции к уровню моря (WGS-84)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Δs = s · h_avg / (R + h_avg)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Где s — длина пути, h_avg — средняя высота (${result.avgElevationMeters.toInt()} м), R ≈ 6 371 008 м — средний радиус Земли WGS-84. На больших высотах длина маршрута по физической поверхности Земли всегда физически превышает ее идеальную геодезическую проекцию на эллипсоид уровня моря.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyMetricBox(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GeodeticParameterCard(
    title: String,
    value: String,
    valueBadge: String? = null,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (valueBadge != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = valueBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun HikingModelCard(
    title: String,
    value: String,
    formula: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = formula,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun DemoTracksDialog(
    onDismiss: () -> Unit,
    onTrackSelected: (com.flathike.app.model.GpsTrack) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Выберите демо-трек", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DemoTracks.allTracks.forEach { demo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onTrackSelected(demo) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = demo.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (demo.description != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = demo.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
fun PasteTrackDialog(
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вставить координаты или GPX XML") },
        text = {
            Column {
                Text(
                    text = "Поддерживается формат GPX, KML, GeoJSON или строки 'широта, долгота, высота':",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("55.751244, 37.618423, 150\n55.755814, 37.617635, 155") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(rawText) },
                enabled = rawText.isNotBlank()
            ) {
                Text("Загрузить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun ExplanationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Справка о расчетах FlatHike") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "1. По прямой на уровне моря (WGS-84)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ортодромия — кратчайшее расстояние между координатами старта и финиша вдоль поверхности Земли на нулевой высоте (на уровне моря). В отличие от 3D хорды, огибает земную кривизну по эллипсоиду WGS-84.",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "2. Коэффициенты при подъеме",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "При подъеме человек совершает работу против гравитации (A = m·g·h). КПД мышц на подъеме составляет ~20-25%.\n\n" +
                            "• Швейцарский SAC / Naismith (k = 10.0): 100 м набора = +1 км равнины.\n" +
                            "• NOLS Petzoldt (k = 8.0): 100 м набора = +0.8 км равнины.\n" +
                            "• ФСТР (k = 6.0): 100 м набора = +0.6 км равнины.\n" +
                            "• Трейлраннинг (k = 4.0): налегке с палками.",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "3. Интеллект AI Gemma",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Приложение использует открытую нейросеть Gemma от Google для детального топографического, физиологического и геодезического анализа ваших походов.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Понятно") }
        }
    )
}

@Composable
fun EmptyStateCard(
    onSelectDemo: () -> Unit,
    onOpenFilePicker: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Загрузите GPS трек",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Поддерживаются файлы GPX, KML, GeoJSON, CSV, а также готовые демонстрационные маршруты.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onSelectDemo) {
                    Text("Выбрать демо-трек")
                }
                TextButton(onClick = onOpenFilePicker) {
                    Text("Открыть файл")
                }
            }
        }
    }
}
