package com.flathike.app.ai

import com.flathike.app.model.HikingPreset
import com.flathike.app.model.TrackCalculationResult
import java.util.Locale

/**
 * Builds structured prompts for Gemma models analyzing hiking tracks.
 */
object GemmaPromptBuilder {

    fun buildSystemPrompt(): String {
        return """
            Ты — Gemma Trail AI, интеллектуальный ассистент по геодезии, горному туризму и биомеханике ходьбы.
            Ты специализируешься на анализе треков GPS, расчете дистанций по прямой на уровне моря,
            коэффициентов подъема (SAC/Naismith, Petzoldt NOLS, ФСТР), крутизны склонов и энергетических затрат.
            Отвечай четко, структурированно, профессионально и на понятном русском языке с практическими советами.
        """.trimIndent()
    }

    fun buildTrailAnalysisPrompt(
        result: TrackCalculationResult,
        preset: HikingPreset,
        effectiveCoeff: Double
    ): String {
        val eff = result.calculateEffectiveDistance(effectiveCoeff, useTrackLength = true)
        val effStraight = result.calculateEffectiveDistance(effectiveCoeff, useTrackLength = false)
        val tortuosity = if (result.straightLineSeaLevelKm > 0.001) {
            result.trackLengthSeaLevelKm / result.straightLineSeaLevelKm
        } else 1.0

        val routeTypeDesc = if (result.isClosedLoop) {
            "Кольцевой маршрут (старт и финиш совпадают). Максимальное радиальное удаление от старта: ${formatNum(result.maxDistanceFromStartKm)} км."
        } else {
            "Линейный маршрут (из точки A в точку B)."
        }

        return """
            Проанализируй следующий маршрут GPS:
            
            НАЗВАНИЕ: ${result.trackName}
            Тип: $routeTypeDesc
            Точек в треке: ${result.pointCount}
            Старт: ${formatCoord(result.startPoint.latitude)}, ${formatCoord(result.startPoint.longitude)}, высота: ${result.startPoint.elevation.toInt()} м
            Финиш: ${formatCoord(result.endPoint.latitude)}, ${formatCoord(result.endPoint.longitude)}, высота: ${result.endPoint.elevation.toInt()} м
            Высшая точка маршрута: ${result.highestPoint.elevation.toInt()} м
            
            ГЕОДЕЗИЧЕСКИЕ ДИСТАНЦИИ:
            - Расстояние по прямой на уровне моря (WGS-84 ортодромия): ${formatNum(result.straightLineSeaLevelKm)} км
            ${if (result.isClosedLoop) "- Макс. удаление от старта по прямой на уровне моря: ${formatNum(result.maxDistanceFromStartKm)} км" else ""}
            - Прямая 3D хорда через геоид: ${formatNum(result.straightLine3dKm)} км
            - Фактическая длина трека на уровне моря: ${formatNum(result.trackLengthSeaLevelKm)} км
            - Истинная 3D длина трека по рельефу: ${formatNum(result.trackLength3dKm)} км
            - Коэффициент извилистости (трек / прямая): ${if (result.isClosedLoop) "Кольцо" else "${formatNum(tortuosity)}x"}
            - Геодезическая редукция (разница за счет высоты): ${formatNum(result.geodeticReductionMeters)} м
            
            ВЫСОТНЫЙ ПРОФИЛЬ И ПОДЪЕМ:
            - Суммарный набор высоты (Ascent): +${result.totalAscentMeters.toInt()} м
            - Суммарный сброс (Descent): -${result.totalDescentMeters.toInt()} м
            - Высоты: мин ${result.minElevationMeters.toInt()} м, макс ${result.maxElevationMeters.toInt()} м, средняя ${result.avgElevationMeters.toInt()} м
            - Уклон: средний ${formatNum(result.avgSlopePercent)}%, максимальный ${formatNum(result.maxSlopePercent)}%
            
            КОЭФФИЦИЕНТЫ ПОДЪЕМА И ЭКВИВАЛЕНТ РАВНИНЫ:
            - Выбранный стандарт: ${preset.title}
            - Коэффициент подъема: k = ${formatNum(effectiveCoeff)} (добавляет ${formatNum(effectiveCoeff / 10.0)} км за каждые 100 м набора)
            - Эквивалентная дистанция трека с учетом подъема: ${formatNum(eff.effectiveDistanceKm)} км (+${formatNum(eff.ascentBonusKm)} км за подъем)
            - Эквивалентная прямая на уровне моря с учетом подъема: ${formatNum(effStraight.effectiveDistanceKm)} км
            
            РАСЧЕТНОЕ ВРЕМЯ ХОДЬБЫ:
            - Функция Тоблера (с переменным уклоном): ${formatMinutes(result.toblerHikingTimeMinutes)}
            - Правило Найсмита (5 км/ч + 600 м/ч): ${formatMinutes(result.naismithHikingTimeMinutes)}
            ${if (result.recordedTimeMetrics != null) """
            
            ФАКТИЧЕСКИЕ ДАННЫЕ ЗАПИСИ (GPS ТАЙМИНГ):
            - Время в движении: ${formatMinutes(result.recordedTimeMetrics.movingDurationSeconds / 60.0)}, общее время: ${formatMinutes(result.recordedTimeMetrics.totalDurationSeconds / 60.0)}
            - Фактическая скорость в движении: ${formatNum(result.recordedTimeMetrics.avgMovingSpeedKmH)} км/ч (максимальная: ${formatNum(result.recordedTimeMetrics.maxSpeedKmH)} км/ч)
            - Реальный темп: ${formatNum(result.recordedTimeMetrics.paceMinutesPerKm)} мин/км
            """.trimIndent() else ""}
            ${if (result.recordedTemperatureMetrics != null) """
            
            ТЕМПЕРАТУРА ВОЗДУХА ПО ДАТЧИКАМ ТРЕКА:
            - Средняя температура: ${formatNum(result.recordedTemperatureMetrics.avgCelsius)}°C (диапазон: ${formatNum(result.recordedTemperatureMetrics.minCelsius)}°C ... ${formatNum(result.recordedTemperatureMetrics.maxCelsius)}°C)
            """.trimIndent() else ""}
            
            Пожалуйста, дай краткий и емкий экспертный отчет:
            1. Оценка сложности маршрута (баллы от 1 до 5) и рельефа.
            2. Разбор коэффициента подъема: почему набор высоты эквивалентен +${formatNum(eff.ascentBonusKm)} км равнины.
            3. Рекомендации по темпу, воде, питанию и безопасности на крутых участках.
        """.trimIndent()
    }

    fun buildQuestionPrompt(
        question: String,
        result: TrackCalculationResult,
        preset: HikingPreset,
        effectiveCoeff: Double
    ): String {
        return """
            Контекст маршрута "${result.trackName}":
            - Прямая на уровне моря: ${formatNum(result.straightLineSeaLevelKm)} км
            - Длина трека: ${formatNum(result.trackLengthSeaLevelKm)} км
            - Набор высоты: +${result.totalAscentMeters.toInt()} м, сброс: -${result.totalDescentMeters.toInt()} м
            - Макс. уклон: ${formatNum(result.maxSlopePercent)}%, Средний: ${formatNum(result.avgSlopePercent)}%
            - Коэффициент набора: k = ${formatNum(effectiveCoeff)} (${preset.title})
            - Эквивалентная дистанция: ${formatNum(result.calculateEffectiveDistance(effectiveCoeff).effectiveDistanceKm)} км
            - Расчетное время: ${formatMinutes(result.toblerHikingTimeMinutes)}
            
            Вопрос пользователя:
            "$question"
            
            Ответь как эксперт Gemma Trail AI кратко, точно и содержательно.
        """.trimIndent()
    }

    private fun formatNum(v: Double): String = String.format(Locale.US, "%.2f", v)
    private fun formatCoord(v: Double): String = String.format(Locale.US, "%.5f", v)

    private fun formatMinutes(minutes: Double): String {
        val totalMin = minutes.toInt()
        val hours = totalMin / 60
        val mins = totalMin % 60
        return if (hours > 0) "${hours} ч ${mins} мин" else "${mins} мин"
    }
}
