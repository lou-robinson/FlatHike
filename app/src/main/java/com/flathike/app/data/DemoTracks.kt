package com.flathike.app.data

import com.flathike.app.model.GpsPoint
import com.flathike.app.model.GpsTrack
import com.flathike.app.model.TrackWaypoint

/**
 * Pre-configured realistic demonstration tracks for instant testing.
 */
object DemoTracks {

    val allTracks: List<GpsTrack> by lazy {
        listOf(
            createElbrusTrack(),
            create2dMountainTrack(),
            createCapeAyaLoopTrack(),
            createRosaKhutorTrack(),
            createBotkinTrailTrack(),
            createMeshcheraFlatTrack()
        )
    }

    /**
     * Classic Elbrus Summit route (Priut 11 -> West Summit 5642m).
     * High altitude, steep climb (+1512m), significant geodetic difference.
     */
    private fun createElbrusTrack(): GpsTrack {
        val points = mutableListOf<GpsPoint>()
        val startLat = 43.3150
        val startLon = 42.4570
        val endLat = 43.3525
        val endLon = 42.4370

        val n = 45
        for (i in 0..n) {
            val f = i.toDouble() / n
            // Serpentine curve on Pastukhov rocks and saddle
            val lat = startLat + (endLat - startLat) * f
            val lon = startLon + (endLon - startLon) * f + (Math.sin(f * Math.PI * 3.0) * 0.0035)
            // Progressive elevation profile from 4130m to 5642m
            val ele = 4130.0 + (5642.0 - 4130.0) * (Math.pow(f, 0.95))
            points.add(GpsPoint(latitude = lat, longitude = lon, elevation = ele))
        }

        val waypoints = listOf(
            TrackWaypoint("Приют 11", startLat, startLon, 4130.0, "Штурмовой базовый лагерь"),
            TrackWaypoint("Скалы Пастухова", 43.3280, 42.4520, 4700.0, "Крутой ледовый взлёт"),
            TrackWaypoint("Седловина", 43.3440, 42.4410, 5350.0, "Развилка между вершинами"),
            TrackWaypoint("Западная вершина", endLat, endLon, 5642.0, "Высшая точка Европы")
        )

        return GpsTrack(
            name = "Эльбрус (Приют 11 → Вершина 5642м)",
            points = points,
            description = "Высокогорный штурмовой трек. Набор высоты +1512 м, разреженный воздух, крутизна склонов до 35%.",
            waypoints = waypoints
        )
    }

    /**
     * Alpine mountain crest in Krasnaya Polyana (Rosa Peak 2320m -> Kamenny Stolb 2509m).
     * Includes recorded GPS timestamps and ambient temperature readings.
     */
    private fun createRosaKhutorTrack(): GpsTrack {
        val points = mutableListOf<GpsPoint>()
        val startLat = 43.6120
        val startLon = 40.3150
        val endLat = 43.6260
        val endLon = 40.3320

        val n = 35
        val baseTimeMillis = 1723454100000L // 2024-08-12 09:15:00 UTC
        for (i in 0..n) {
            val f = i.toDouble() / n
            val lat = startLat + (endLat - startLat) * f
            val lon = startLon + (endLon - startLon) * f + (Math.sin(f * Math.PI * 2.0) * 0.002)
            // Ridge with ups and downs
            val ele = 2320.0 + 190.0 * f + Math.sin(f * Math.PI * 4.0) * 45.0
            val time = baseTimeMillis + (i * 115_000L) // ~1.9 min interval (~67 min total)
            val temp = 18.0 - (f * 5.0) + (Math.sin(f * Math.PI) * 1.5) // 13.0°C to 18.5°C
            points.add(
                GpsPoint(
                    latitude = lat,
                    longitude = lon,
                    elevation = ele,
                    time = time,
                    temperatureCelsius = Math.round(temp * 10.0) / 10.0
                )
            )
        }

        val waypoints = listOf(
            TrackWaypoint("Роза Пик", startLat, startLon, 2320.0, "Станция канатной дороги"),
            TrackWaypoint("Седой Перевал", 43.6190, 40.3235, 2410.0, "Хребтовая седловина"),
            TrackWaypoint("Каменный Столб", endLat, endLon, 2509.0, "Высшая точка хребта Аибга")
        )

        return GpsTrack(
            name = "Красная Поляна (Роза Пик → Каменный Столб)",
            points = points,
            description = "Хребтовый альпийский маршрут с панорамными видами. Содержит запись GPS-времени и температуру воздуха.",
            waypoints = waypoints
        )
    }

    /**
     * Botkin Trail in Yalta, Crimea.
     * Pine mountain forest serpentine trail (+460m ascent).
     * Includes recorded GPS timestamps and ambient temperature readings.
     */
    private fun createBotkinTrailTrack(): GpsTrack {
        val points = mutableListOf<GpsPoint>()
        val startLat = 44.4930
        val startLon = 34.1160
        val endLat = 44.5020
        val endLon = 34.0980

        val n = 40
        val baseTimeMillis = 1725526800000L // 2024-09-05 10:00:00 UTC
        for (i in 0..n) {
            val f = i.toDouble() / n
            val lat = startLat + (endLat - startLat) * f
            val lon = startLon + (endLon - startLon) * f + (Math.sin(f * Math.PI * 5.0) * 0.004)
            val ele = 320.0 + (780.0 - 320.0) * f
            val time = baseTimeMillis + (i * 140_000L) // ~2.3 min interval (~93 min total)
            val temp = 24.0 - (f * 4.5) // 19.5°C to 24.0°C
            points.add(
                GpsPoint(
                    latitude = lat,
                    longitude = lon,
                    elevation = ele,
                    time = time,
                    temperatureCelsius = Math.round(temp * 10.0) / 10.0
                )
            )
        }

        val waypoints = listOf(
            TrackWaypoint("Поляна Сказок", startLat, startLon, 280.0, "Старт тропы у зоопарка"),
            TrackWaypoint("Скала Ставри-Кая", 44.4990, 34.1030, 663.0, "Панорамная видовая площадка"),
            TrackWaypoint("Водопад Учан-Су", endLat, endLon, 390.0, "Высокогорный водопад")
        )

        return GpsTrack(
            name = "Боткинская тропа (Крым, Ялта)",
            points = points,
            description = "Классический терренкур и серпантин по крымской сосне. Содержит запись времени и датчик температуры.",
            waypoints = waypoints
        )
    }

    /**
     * Pure flat forest track in Meshchera National Park.
     * Almost 0m climb, long horizontal distance along river curves.
     */
    private fun createMeshcheraFlatTrack(): GpsTrack {
        val points = mutableListOf<GpsPoint>()
        val startLat = 55.1200
        val startLon = 40.2500
        val endLat = 55.1850
        val endLon = 40.3200

        val n = 50
        for (i in 0..n) {
            val f = i.toDouble() / n
            val lat = startLat + (endLat - startLat) * f
            val lon = startLon + (endLon - startLon) * f + (Math.sin(f * Math.PI * 3.0) * 0.007)
            val ele = 98.0 + Math.sin(f * Math.PI * 2.0) * 5.0
            points.add(GpsPoint(latitude = lat, longitude = lon, elevation = ele))
        }

        return GpsTrack(
            name = "Мещера (Лесная равнинная тропа у реки)",
            points = points,
            description = "Чисто равнинный трек в сосновом бору (набор всего ~15 м). Отличный пример для сравнения с горными коэффициентами."
        )
    }

    /**
     * Cape Aya circular mountain loop track in Crimea (start == finish).
     * Tests loop track detection, max distance reached from start, and elevation profiles.
     */
    private fun createCapeAyaLoopTrack(): GpsTrack {
        val points = mutableListOf<GpsPoint>()
        val startLat = 44.4300
        val startLon = 33.6800
        val n = 48

        for (i in 0..n) {
            val theta = (i.toDouble() / n) * 2.0 * Math.PI
            // Elliptical loop around mountain ridge
            val lat = startLat + 0.022 * (1.0 - Math.cos(theta))
            val lon = startLon + 0.028 * Math.sin(theta)
            // Climb to coastal peak (680m) halfway through, then descend back to 310m
            val peakProgress = Math.sin(theta / 2.0)
            val ele = 310.0 + (680.0 - 310.0) * peakProgress * peakProgress
            points.add(GpsPoint(latitude = lat, longitude = lon, elevation = ele))
        }

        return GpsTrack(
            name = "Мыс Айя (Кольцевой горный трек)",
            points = points,
            description = "Кольцевой маршрут вокруг скального массива. Старт и финиш в одной точке, радиальное удаление до вершины 4.9 км, набор высоты +370 м."
        )
    }

    /**
     * 2D mountain track without elevation data (Фишт - Оштен, Кавказ).
     * All points have elevation = 0.0 m. Used to test DEM elevation lookup.
     */
    private fun create2dMountainTrack(): GpsTrack {
        val points = mutableListOf<GpsPoint>()
        val startLat = 43.9850
        val startLon = 39.8700
        val endLat = 44.0250
        val endLon = 39.9350
        val n = 40

        for (i in 0..n) {
            val f = i.toDouble() / n
            val lat = startLat + (endLat - startLat) * f
            val lon = startLon + (endLon - startLon) * f + (Math.sin(f * Math.PI * 2.0) * 0.004)
            // Elevation is intentionally absent / 0.0m
            points.add(GpsPoint(latitude = lat, longitude = lon, elevation = 0.0))
        }

        return GpsTrack(
            name = "Кавказ: Фишт-Оштен (2D трек без высот)",
            points = points,
            description = "Маршрут через Лагонакское нагорье. Данные о высоте отсутствуют (все точки 0 м). Подходит для тестирования загрузки рельефа из DEM (SRTM/Copernicus)."
        )
    }
}
