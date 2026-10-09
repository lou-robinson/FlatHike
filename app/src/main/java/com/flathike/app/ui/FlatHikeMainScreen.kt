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
            handleFileUri(context, uri, viewModel, strings)
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
                            contentDescription = strings.loadTrack,
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
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = strings.menu)
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
                            text = { Text(strings.helpClimbCoeffs) },
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
                    maxEle = result.maxElevationMeters,
                    strings = strings
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
                        onDismissError = { viewModel.dismissElevationError() },
                        strings = strings
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
                    onOpenSettings = { viewModel.openSettings() },
                    strings = strings
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
                        showTemp = uiState.showRecordedTemperature,
                        strings = strings
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Card 2: Key Route Metrics & Optional Geodetic Details
                RouteKeyMetricsCard(
                    result = result,
                    showAdvancedGeodetic = uiState.showAdvancedGeodetic,
                    onToggleAdvanced = { viewModel.toggleAdvancedGeodetic() },
                    onInfoClick = { showExplanationDialog = true },
                    strings = strings
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card 3: Elevation Profile Chart with Waypoints
                ElevationProfileChart(
                    points = result.elevationProfile,
                    waypoints = result.waypoints,
                    selectedWaypoint = uiState.selectedWaypoint,
                    onWaypointSelected = { viewModel.selectWaypoint(it) },
                    onDeleteWaypoint = { viewModel.deleteWaypoint(it) },
                    onAddWaypointAtDistance = { distKm -> viewModel.openAddWaypointDialog(distKm) },
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
                    onOpenSettings = { viewModel.openSettings() },
                    appStrings = strings
                )
            } else {
                EmptyStateCard(
                    onSelectDemo = { showDemoDialog = true },
                    onOpenFilePicker = {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    },
                    strings = strings
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
            },
            strings = strings
        )
    }

    // Paste Track Dialog
    if (showPasteDialog) {
        PasteTrackDialog(
            onDismiss = { showPasteDialog = false },
            onApply = {
                viewModel.loadTrackFromText(it)
                showPasteDialog = false
            },
            strings = strings
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
            appStrings = strings,
            initialDistanceKm = uiState.initialWaypointDistance
        )
    }

    // Explanation Dialog
    if (showExplanationDialog) {
        ExplanationDialog(onDismiss = { showExplanationDialog = false }, strings = strings)
    }
}

private fun handleFileUri(context: Context, uri: Uri, viewModel: FlatHikeViewModel, strings: AppStrings) {
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
        viewModel.setTrackError(strings.fileOpenError(e.localizedMessage ?: strings.fileReadError))
    }
}

@Composable
fun ElevationMissingCard(
    isElevationMissing: Boolean,
    isFetching: Boolean,
    progress: Float,
    errorMessage: String?,
    onFetchClick: () -> Unit,
    onDismissError: (() -> Unit)? = null,
    strings: AppStrings
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
                        text = if (isElevationMissing) strings.elevationMissingCardTitle else strings.demUpdatingTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isElevationMissing) {
                            strings.elevationMissingCardDesc
                        } else {
                            strings.demUpdatingDesc
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
                            contentDescription = strings.close,
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
                        text = strings.demRequestingProgress((progress * 100).roundToInt()),
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
                        text = if (errorMessage != null) strings.demRetryButton else strings.demFetchButton,
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
    maxEle: Double = 0.0,
    strings: AppStrings
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
                                text = strings.loopRoute,
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
                        text = strings.pointsCount(pointCount),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = strings.elevationsRange(startEle.toInt(), endEle.toInt(), maxEle.toInt()),
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
    onOpenSettings: () -> Unit,
    strings: AppStrings
) {
    if (effectiveResult == null) return

    val totalMinutes = estimatedMinutes.roundToInt()
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    val timeFormatted = if (hours > 0) "$hours ${strings.hours} $mins ${strings.min}" else "$mins ${strings.min}"

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
                            text = strings.flatEquivalentHeader,
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
                            text = "${selectedPreset.getTitle(strings.isRu).substringBefore(' ')} k=${String.format(Locale.US, "%.0f", effectiveCoeff)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = strings.coeffSettings,
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
                                text = strings.distanceHeader,
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
                                text = strings.km,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (applyToTrackLength) {
                                strings.basePlusBonus(effectiveResult.baseDistanceKm, effectiveResult.ascentBonusKm)
                            } else {
                                strings.straightPlusBonus(effectiveResult.baseDistanceKm, effectiveResult.ascentBonusKm)
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
                                text = strings.hikingTimeHeader,
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
                            text = strings.atSpeed(walkingSpeed),
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
                    text = strings.avgWalkingSpeed,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", walkingSpeed)} ${strings.kmh}",
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
                    text = "1.5 ${strings.kmh}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "8.0 ${strings.kmh}",
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
    showTemp: Boolean,
    strings: AppStrings
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
                        text = strings.recordedMetricsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (showTime && showTemperature) {
                            strings.recordedDataSubtitleBoth
                        } else if (showTime) {
                            strings.recordedDataSubtitleTime
                        } else {
                            strings.recordedDataSubtitleTemp
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
                    return if (h > 0) "$h ${strings.hours} $m ${strings.min}" else "$m ${strings.min} $s ${strings.sec}"
                }

                fun formatClock(millis: Long): String {
                    val sdf = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                    return sdf.format(java.util.Date(millis))
                }

                val paceMin = timeMetrics.paceMinutesPerKm.toInt()
                val paceSec = ((timeMetrics.paceMinutesPerKm - paceMin) * 60).roundToInt()
                val paceStr = if (timeMetrics.paceMinutesPerKm in 0.5..120.0) {
                    String.format(Locale.US, "%d:%02d ${strings.perKm}", paceMin, paceSec)
                } else "—"

                // Row 1: Скорость в движении & Время в движении
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyMetricBox(
                        title = strings.movingSpeed,
                        value = "${String.format(Locale.US, "%.1f", timeMetrics.avgMovingSpeedKmH)} ${strings.kmh}",
                        subtitle = strings.maxSpeedLabel(timeMetrics.maxSpeedKmH),
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )

                    KeyMetricBox(
                        title = strings.movingTime,
                        value = formatSec(timeMetrics.movingDurationSeconds),
                        subtitle = strings.stoppedDuration(formatSec(timeMetrics.stoppedDurationSeconds)),
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
                        title = strings.movingPace,
                        value = paceStr,
                        subtitle = strings.overallAvgSpeedLabel(timeMetrics.avgSpeedKmH),
                        icon = Icons.Default.DirectionsWalk,
                        modifier = Modifier.weight(1f)
                    )

                    KeyMetricBox(
                        title = strings.recordingTime,
                        value = "${formatClock(timeMetrics.startTimeMillis)} — ${formatClock(timeMetrics.endTimeMillis)}",
                        subtitle = strings.totalDurationLabel(formatSec(timeMetrics.totalDurationSeconds)),
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
                                    text = strings.airTempLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(Locale.US, "${if (strings.isRu) "ср." else "avg"} %+.1f°C", tempMetrics.avgCelsius),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.tempRangeLabel(tempMetrics.minCelsius, tempMetrics.maxCelsius, tempMetrics.startCelsius, tempMetrics.endCelsius),
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
    onInfoClick: () -> Unit,
    strings: AppStrings
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
                            text = strings.keyTrackParams,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = strings.geodeticsTitle,
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
                    title = strings.straightLineDistance,
                    value = "${String.format(Locale.US, "%.2f", straightKm)} ${strings.km}",
                    subtitle = if (result.isClosedLoop) strings.maxRadialDist else strings.startToFinish,
                    icon = Icons.Default.Straighten,
                    modifier = Modifier.weight(1f)
                )

                KeyMetricBox(
                    title = strings.pathAlongTrack,
                    value = "${String.format(Locale.US, "%.2f", result.trackLengthSeaLevelKm)} ${strings.km}",
                    subtitle = strings.gpsPointsCount(result.pointCount),
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
                    title = strings.ascentDescentLabel,
                    value = "+${result.totalAscentMeters.toInt()} / -${result.totalDescentMeters.toInt()} ${strings.meters}",
                    subtitle = "${strings.slope}: ${if (strings.isRu) "ср." else "avg"} ${String.format(Locale.US, "%.1f", result.avgSlopePercent)}%",
                    icon = Icons.Default.Terrain,
                    modifier = Modifier.weight(1f)
                )

                KeyMetricBox(
                    title = strings.trackElevationsLabel,
                    value = "${result.minElevationMeters.toInt()} – ${result.maxElevationMeters.toInt()} ${strings.meters}",
                    subtitle = strings.elevationDiffLabel(result.elevationDifferenceMeters.toInt()),
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
                        strings.hideGeodetics
                    } else {
                        strings.showGeodetics
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
                                text = strings.geodeticsHeader,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.geodeticsSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Group 1: Кривизна Земли
                    Text(
                        text = strings.groupEarthCurvature,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )

                    GeodeticParameterCard(
                        title = strings.reductionTitle,
                        value = "-${String.format(Locale.US, "%.1f", result.geodeticReductionMeters)} ${strings.meters}",
                        valueBadge = "-${String.format(Locale.US, "%.4f", reductionRatio * 100)}%",
                        description = strings.reductionDesc(result.avgElevationMeters.toInt()),
                        icon = Icons.Default.Public
                    )

                    GeodeticParameterCard(
                        title = if (result.isClosedLoop) strings.chordClosedTitle else strings.chord3dTitle,
                        value = if (result.isClosedLoop) {
                            "${String.format(Locale.US, "%.3f", result.straightLineSeaLevelChordKm)} ${strings.km} (${(result.straightLineSeaLevelChordKm * 1000).roundToInt()} ${strings.meters})"
                        } else {
                            "${String.format(Locale.US, "%.3f", result.straightLine3dKm)} ${strings.km}"
                        },
                        valueBadge = if (result.isClosedLoop) strings.loopMisclosureBadge else strings.chordEcefBadge,
                        description = if (result.isClosedLoop) strings.chordClosedDesc else strings.chord3dDesc,
                        icon = Icons.Default.LinearScale
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Group 2: Геометрия пути и рельеф
                    Text(
                        text = strings.groupGeometryTerrain,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )

                    GeodeticParameterCard(
                        title = strings.tortuosityTitle,
                        value = "${String.format(Locale.US, "%.2f", tortuosity)}×",
                        description = if (result.isClosedLoop) {
                            strings.tortuosityDescClosed(result.trackLengthSeaLevelKm, straightKm)
                        } else {
                            strings.tortuosityDescDirect(result.trackLengthSeaLevelKm, straightKm)
                        },
                        icon = Icons.Default.AltRoute
                    )

                    GeodeticParameterCard(
                        title = strings.true3dTitle,
                        value = "${String.format(Locale.US, "%.2f", result.trackLength3dKm)} ${strings.km}",
                        valueBadge = "+${result.terrainExtensionDifferenceMeters.toInt()} ${strings.meters} (+${String.format(Locale.US, "%.2f", terrainExtensionPercent)}%)",
                        description = strings.true3dDesc,
                        icon = Icons.Default.Terrain
                    )

                    GeodeticParameterCard(
                        title = strings.maxSlopeTitle,
                        value = "${String.format(Locale.US, "%.1f", result.maxSlopePercent)}%",
                        valueBadge = "~$maxSlopeDegrees°",
                        description = strings.maxSlopeDesc(result.avgSlopePercent),
                        icon = Icons.Default.Landscape
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Group 3: Физические модели времени ходьбы
                    Text(
                        text = strings.groupHikingModels,
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
                            title = strings.toblerTitle,
                            value = "${toblerH}${strings.hours} ${toblerM}${strings.min}",
                            formula = "W = 6 · e^(-3.5·|s+0.05|)",
                            description = strings.toblerDesc,
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )

                        HikingModelCard(
                            title = strings.naismithTitle,
                            value = "${naismithH}${strings.hours} ${naismithM}${strings.min}",
                            formula = if (strings.isRu) "5 км/ч + 1ч / 600м набора" else "5 km/h + 1h / 600m climb",
                            description = strings.naismithDesc,
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
                                    text = if (strings.isRu) "Формула редукции к уровню моря (WGS-84)" else "Sea Level Reduction Formula (WGS-84)",
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
                                text = if (strings.isRu) "Где s — длина пути, h_avg — средняя высота (${result.avgElevationMeters.toInt()} м), R ≈ 6 371 008 м — средний радиус Земли WGS-84. На больших высотах длина маршрута по физической поверхности Земли всегда физически превышает ее идеальную геодезическую проекцию на эллипсоид уровня моря." else "Where s is path distance, h_avg is mean altitude (${result.avgElevationMeters.toInt()} m), R ≈ 6,371,008 m is WGS-84 mean Earth radius. At high altitudes, path length along physical surface always exceeds its ideal geodetic sea-level projection.",
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
    onTrackSelected: (com.flathike.app.model.GpsTrack) -> Unit,
    strings: AppStrings
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(strings.demoTracks, style = MaterialTheme.typography.titleLarge)
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
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}

@Composable
fun PasteTrackDialog(
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
    strings: AppStrings
) {
    var rawText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.pasteTrackTitle) },
        text = {
            Column {
                Text(
                    text = strings.pasteTrackDesc,
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
                Text(strings.pasteTrackLoad)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun ExplanationDialog(onDismiss: () -> Unit, strings: AppStrings) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.explanationDialogTitle) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = if (strings.isRu) "1. По прямой на уровне моря (WGS-84)" else "1. Straight Line at Sea Level (WGS-84)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (strings.isRu) {
                        "Ортодромия — кратчайшее расстояние между координатами старта и финиша вдоль поверхности Земли на нулевой высоте (на уровне моря). В отличие от 3D хорды, огибает земную кривизну по эллипсоиду WGS-84."
                    } else {
                        "Great-circle distance along Earth's ellipsoid at sea level (zero elevation). Unlike a 3D chord, it curves around the WGS-84 ellipsoid."
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (strings.isRu) "2. Коэффициенты при подъеме" else "2. Climb Effort Coefficients",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (strings.isRu) {
                        "При подъеме человек совершает работу против гравитации (A = m·g·h). КПД мышц на подъеме составляет ~20-25%.\n\n" +
                                "• Швейцарский SAC / Naismith (k = 10.0): 100 м набора = +1 км равнины.\n" +
                                "• NOLS Petzoldt (k = 8.0): 100 м набора = +0.8 км равнины.\n" +
                                "• ФСТР (k = 6.0): 100 м набора = +0.6 км равнины.\n" +
                                "• Трейлраннинг (k = 4.0): налегке с палками."
                    } else {
                        "When climbing, a hiker works against gravity (W = m·g·h). Muscle biomechanical efficiency is ~20-25%.\n\n" +
                                "• Swiss SAC / Naismith (k = 10.0): 100m climb = +1 km flat.\n" +
                                "• NOLS Petzoldt (k = 8.0): 100m climb = +0.8 km flat.\n" +
                                "• Mountaineering standard (k = 6.0): 100m climb = +0.6 km flat.\n" +
                                "• Trail running (k = 4.0): fastpacking with trekking poles."
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (strings.isRu) "3. Интеллект AI Gemma" else "3. AI Gemma Trail Intelligence",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (strings.isRu) {
                        "Приложение использует открытую нейросеть Gemma от Google для детального топографического, физиологического и геодезического анализа ваших походов."
                    } else {
                        "The application uses Google's open Gemma neural model for detailed topographic, physiological, and geodetic trail analysis."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.explanationGotIt) }
        }
    )
}

@Composable
fun EmptyStateCard(
    onSelectDemo: () -> Unit,
    onOpenFilePicker: () -> Unit,
    strings: AppStrings
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
                text = strings.emptyStateTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.emptyStateDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onSelectDemo) {
                    Text(strings.emptySelectDemo)
                }
                TextButton(onClick = onOpenFilePicker) {
                    Text(strings.emptyOpenFile)
                }
            }
        }
    }
}
