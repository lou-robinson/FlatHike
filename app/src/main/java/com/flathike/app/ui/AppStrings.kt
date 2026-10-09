package com.flathike.app.ui

import com.flathike.app.model.AppLanguage
import java.util.Locale

/**
 * Complete localization dictionary for Russian and English languages.
 */
class AppStrings(val language: AppLanguage) {

    val isRu: Boolean
        get() = when (language) {
            AppLanguage.RU -> true
            AppLanguage.EN -> false
            AppLanguage.SYSTEM -> Locale.getDefault().language.lowercase() == "ru"
        }

    // Header & Titles
    val appTitle: String get() = if (isRu) "FlatHike" else "FlatHike"
    val appSubtitle: String get() = if (isRu) "Горный эквивалент и геодезия" else "Mountain Trail & Geodesy"
    val settings: String get() = if (isRu) "Настройки" else "Settings"
    val save: String get() = if (isRu) "Сохранить" else "Save"
    val cancel: String get() = if (isRu) "Отмена" else "Cancel"
    val delete: String get() = if (isRu) "Удалить" else "Delete"
    val close: String get() = if (isRu) "Закрыть" else "Close"

    // Tabs
    val tabCoefficients: String get() = if (isRu) "Коэффициенты" else "Coefficients"
    val tabAppearance: String get() = if (isRu) "Внешний вид" else "Appearance"
    val tabAiGemma: String get() = if (isRu) "AI Gemma" else "AI Gemma"

    // Theme & Language
    val themeSectionTitle: String get() = if (isRu) "Тема оформления:" else "Theme Mode:"
    val themeSystem: String get() = if (isRu) "Системная" else "System Default"
    val themeLight: String get() = if (isRu) "Светлая тема" else "Light Theme"
    val themeDark: String get() = if (isRu) "Тёмная тема" else "Dark Theme"
    val languageSectionTitle: String get() = if (isRu) "Язык интерфейса:" else "UI Language:"
    val languageSystem: String get() = if (isRu) "По языку системы" else "System Default"
    val languageRu: String get() = "Русский (RU)"
    val languageEn: String get() = "English (EN)"

    // Demo Tracks
    val demoTracks: String get() = if (isRu) "Демо-треки" else "Demo Tracks"
    val openFile: String get() = if (isRu) "Открыть файл" else "Open File"
    val pasteText: String get() = if (isRu) "Вставить текст" else "Paste Text"

    // Waypoints
    val waypointsTitle: String get() = if (isRu) "Промежуточные точки" else "Intermediate Waypoints"
    val waypointsCount: (Int) -> String = { count ->
        if (isRu) "Промежуточные точки ($count)" else "Waypoints ($count)"
    }
    val noWaypoints: String get() = if (isRu) "На треке нет промежуточных отметок" else "No waypoints marked on track"
    val addWaypoint: String get() = if (isRu) "Добавить точку" else "Add Waypoint"
    val waypointName: String get() = if (isRu) "Название точки" else "Waypoint Name"
    val waypointDistanceKm: String get() = if (isRu) "Километр трека (км)" else "Track Distance (km)"
    val waypointElevationM: String get() = if (isRu) "Высота (м)" else "Elevation (m)"
    val waypointDesc: String get() = if (isRu) "Описание (опционально)" else "Description (optional)"

    // Elevation Profile
    val elevationProfile: String get() = if (isRu) "Высотный профиль" else "Elevation Profile"
    val noElevationData: String get() = if (isRu) "Нет данных высотного профиля" else "No elevation profile data"
    val fetchDemElevations: String get() = if (isRu) "Загрузить высоты с DEM" else "Fetch DEM Elevations"
    val elevationMissingWarning: String get() = if (isRu) "Внимание: в треке отсутствуют высоты (2D)" else "Warning: elevation data missing in track (2D)"

    // Splits and Speed Analysis
    val speedAnalysisTitle: String get() = if (isRu) "Анализ скорости и разбивка трека" else "Speed Analysis & Track Splits"
    val speedVsSlopeTitle: String get() = if (isRu) "Скорость по углам наклона" else "Speed vs. Slope Gradient"
    val speedVsSlopeDesc: String get() = if (isRu) "Зависимость темпа от рельефа:" else "Pace and speed vs. terrain slope:"
    val partitionMode: String get() = if (isRu) "Разбивка:" else "Splits:"
    val splitBySlope: String get() = if (isRu) "По уклону" else "By Slope"
    val splitBy1Km: String get() = if (isRu) "Каждый 1 км" else "1 km Splits"
    val splitBy5Km: String get() = if (isRu) "Каждые 5 км" else "5 km Splits"
    val splitByWaypoints: String get() = if (isRu) "По точкам" else "By Waypoints"

    val steepAscent: String get() = if (isRu) "Крутой подъём (>15%)" else "Steep Ascent (>15%)"
    val moderateAscent: String get() = if (isRu) "Умеренный подъём (5–15%)" else "Moderate Ascent (5–15%)"
    val flatTerrain: String get() = if (isRu) "Пологий / Равнина (±5%)" else "Flat / Gentle (±5%)"
    val moderateDescent: String get() = if (isRu) "Умеренный спуск (-15…-5%)" else "Moderate Descent (-15…-5%)"
    val steepDescent: String get() = if (isRu) "Крутой спуск (<-15%)" else "Steep Descent (<-15%)"

    val colDistance: String get() = if (isRu) "Дистанция" else "Distance"
    val colSlope: String get() = if (isRu) "Уклон" else "Slope"
    val colSpeed: String get() = if (isRu) "Скорость" else "Speed"
    val colTime: String get() = if (isRu) "Время" else "Duration"
    val colAscent: String get() = if (isRu) "Набор" else "Climb"
    val colDescent: String get() = if (isRu) "Спуск" else "Descent"
    val gpsSource: String get() = if (isRu) "GPS запись" else "GPS recorded"
    val toblerSource: String get() = if (isRu) "Расчётная" else "Estimated"

    // Primary metrics
    val flatEquivalent: String get() = if (isRu) "Эквивалент по равнине" else "Flat Equivalent Distance"
    val actualTrackLength: String get() = if (isRu) "Фактическая длина" else "Actual Track Length"
    val straightLineSeaLevel: String get() = if (isRu) "По прямой на ур. моря" else "Straight Line Sea Level"
    val totalAscent: String get() = if (isRu) "Набор высоты" else "Total Ascent"
    val totalDescent: String get() = if (isRu) "Сброс высоты" else "Total Descent"
    val maxElevation: String get() = if (isRu) "Макс. высота" else "Max Altitude"
    val minElevation: String get() = if (isRu) "Мин. высота" else "Min Altitude"
    val avgSlope: String get() = if (isRu) "Средний уклон" else "Avg Slope"
    val maxSlope: String get() = if (isRu) "Макс. уклон" else "Max Slope"
    val estTimeTobler: String get() = if (isRu) "Время (Тоблер)" else "Time (Tobler)"
    val estTimeNaismith: String get() = if (isRu) "Время (Нейсмит)" else "Time (Naismith)"
    val estTimeCustom: String get() = if (isRu) "Время (с учётом k)" else "Time (with k)"

    // Recorded time metrics
    val recordedMetricsTitle: String get() = if (isRu) "ФАКТИЧЕСКИЕ ДАННЫЕ С ДАТЧИКОВ" else "RECORDED SENSOR METRICS"
    val recordedTotalTime: String get() = if (isRu) "Общее время" else "Total Elapsed Time"
    val recordedMovingTime: String get() = if (isRu) "Время в движении" else "Moving Time"
    val recordedStoppedTime: String get() = if (isRu) "Остановки" else "Stopped Time"
    val recordedAvgSpeed: String get() = if (isRu) "Средняя скорость" else "Avg Speed"
    val recordedMovingSpeed: String get() = if (isRu) "Скорость в движении" else "Moving Speed"
    val recordedMaxSpeed: String get() = if (isRu) "Макс. скорость" else "Max Speed"
    val recordedPace: String get() = if (isRu) "Темп" else "Pace"
    val recordedAvgTemp: String get() = if (isRu) "Температура" else "Temperature"

    // Geodetic details
    val geodeticsTitle: String get() = if (isRu) "ГЕОДЕЗИЧЕСКИЕ ТОНКОСТИ" else "GEODETIC PRECISION"
    val straightLine3dChord: String get() = if (isRu) "3D хорда сквозь Землю" else "3D Chord Through Earth"
    val true3dDistance: String get() = if (isRu) "Истинный 3D путь (с рельефом)" else "True 3D Terrain Distance"
    val terrainDiff: String get() = if (isRu) "Удлинение за счёт рельефа" else "Terrain Extension"
    val geodeticReduction: String get() = if (isRu) "Редукция на уровень моря" else "Sea Level Reduction"

    // AI Card
    val gemmaTitle: String get() = if (isRu) "Gemma Trail Intelligence" else "Gemma Trail Intelligence"
    val gemmaPromptPlaceholder: String get() = if (isRu) "Спросите о сложности, воде, снаряжении или погоде..." else "Ask about difficulty, water, gear, or weather..."
    val gemmaGenerateReport: String get() = if (isRu) "Сформировать отчет" else "Generate Report"
    val gemmaAskButton: String get() = if (isRu) "Спросить" else "Ask"
}
