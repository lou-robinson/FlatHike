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
    val appTitle: String get() = "FlatHike"
    val appSubtitle: String get() = if (isRu) "Горный эквивалент и геодезия" else "Mountain Trail & Geodesy"
    val settings: String get() = if (isRu) "Настройки" else "Settings"
    val save: String get() = if (isRu) "Сохранить" else "Save"
    val cancel: String get() = if (isRu) "Отмена" else "Cancel"
    val delete: String get() = if (isRu) "Удалить" else "Delete"
    val close: String get() = if (isRu) "Закрыть" else "Close"
    val menu: String get() = if (isRu) "Меню" else "Menu"
    val loadTrack: String get() = if (isRu) "Загрузить трек" else "Load track"
    val helpClimbCoeffs: String get() = if (isRu) "Справка: коэффициенты подъема" else "Help: climb coefficients"

    // Units
    val km: String get() = if (isRu) "км" else "km"
    val kmh: String get() = if (isRu) "км/ч" else "km/h"
    val meters: String get() = if (isRu) "м" else "m"
    val min: String get() = if (isRu) "мин" else "min"
    val sec: String get() = if (isRu) "с" else "s"
    val hours: String get() = if (isRu) "ч" else "h"
    val perKm: String get() = if (isRu) "/км" else "/km"
    val elevation: String get() = if (isRu) "Высота" else "Elevation"
    val slope: String get() = if (isRu) "уклон" else "slope"
    val segmentPrefix: String get() = if (isRu) "Участок" else "Segment"
    val start: String get() = if (isRu) "Старт" else "Start"
    val finish: String get() = if (isRu) "Финиш" else "Finish"
    val tobler: String get() = if (isRu) "Тоблер" else "Tobler"
    val gps: String get() = "GPS"

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
    val addWaypointHere: String get() = if (isRu) "Добавить точку здесь" else "Add waypoint here"
    val waypointName: String get() = if (isRu) "Название точки" else "Waypoint Name"
    val waypointDistanceKm: String get() = if (isRu) "Километр трека (км)" else "Track Distance (km)"
    val waypointElevationM: String get() = if (isRu) "Высота (м)" else "Elevation (m)"
    val waypointDesc: String get() = if (isRu) "Описание (опционально)" else "Description (optional)"
    val waypointDefault: (Int) -> String = { idx -> if (isRu) "Точка $idx" else "Waypoint $idx" }
    val waypointPlaceholder: String get() = if (isRu) "Привал / Родник / Перевал" else "Camp / Spring / Pass"

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
    val notEnoughData: String get() = if (isRu) "Недостаточно данных для разбивки" else "Not enough data for segmentation"

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

    // Overview Card Strings
    val loopRoute: String get() = if (isRu) "Кольцевой маршрут (старт ≈ финиш)" else "Closed loop route (start ≈ finish)"
    val pointsCount: (Int) -> String = { count -> if (isRu) "Точек: $count" else "Points: $count" }
    val elevationsRange: (Int, Int, Int) -> String = { start, end, peak ->
        if (isRu) "Высоты: $start → $end м (пик $peak м)" else "Elevation: $start → $end m (peak $peak m)"
    }

    // Hero Card Strings
    val flatEquivalentHeader: String get() = if (isRu) "ПО РОВНОЙ ПОВЕРХНОСТИ" else "ON FLAT SURFACE"
    val distanceHeader: String get() = if (isRu) "ДИСТАНЦИЯ" else "DISTANCE"
    val hikingTimeHeader: String get() = if (isRu) "ВРЕМЯ ХОДА" else "HIKING TIME"
    val atSpeed: (Double) -> String = { spd ->
        if (isRu) "при ${String.format(Locale.US, "%.1f", spd)} км/ч" else "at ${String.format(Locale.US, "%.1f", spd)} km/h"
    }
    val avgWalkingSpeed: String get() = if (isRu) "Средняя скорость ходьбы:" else "Average walking speed:"
    val basePlusBonus: (Double, Double) -> String = { base, bonus ->
        if (isRu) "База ${String.format(Locale.US, "%.1f", base)} + ${String.format(Locale.US, "%.1f", bonus)} км"
        else "Base ${String.format(Locale.US, "%.1f", base)} + ${String.format(Locale.US, "%.1f", bonus)} km"
    }
    val straightPlusBonus: (Double, Double) -> String = { base, bonus ->
        if (isRu) "Прямая ${String.format(Locale.US, "%.1f", base)} + ${String.format(Locale.US, "%.1f", bonus)} км"
        else "Direct ${String.format(Locale.US, "%.1f", base)} + ${String.format(Locale.US, "%.1f", bonus)} km"
    }
    val coeffSettings: String get() = if (isRu) "Настройки коэффициента" else "Coefficient settings"

    // Recorded time metrics
    val recordedMetricsTitle: String get() = if (isRu) "ФАКТИЧЕСКИЕ ДАННЫЕ ЗАПИСИ" else "RECORDED TRACK DATA"
    val recordedDataSubtitleBoth: String get() = if (isRu) "Хронометраж, реальная скорость и температура воздуха" else "Timing, actual speed and air temperature"
    val recordedDataSubtitleTime: String get() = if (isRu) "Хронометраж и реальная скорость по меткам трека" else "Timing and actual speed from track timestamps"
    val recordedDataSubtitleTemp: String get() = if (isRu) "Температурный профиль по датчикам трека" else "Temperature profile from track sensors"
    val movingSpeed: String get() = if (isRu) "Скорость в движении" else "Moving speed"
    val movingTime: String get() = if (isRu) "Время в движении" else "Moving time"
    val movingPace: String get() = if (isRu) "Темп движения" else "Moving pace"
    val recordingTime: String get() = if (isRu) "Время записи" else "Recording time"
    val stoppedDuration: (String) -> String = { dur -> if (isRu) "остановки: $dur" else "stops: $dur" }
    val totalDurationLabel: (String) -> String = { dur -> if (isRu) "всего: $dur" else "total: $dur" }
    val maxSpeedLabel: (Double) -> String = { spd ->
        if (isRu) "макс. ${String.format(Locale.US, "%.1f", spd)} км/ч" else "max ${String.format(Locale.US, "%.1f", spd)} km/h"
    }
    val overallAvgSpeedLabel: (Double) -> String = { spd ->
        if (isRu) "общая ср.: ${String.format(Locale.US, "%.1f", spd)} км/ч" else "overall avg: ${String.format(Locale.US, "%.1f", spd)} km/h"
    }
    val airTempLabel: String get() = if (isRu) "Температура воздуха" else "Air temperature"
    val tempRangeLabel: (Double, Double, Double, Double) -> String = { min, max, start, end ->
        if (isRu) "Диапазон: ${String.format(Locale.US, "%+.1f°C", min)} … ${String.format(Locale.US, "%+.1f°C", max)}   •   Старт / финиш: ${String.format(Locale.US, "%+.0f°", start)} / ${String.format(Locale.US, "%+.0f°C", end)}"
        else "Range: ${String.format(Locale.US, "%+.1f°C", min)} … ${String.format(Locale.US, "%+.1f°C", max)}   •   Start / finish: ${String.format(Locale.US, "%+.0f°", start)} / ${String.format(Locale.US, "%+.0f°C", end)}"
    }

    // Key metrics card
    val keyTrackParams: String get() = if (isRu) "КЛЮЧЕВЫЕ ПАРАМЕТРЫ ТРЕКА" else "KEY TRACK PARAMETERS"
    val straightLineDistance: String get() = if (isRu) "По прямой" else "Straight line"
    val maxRadialDist: String get() = if (isRu) "макс. удаление" else "max radial distance"
    val startToFinish: String get() = if (isRu) "старт → финиш" else "start → finish"
    val pathAlongTrack: String get() = if (isRu) "Путь по треку" else "Path along track"
    val gpsPointsCount: (Int) -> String = { count -> if (isRu) "$count GPS-точек" else "$count GPS points" }
    val ascentDescentLabel: String get() = if (isRu) "Набор / Сброс" else "Ascent / Descent"
    val elevationDiffLabel: (Int) -> String = { diff -> if (isRu) "перепад: $diff м" else "diff: $diff m" }
    val trackElevationsLabel: String get() = if (isRu) "Высоты трека" else "Track altitudes"
    val showGeodetics: String get() = if (isRu) "▼ Подробная геодезия (WGS-84, 3D хорда, уклоны)" else "▼ Detailed geodesy (WGS-84, 3D chord, slopes)"
    val hideGeodetics: String get() = if (isRu) "▲ Скрыть геодезические детали" else "▲ Hide geodetic details"

    // Geodetic details
    val geodeticsHeader: String get() = if (isRu) "Геодезические параметры WGS-84" else "WGS-84 Geodetic Parameters"
    val geodeticsTitle: String get() = geodeticsHeader
    val geodeticsSubtitle: String get() = if (isRu) "Редукция эллипсоида, пространственные хорды и модели ходьбы" else "Ellipsoid reduction, 3D chords and hiking models"
    val groupEarthCurvature: String get() = if (isRu) "КРИВИЗНА ЗЕМЛИ И ЭЛЛИПСОИД WGS-84" else "EARTH CURVATURE & WGS-84 ELLIPSOID"
    val reductionTitle: String get() = if (isRu) "Редукция высоты на эллипсоид" else "Elevation reduction to ellipsoid"
    val reductionDesc: (Int) -> String = { avgEle ->
        if (isRu) "Поправка за счет средней высоты маршрута $avgEle м над геоидом. Из-за радиуса Земли (6371 км) реальный путь на высоте физически длиннее своей проекции на уровень моря."
        else "Correction due to average route altitude of $avgEle m above geoid. Due to Earth's radius (6371 km), real physical distance at altitude is longer than sea-level projection."
    }
    val chordClosedTitle: String get() = if (isRu) "Хорда замыкания кольца (Старт ↔ Финиш)" else "Loop closure chord (Start ↔ Finish)"
    val chord3dTitle: String get() = if (isRu) "3D Хорда сквозь геоид (Старт → Финиш)" else "3D Chord through geoid (Start → Finish)"
    val loopMisclosureBadge: String get() = if (isRu) "Невязка кольца" else "Loop misclosure"
    val chordEcefBadge: String get() = if (isRu) "Хорда ECEF" else "ECEF chord"
    val chordClosedDesc: String get() = if (isRu) "Геометрическое расстояние между точками старта и завершения трека (точность сведения кольцевого маршрута)." else "Geometric distance between route start and finish points (loop closing precision)."
    val chord3dDesc: String get() = if (isRu) "Кратчайший евклидов отрезок в трехмерном декартовом пространстве ECEF сквозь тело Земли в обход кривизны эллипсоида." else "Shortest Euclidean segment in 3D ECEF Cartesian space through Earth bypassing ellipsoid curvature."
    val groupGeometryTerrain: String get() = if (isRu) "ГЕОМЕТРИЯ ПУТИ И МИКРОРЕЛЬЕФ" else "PATH GEOMETRY & TERRAIN"
    val tortuosityTitle: String get() = if (isRu) "Коэффициент извилистости (Track / Direct)" else "Tortuosity ratio (Track / Direct)"
    val tortuosityDescClosed: (Double, Double) -> String = { trackKm, straightKm ->
        if (isRu) "Отношение длины пути (${String.format(Locale.US, "%.2f", trackKm)} км) к максимальному радиальному удалению (${String.format(Locale.US, "%.2f", straightKm)} км). Показывает, насколько сильно маршрут петляет."
        else "Ratio of track distance (${String.format(Locale.US, "%.2f", trackKm)} km) to maximum radial distance (${String.format(Locale.US, "%.2f", straightKm)} km). Measures track winding."
    }
    val tortuosityDescDirect: (Double, Double) -> String = { trackKm, straightKm ->
        if (isRu) "Отношение длины пути (${String.format(Locale.US, "%.2f", trackKm)} км) к прямому расстоянию старт-финиш (${String.format(Locale.US, "%.2f", straightKm)} км)."
        else "Ratio of track distance (${String.format(Locale.US, "%.2f", trackKm)} km) to direct start-finish distance (${String.format(Locale.US, "%.2f", straightKm)} km)."
    }
    val true3dTitle: String get() = if (isRu) "Истинная 3D-длина по рельефу" else "True 3D terrain distance"
    val true3dDesc: String get() = if (isRu) "Реальное физическое трехмерное расстояние с учетом всех подъемов, спусков и кривизны склонов по отношению к горизонтальной проекции." else "Actual physical 3D distance accounting for all climbs, descents, and slope terrain relative to horizontal projection."
    val maxSlopeTitle: String get() = if (isRu) "Максимальный уклон склона" else "Maximum terrain slope"
    val maxSlopeDesc: (Double) -> String = { avg ->
        if (isRu) "Крутизна самого крутого сегмента пути с фильтрацией шумов GPS-высотомера (средний уклон по всему маршруту: ${String.format(Locale.US, "%.1f", avg)}%)."
        else "Steepness of steepest path segment with noise filtering (average trail slope: ${String.format(Locale.US, "%.1f", avg)}%)."
    }
    val groupHikingModels: String get() = if (isRu) "ФИЗИЧЕСКИЕ МОДЕЛИ ВРЕМЕНИ В ПУТИ (БЕЗ ПРИВАЛОВ)" else "PHYSICAL HIKING TIME MODELS (NO BREAKS)"
    val toblerTitle: String get() = if (isRu) "Функция Тоблера" else "Tobler Function"
    val toblerDesc: String get() = if (isRu) "Учитывает уклон каждого отрезка трека (замедляет на крутых склонах, ускоряет на спуске -5%)." else "Accounts for slope on each track section (slows on steep grades, speeds up at -5% descent)."
    val naismithTitle: String get() = if (isRu) "Правило Найсмита" else "Naismith's Rule"
    val naismithDesc: String get() = if (isRu) "Классический швейцарский альпинистский норматив SAC (1892 г.) для непрерывного движения." else "Classic Swiss Alpine Club (SAC / 1892) pacing rule for continuous movement."

    // Empty state & Dialogs
    val emptyStateTitle: String get() = if (isRu) "Загрузите GPS трек" else "Load GPS Track"
    val emptyStateDesc: String get() = if (isRu) "Поддерживаются файлы GPX, KML, GeoJSON, CSV, а также готовые демонстрационные маршруты." else "Supports GPX, KML, GeoJSON, CSV files, and built-in demo routes."
    val emptySelectDemo: String get() = if (isRu) "Выбрать демо-трек" else "Select demo track"
    val emptyOpenFile: String get() = if (isRu) "Открыть файл" else "Open file"
    val pasteTrackTitle: String get() = if (isRu) "Вставить координаты или GPX XML" else "Paste coordinates or GPX XML"
    val pasteTrackDesc: String get() = if (isRu) "Поддерживается формат GPX, KML, GeoJSON или строки 'широта, долгота, высота':" else "Supports GPX, KML, GeoJSON, or lines of 'latitude, longitude, elevation':"
    val pasteTrackLoad: String get() = if (isRu) "Загрузить" else "Load"
    val explanationDialogTitle: String get() = if (isRu) "Справка о расчетах FlatHike" else "FlatHike Calculations Guide"
    val explanationGotIt: String get() = if (isRu) "Понятно" else "Got it"
    val fileOpenError: (String) -> String = { err -> if (isRu) "Не удалось открыть файл: $err" else "Failed to open file: $err" }
    val fileReadError: String get() = if (isRu) "ошибка чтения" else "read error"

    // Elevation Missing Card
    val elevationMissingCardTitle: String get() = if (isRu) "Профиль высоты отсутствует (2D трек)" else "Elevation profile missing (2D track)"
    val elevationMissingCardDesc: String get() = if (isRu) "Все точки имеют высоту 0 м. Набор высоты, 3D дистанция и расчет времени не могут быть вычислены без данных рельефа." else "All points have 0m elevation. Ascent, 3D distance and time calculation require terrain data."
    val demUpdatingTitle: String get() = if (isRu) "Загрузка высот DEM" else "Fetching DEM elevations"
    val demUpdatingDesc: String get() = if (isRu) "Обновление профиля высот из открытой цифровой модели рельефа DEM (SRTM/Copernicus)." else "Fetching elevation profile from open digital elevation models (SRTM/Copernicus)."
    val demRequestingProgress: (Int) -> String = { pct ->
        if (isRu) "Запрос открытой модели рельефа DEM (SRTM/Copernicus)... $pct%" else "Requesting open DEM model (SRTM/Copernicus)... $pct%"
    }
    val demRetryButton: String get() = if (isRu) "Повторить загрузку высот (DEM)" else "Retry fetching elevations (DEM)"
    val demFetchButton: String get() = if (isRu) "Загрузить высоты с DEM (SRTM/Copernicus)" else "Fetch elevations from DEM (SRTM/Copernicus)"

    // AI Card
    val gemmaTitle: String get() = "Gemma Trail Intelligence"
    val gemmaPromptPlaceholder: String get() = if (isRu) "Спросите о сложности, воде, снаряжении или погоде..." else "Ask about difficulty, water, gear, or weather..."
    val gemmaGenerateReport: String get() = if (isRu) "Сформировать отчет" else "Generate Report"
    val gemmaAskButton: String get() = if (isRu) "Спросить" else "Ask"

    // Settings Dialog: Coefficients & Sensors
    val climbCoefficientStandard: String get() = if (isRu) "Стандарт коэффициента подъема:" else "Climb effort standard:"
    val climbCoefficientExplanation: String get() = if (isRu) "Определяет, сколько метров набора высоты эквивалентно 1 км ходьбы по равнине:" else "Defines how many meters of ascent equal 1 km of flat walking:"
    val customKLabel: (Double) -> String = { k -> if (isRu) "Пользовательский k: ${String.format(Locale.US, "%.1f", k)}" else "Custom k: ${String.format(Locale.US, "%.1f", k)}" }
    val customKPer100m: (Double) -> String = { bonus -> if (isRu) "+${String.format(Locale.US, "%.1f", bonus)} км за 100м набора" else "+${String.format(Locale.US, "%.1f", bonus)} km per 100m climb" }
    val baseDistanceForCalculation: String get() = if (isRu) "Базовая дистанция для расчета:" else "Base distance for calculation:"
    val applyToActualTrackLength: String get() = if (isRu) "К фактической длине трека (Рекомендуется)" else "To actual track length (Recommended)"
    val applyToActualTrackLengthDesc: String get() = if (isRu) "Длина тропы по GPS + надбавка за подъем" else "GPS trail distance + climb effort bonus"
    val applyToStraightLine: String get() = if (isRu) "К расстоянию по прямой на уровне моря" else "To straight-line distance at sea level"
    val applyToStraightLineDesc: String get() = if (isRu) "Прямая линия между точками + надбавка за подъем" else "Straight line between points + climb effort bonus"
    val defaultWalkingSpeed: (Double) -> String = { spd -> if (isRu) "Скорость ходьбы по умолчанию: ${String.format(Locale.US, "%.1f", spd)} км/ч" else "Default walking speed: ${String.format(Locale.US, "%.1f", spd)} km/h" }
    val defaultWalkingSpeedDesc: String get() = if (isRu) "Используется для начального расчета времени пути" else "Used for initial hiking time estimation"
    val trackDataAndSensors: String get() = if (isRu) "ДАННЫЕ ТРЕКА И ДАТЧИКИ" else "TRACK DATA & SENSORS"
    val trackDataAndSensorsDesc: String get() = if (isRu) "Отображение фактических параметров при их наличии в файле трека:" else "Show actual parameters if present in track file:"
    val actualTimeAndSpeed: String get() = if (isRu) "Фактическое время и реальная скорость" else "Actual time and speed"
    val actualTimeAndSpeedDesc: String get() = if (isRu) "Показывать время движения, среднюю и макс. скорость и темп по GPS-меткам" else "Show moving time, avg and max speed, and pace from GPS points"
    val airTemperature: String get() = if (isRu) "Температура воздуха" else "Air temperature"
    val airTemperatureDesc: String get() = if (isRu) "Показывать среднюю и мин/макс температуру из точек трека (Garmin / atemp)" else "Show average and min/max temperature from track points (Garmin / atemp)"

    // Settings Dialog: AI Gemma
    val gemmaModeSelection: String get() = if (isRu) "Выберите режим работы нейросети Gemma:" else "Select Gemma neural model mode:"
    val gemmaOfflineEngineReady: String get() = if (isRu) "Встроенный офлайн-эксперт готов к работе сразу. Рассчитывает биомеханику, коэффициенты и рельеф автономно без интернета." else "Built-in offline expert is ready immediately. Computes biomechanics, coefficients, and terrain offline."
    val gemmaSpecifyModelPath: String get() = if (isRu) "Укажите абсолютный путь к файлу модели Gemma (.bin или .task) на вашем устройстве:" else "Specify absolute path to Gemma model file (.bin or .task) on device:"
    val gemmaModelPathLabel: String get() = if (isRu) "Путь к модели (.bin / .task)" else "Model path (.bin / .task)"
    val gemmaProviderTemplates: String get() = if (isRu) "Быстрые шаблоны провайдеров Gemma:" else "Gemma provider quick templates:"
    val gemmaOfficialBadge: String get() = if (isRu) "Офиц." else "Official"
    val gemmaEndpointUrl: String get() = if (isRu) "URL эндпоинта" else "Endpoint URL"
    val gemmaApiKey: String get() = if (isRu) "API Ключ (Bearer token)" else "API Key (Bearer token)"
    val gemmaModelName: String get() = if (isRu) "Имя модели" else "Model name"
    val gemmaSettingsTitle: String get() = if (isRu) "Настройки AI Gemma" else "AI Gemma Settings"
}

