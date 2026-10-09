package com.flathike.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flathike.app.ai.GemmaConfig
import com.flathike.app.ai.GemmaService
import com.flathike.app.calculator.TrackAnalyzer
import com.flathike.app.data.DemoTracks
import com.flathike.app.model.HikingPreset
import com.flathike.app.model.TrackCalculationResult
import com.flathike.app.model.GpsTrack
import com.flathike.app.parser.TrackFormatDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream

import com.flathike.app.calculator.ElevationLookupService
import com.flathike.app.model.AppLanguage
import com.flathike.app.model.EffectiveDistanceResult
import com.flathike.app.model.ThemeMode
import com.flathike.app.model.TrackSplitMode
import com.flathike.app.model.TrackWaypoint
import kotlin.math.abs

data class FlatHikeUiState(
    val currentTrack: GpsTrack? = null,
    val calculationResult: TrackCalculationResult? = null,
    val selectedPreset: HikingPreset = HikingPreset.SWISS_SAC,
    val customCoefficient: Double = 10.0,
    val applyToTrackLength: Boolean = true,
    val walkingSpeedKmH: Double = 4.0,
    val showAdvancedGeodetic: Boolean = false,
    val gemmaConfig: GemmaConfig = GemmaConfig(),
    val gemmaReport: String? = null,
    val gemmaAnswer: String? = null,
    val isGemmaLoading: Boolean = false,
    val gemmaError: String? = null,
    val trackError: String? = null,
    val isSettingsOpen: Boolean = false,
    val showRecordedTimeSpeed: Boolean = true,
    val showRecordedTemperature: Boolean = true,
    val isFetchingElevation: Boolean = false,
    val elevationFetchProgress: Float = 0f,
    val elevationFetchError: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val trackSplitMode: TrackSplitMode = TrackSplitMode.BY_SLOPE,
    val selectedWaypoint: TrackWaypoint? = null,
    val isAddWaypointDialogOpen: Boolean = false,
    val initialWaypointDistance: Double? = null
) {
    val effectiveCoefficient: Double
        get() = if (selectedPreset == HikingPreset.CUSTOM) customCoefficient else selectedPreset.ascentCoefficient

    val effectiveDistanceResult: EffectiveDistanceResult?
        get() = calculationResult?.calculateEffectiveDistance(effectiveCoefficient, applyToTrackLength)

    val estimatedHikingMinutes: Double
        get() {
            val effDist = effectiveDistanceResult?.effectiveDistanceKm ?: return 0.0
            val speed = if (walkingSpeedKmH > 0.1) walkingSpeedKmH else 4.0
            return (effDist / speed) * 60.0
        }
}

class FlatHikeViewModel(application: Application) : AndroidViewModel(application) {

    private val gemmaService = GemmaService(application.applicationContext)
    private val elevationLookupService = ElevationLookupService()

    private val _uiState = MutableStateFlow(FlatHikeUiState())
    val uiState: StateFlow<FlatHikeUiState> = _uiState.asStateFlow()

    init {
        // Load initial demo track so the user immediately sees calculation & Gemma AI
        loadTrack(DemoTracks.allTracks.first())
    }

    fun loadTrack(track: GpsTrack) {
        val result = TrackAnalyzer.analyze(
            points = track.points,
            trackName = track.name,
            rawWaypoints = track.waypoints,
            walkingSpeedDefaultKmH = _uiState.value.walkingSpeedKmH
        )
        _uiState.update { current ->
            current.copy(
                currentTrack = track,
                calculationResult = result,
                selectedWaypoint = null,
                gemmaReport = null,
                gemmaAnswer = null,
                gemmaError = null,
                trackError = null,
                elevationFetchError = null,
                isFetchingElevation = false,
                elevationFetchProgress = 0f
            )
        }
    }

    private val isRu: Boolean
        get() = AppStrings(_uiState.value.appLanguage).isRu

    fun loadTrackFromStream(inputStream: InputStream, filename: String) {
        try {
            val track = TrackFormatDetector.parseStream(
                inputStream = inputStream,
                filename = filename,
                defaultName = filename.substringBeforeLast('.')
            )
            if (track.points.isEmpty()) {
                _uiState.update { it.copy(trackError = if (isRu) "Файл не содержит корректных GPS координат" else "File does not contain valid GPS coordinates") }
            } else {
                loadTrack(track)
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(trackError = if (isRu) "Ошибка чтения трека: ${e.localizedMessage}" else "Failed to read track: ${e.localizedMessage}") }
        }
    }

    fun loadTrackFromText(text: String) {
        try {
            val track = TrackFormatDetector.parseText(text)
            if (track.points.isEmpty()) {
                _uiState.update { it.copy(trackError = if (isRu) "Не удалось распознать координаты в тексте" else "Could not recognize coordinates in text") }
            } else {
                loadTrack(track)
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(trackError = if (isRu) "Ошибка парсинга: ${e.localizedMessage}" else "Parsing error: ${e.localizedMessage}") }
        }
    }

    fun setTrackError(error: String) {
        _uiState.update { it.copy(trackError = error) }
    }

    fun selectPreset(preset: HikingPreset) {
        _uiState.update { it.copy(selectedPreset = preset) }
    }

    fun setCustomCoefficient(coefficient: Double) {
        _uiState.update {
            it.copy(
                customCoefficient = coefficient,
                selectedPreset = HikingPreset.CUSTOM
            )
        }
    }

    val effectiveCoefficient: Double
        get() {
            val s = _uiState.value
            return if (s.selectedPreset == HikingPreset.CUSTOM) {
                s.customCoefficient
            } else {
                s.selectedPreset.ascentCoefficient
            }
        }

    fun requestGemmaReport() {
        val result = _uiState.value.calculationResult ?: return
        val preset = _uiState.value.selectedPreset
        val coeff = effectiveCoefficient

        viewModelScope.launch {
            _uiState.update { it.copy(isGemmaLoading = true, gemmaError = null) }
            try {
                val report = gemmaService.generateTrailReport(result, preset, coeff)
                _uiState.update {
                    it.copy(
                        gemmaReport = report,
                        gemmaAnswer = null,
                        isGemmaLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        gemmaError = if (isRu) "Ошибка генерации Gemma: ${e.localizedMessage}" else "Gemma generation error: ${e.localizedMessage}",
                        isGemmaLoading = false
                    )
                }
            }
        }
    }

    fun askGemmaQuestion(question: String) {
        val result = _uiState.value.calculationResult ?: return
        val preset = _uiState.value.selectedPreset
        val coeff = effectiveCoefficient

        viewModelScope.launch {
            _uiState.update { it.copy(isGemmaLoading = true, gemmaError = null) }
            try {
                val answer = gemmaService.answerQuestion(question, result, preset, coeff)
                _uiState.update {
                    it.copy(
                        gemmaAnswer = answer,
                        isGemmaLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        gemmaError = if (isRu) "Ошибка Gemma: ${e.localizedMessage}" else "Gemma error: ${e.localizedMessage}",
                        isGemmaLoading = false
                    )
                }
            }
        }
    }

    fun setWalkingSpeed(speed: Double) {
        _uiState.update { it.copy(walkingSpeedKmH = speed.coerceIn(1.0, 15.0)) }
    }

    fun setApplyToTrackLength(apply: Boolean) {
        _uiState.update { it.copy(applyToTrackLength = apply) }
    }

    fun toggleAdvancedGeodetic() {
        _uiState.update { it.copy(showAdvancedGeodetic = !it.showAdvancedGeodetic) }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun saveSettings(
        preset: HikingPreset,
        customCoeff: Double,
        applyToTrackLength: Boolean,
        walkingSpeed: Double,
        showRecordedTimeSpeed: Boolean,
        showRecordedTemperature: Boolean,
        gemmaConfig: GemmaConfig,
        themeMode: ThemeMode = _uiState.value.themeMode,
        appLanguage: AppLanguage = _uiState.value.appLanguage
    ) {
        viewModelScope.launch {
            gemmaService.updateConfig(gemmaConfig)
            _uiState.update {
                it.copy(
                    selectedPreset = preset,
                    customCoefficient = customCoeff,
                    applyToTrackLength = applyToTrackLength,
                    walkingSpeedKmH = walkingSpeed,
                    showRecordedTimeSpeed = showRecordedTimeSpeed,
                    showRecordedTemperature = showRecordedTemperature,
                    gemmaConfig = gemmaConfig,
                    themeMode = themeMode,
                    appLanguage = appLanguage,
                    isSettingsOpen = false
                )
            }
        }
    }

    fun openAddWaypointDialog(initialDistanceKm: Double? = null) {
        _uiState.update { it.copy(isAddWaypointDialogOpen = true, initialWaypointDistance = initialDistanceKm) }
    }

    fun closeAddWaypointDialog() {
        _uiState.update { it.copy(isAddWaypointDialogOpen = false, initialWaypointDistance = null) }
    }

    fun addWaypoint(name: String, distanceKm: Double, description: String? = null) {
        val current = _uiState.value.currentTrack ?: return
        val targetPt = TrackAnalyzer.findPointAtDistance(current.points, distanceKm)

        if (targetPt != null) {
            val newWpt = TrackWaypoint(
                name = name,
                latitude = targetPt.latitude,
                longitude = targetPt.longitude,
                elevation = targetPt.elevation,
                description = description,
                distanceKm = distanceKm
            )
            val updatedTrack = current.copy(waypoints = current.waypoints + newWpt)
            loadTrack(updatedTrack)
        }
        closeAddWaypointDialog()
    }

    fun deleteWaypoint(waypoint: TrackWaypoint) {
        val current = _uiState.value.currentTrack ?: return
        val updatedTrack = current.copy(waypoints = current.waypoints.filter { it != waypoint })
        loadTrack(updatedTrack)
        if (_uiState.value.selectedWaypoint == waypoint) {
            selectWaypoint(null)
        }
    }

    fun selectWaypoint(waypoint: TrackWaypoint?) {
        _uiState.update { it.copy(selectedWaypoint = waypoint) }
    }

    fun setSplitMode(mode: TrackSplitMode) {
        _uiState.update { it.copy(trackSplitMode = mode) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setAppLanguage(lang: AppLanguage) {
        _uiState.update { it.copy(appLanguage = lang) }
    }

    fun updateGemmaConfig(newConfig: GemmaConfig) {
        viewModelScope.launch {
            gemmaService.updateConfig(newConfig)
            _uiState.update {
                it.copy(
                    gemmaConfig = newConfig,
                    isSettingsOpen = false
                )
            }
        }
    }

    /**
     * Downloads real elevation data from Digital Elevation Models (Open-Meteo / Open-Elevation)
     * and updates the track with enriched 3D coordinates.
     */
    fun fetchElevationProfile() {
        val track = _uiState.value.currentTrack ?: return
        if (track.points.isEmpty() || _uiState.value.isFetchingElevation) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFetchingElevation = true,
                    elevationFetchProgress = 0f,
                    elevationFetchError = null
                )
            }
            try {
                val enrichedPoints = elevationLookupService.fetchElevations(track.points) { progress ->
                    _uiState.update { it.copy(elevationFetchProgress = progress) }
                }
                val enrichedTrack = track.copy(points = enrichedPoints)
                val newResult = TrackAnalyzer.analyze(enrichedPoints, enrichedTrack.name)
                _uiState.update {
                    it.copy(
                        currentTrack = enrichedTrack,
                        calculationResult = newResult,
                        isFetchingElevation = false,
                        elevationFetchProgress = 1f,
                        elevationFetchError = null,
                        gemmaReport = null,
                        gemmaAnswer = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFetchingElevation = false,
                        elevationFetchError = e.localizedMessage ?: if (isRu) "Сетевая ошибка при загрузке DEM" else "Network error while fetching DEM"
                    )
                }
            }
        }
    }

    fun dismissElevationError() {
        _uiState.update { it.copy(elevationFetchError = null) }
    }
}
