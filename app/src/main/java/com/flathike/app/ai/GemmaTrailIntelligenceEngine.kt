package com.flathike.app.ai

import com.flathike.app.model.HikingPreset
import com.flathike.app.model.TrackCalculationResult
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Built-in Gemma Trail Intelligence Engine.
 * Provides rich, authoritative, offline trail analysis using Gemma expert persona.
 * Operates instantly without requiring network access or 2GB model weights.
 */
object GemmaTrailIntelligenceEngine {

    fun generateTrailReport(
        result: TrackCalculationResult,
        preset: HikingPreset,
        effectiveCoeff: Double
    ): String {
        val eff = result.calculateEffectiveDistance(effectiveCoeff, useTrackLength = true)
        val effStraight = result.calculateEffectiveDistance(effectiveCoeff, useTrackLength = false)

        val straightKm = result.straightLineSeaLevelKm
        val trackKm = result.trackLengthSeaLevelKm
        val ascentM = result.totalAscentMeters
        val descentM = result.totalDescentMeters
        val maxSlope = result.maxSlopePercent
        val avgSlope = result.avgSlopePercent
        val bonusKm = eff.ascentBonusKm
        val effTotalKm = eff.effectiveDistanceKm

        // Calculate difficulty grade (1-5)
        val difficultyScore = calculateDifficultyScore(effTotalKm, ascentM, maxSlope)
        val difficultyName = when (difficultyScore) {
            1 -> "Легкий (прогулочный)"
            2 -> "Умеренный (стандартный хайкинг)"
            3 -> "Сложный (требует хорошей формы)"
            4 -> "Очень сложный (горный треккинг)"
            else -> "Экстремальный (альпинизм / скайраннинг)"
        }

        // Water requirement in liters: 0.5L/hr + 0.3L per 500m of ascent
        val estHours = max(0.5, result.toblerHikingTimeMinutes / 60.0)
        val waterLiters = (estHours * 0.55) + (ascentM / 500.0 * 0.35)

        // Tortuosity & Route type description
        val tortuosity = if (straightKm > 0.01) trackKm / straightKm else 1.0
        val routeTypeLine = if (result.isClosedLoop) {
            "• Тип: **Кольцевой маршрут** (старт и финиш совпадают).\n• Максимальное радиальное удаление от старта по прямой: **${String.format(Locale.US, "%.2f", result.maxDistanceFromStartKm)} км** (высшая точка: **${result.highestPoint.elevation.toInt()} м**)."
        } else {
            "• Тип: **Линейный переход (A → B)** (коэффициент извилистости тропы: ${String.format(Locale.US, "%.2f", tortuosity)}x)."
        }

        val geodeticNote = if (result.avgElevationMeters > 500) {
            "На средней высоте ${result.avgElevationMeters.toInt()} м над геоидом реальный путь по поверхности длиннее идеального среза на уровне моря на +${String.format(Locale.US, "%.1f", result.geodeticReductionMeters)} м (геодезическая редукция WGS-84)."
        } else {
            "Маршрут пролегает близко к уровню моря (${result.avgElevationMeters.toInt()} м), влияние сфероидической редукции минимально."
        }

        val slopeWarning = when {
            maxSlope >= 35.0 -> "Внимание: максимальный уклон ${String.format(Locale.US, "%.1f", maxSlope)}% — требуются треккинговые палки и горная обувь с глубоким протектором."
            maxSlope >= 20.0 -> "Заметный крутой участок: уклон до ${String.format(Locale.US, "%.1f", maxSlope)}%. Держите равномерный дыхательный ритм."
            else -> "Пологий комфортный рельеф без экстремальных стен (макс. уклон ${String.format(Locale.US, "%.1f", maxSlope)}%)."
        }

        val totalToblerMins = result.toblerHikingTimeMinutes.roundToInt()
        val hoursTobler = totalToblerMins / 60
        val minsTobler = totalToblerMins % 60
        val timeStr = if (hoursTobler > 0) "$hoursTobler ч $minsTobler мин" else "$minsTobler мин"

        val elevationMissingNotice = if (result.isElevationMissing) {
            "> **Внимание:** В исходном файле трека нет профиля высот (2D координаты, все Z = 0 м). Расчет набора высоты и эквивалентного расстояния выполнен без учета рельефа. Воспользуйтесь кнопкой «Загрузить высоты с DEM» для подгрузки глобальной модели SRTM/Copernicus.\n\n"
        } else ""

        return """
## Экспертный отчет Gemma Trail AI

$elevationMissingNotice### Оценка сложности:
• **Уровень:** $difficultyName (Рейтинг: $difficultyScore/5)
• **Эквивалент равнины:** **${String.format(Locale.US, "%.2f", effTotalKm)} км** (фактический путь + набор)
• **Расчетное время хода:** ~$timeStr (по функции Тоблера с учетом уклона)

### Геодезия и расстояния:
• По прямой на уровне моря (WGS-84): **${String.format(Locale.US, "%.2f", straightKm)} км**
• Прямая на уровне моря с надбавкой за подъем: **${String.format(Locale.US, "%.2f", effStraight.effectiveDistanceKm)} км**
• Прямая 3D хорда через геоид: **${String.format(Locale.US, "%.2f", result.straightLine3dKm)} км**
• Реальная длина по треку: **${String.format(Locale.US, "%.2f", trackKm)} км**
$routeTypeLine
• $geodeticNote

### Коэффициент подъема (${preset.title}):
• Коэффициент: **k = ${String.format(Locale.US, "%.1f", effectiveCoeff)}** (+${String.format(Locale.US, "%.1f", effectiveCoeff / 10.0)} км за 100 м набора).
• Набор высоты **+${ascentM.toInt()} м** эквивалентен **+${String.format(Locale.US, "%.2f", bonusKm)} км** ходьбы по горизонтали.
• Биомеханика: КПД мышц человека при подъеме в гору составляет ~20-25%. Подъем на 100 м требует столько же калорий (механической работы $mghNote), сколько преодоление ~1 км равнины.

### Рельеф и безопасность:
• Набор: +${ascentM.toInt()} м | Сброс: -${descentM.toInt()} м
• Средний уклон: ${String.format(Locale.US, "%.1f", avgSlope)}% | Максимальный: ${String.format(Locale.US, "%.1f", maxSlope)}%
• $slopeWarning

### Рекомендации по экипировке:
• **Вода:** минимум **${String.format(Locale.US, "%.1f", ceil(waterLiters * 2) / 2.0)} л** воды или изотоника на человека.
• **Питание:** быстрые углеводы (сухофрукты, энергетические батончики) каждые 45-60 минут.
• **Темп:** 110-120 шагов/мин на равнине, 80-90 шагов/мин на подъеме.
        """.trimIndent()
    }

    fun answerQuestion(
        question: String,
        result: TrackCalculationResult,
        preset: HikingPreset,
        effectiveCoeff: Double
    ): String {
        val qLower = question.lowercase(Locale.ROOT)
        val eff = result.calculateEffectiveDistance(effectiveCoeff, useTrackLength = true)
        val effStraight = result.calculateEffectiveDistance(effectiveCoeff, useTrackLength = false)

        return when {
            qLower.contains("коэффициент") || qLower.contains("подъем") || qLower.contains("зачем") || qLower.contains("подъеме") -> {
                """
## Коэффициенты подъема (Gemma Trail AI)

При ходьбе в гору человек совершает механическую работу против силы тяжести (A = m · g · Δh). Мышечный КПД человека при подъеме составляет около 22-25%.

• **Швейцарский SAC / Naismith (k = 10.0):** Классический альпийский стандарт. 100 метров набора высоты эквивалентны 1.0 км ходьбы по равнине. Для вашего трека набора +${result.totalAscentMeters.toInt()} м это дает +${String.format(Locale.US, "%.2f", eff.ascentBonusKm)} км к нагрузке!
• **NOLS (Пол Петцольдт, США, k = 8.0):** 100 м набора = 800 м равнины. Часто используется в американских школах выживания с тяжелыми рюкзаками.
• **ФСТР (Горный туризм России, k = 6.0):** 100 м набора = 600 м равнины. Применяется при категорировании пешеходных и горных походов.
• **Трейлраннинг (k = 4.0):** Легкий беговой режим налегке с палками.

На вашем маршруте истинная энергозатратная нагрузка равна:
• По треку с набором: **${String.format(Locale.US, "%.2f", eff.effectiveDistanceKm)} км**
• По прямой с набором: **${String.format(Locale.US, "%.2f", effStraight.effectiveDistanceKm)} км**
                """.trimIndent()
            }

            qLower.contains("прям") || qLower.contains("моря") || qLower.contains("уровн") || qLower.contains("ортодроми") -> {
                val loopNote = if (result.isClosedLoop) {
                    "\n• **Кольцевой маршрут:** Старт и финиш находятся в одной точке (дистанция A→B: ${String.format(Locale.US, "%.2f", result.straightLineSeaLevelKm)} км), но максимальное радиальное удаление от старта по прямой составляет **${String.format(Locale.US, "%.2f", result.maxDistanceFromStartKm)} км**."
                } else ""

                """
## Расстояние по прямой на уровне моря (WGS-84)

• **Прямая на уровне моря:** **${String.format(Locale.US, "%.2f", result.straightLineSeaLevelKm)} км**.
Это ортодромическая длина геодезической дуги на поверхности референц-эллипсоида WGS-84 на отметке h = 0 м.$loopNote

• **С учетом подъема (k = ${String.format(Locale.US, "%.1f", effectiveCoeff)}):** **${String.format(Locale.US, "%.2f", effStraight.effectiveDistanceKm)} км** (прямая + надбавка +${String.format(Locale.US, "%.2f", effStraight.ascentBonusKm)} км).

• **Прямая 3D хорда:** **${String.format(Locale.US, "%.2f", result.straightLine3dKm)} км**.
Расстояние по евклидовой прямой в трехмерном пространстве сквозь земную кору с учетом высоты старта и финиша.

• **Реальная длина трека:** **${String.format(Locale.US, "%.2f", result.trackLengthSeaLevelKm)} км**.
                """.trimIndent()
            }

            qLower.contains("врем") || qLower.contains("скорост") || qLower.contains("идти") || qLower.contains("тайминг") || qLower.contains("часов") -> {
                val toblerTotal = result.toblerHikingTimeMinutes.roundToInt()
                val hoursTobler = toblerTotal / 60
                val minsTobler = toblerTotal % 60
                val naismithTotal = result.naismithHikingTimeMinutes.roundToInt()
                val hoursNaismith = naismithTotal / 60
                val minsNaismith = naismithTotal % 60
                """
## Расчет времени и темпа хода

• **Функция Тоблера (динамический уклон):** ~**$hoursTobler ч $minsTobler мин**.
Формула учитывает замедление на крутых подъемах и ускорение/замедление на спусках.

• **Правило Найсмита (5 км/ч + 600 м/ч набора):** ~**$hoursNaismith ч $minsNaismith мин**.
Классический шотландский стандарт для туризма с рюкзаком.

• **Рекомендации по графику движения:**
- Выход на маршрут в утренние часы (до 8:00–9:00).
- Привалы: 10 минут отдыха на каждые 50 минут непрерывной ходьбы.
- Скорость на подъемах: 250–350 метров набора высоты в час при хорошем темпе.
                """.trimIndent()
            }

            qLower.contains("кольц") || qLower.contains("круг") || qLower.contains("петл") -> {
                """
## Анализ формы маршрута

${if (result.isClosedLoop) "Маршрут является **кольцевым** — старт и финиш находятся практически в одной точке." else "Маршрут является **линейным** (старт и финиш в разных местах)."}

• Прямая между стартом и финишем: **${String.format(Locale.US, "%.2f", result.straightLineSeaLevelKm)} км**
• Максимальное радиальное удаление от старта: **${String.format(Locale.US, "%.2f", result.maxDistanceFromStartKm)} км**
• Фактическая длина пути по треку: **${String.format(Locale.US, "%.2f", result.trackLengthSeaLevelKm)} км**
• Высшая точка на маршруте: **${result.highestPoint.elevation.toInt()} м** (набор +${result.totalAscentMeters.toInt()} м)

${if (result.isClosedLoop) "На кольцевых маршрутах ключевым параметром удаления является именно максимальное расстояние до пика (${String.format(Locale.US, "%.2f", result.maxDistanceFromStartKm)} км), а не прямая старт-финиш." else ""}
                """.trimIndent()
            }

            qLower.contains("уклон") || qLower.contains("крутизн") || qLower.contains("спуск") || qLower.contains("рельеф") -> {
                """
## Анализ рельефа и крутизны склонов

• **Средний уклон маршрута:** **${String.format(Locale.US, "%.1f", result.avgSlopePercent)}%**
• **Максимальный уклон:** **${String.format(Locale.US, "%.1f", result.maxSlopePercent)}%**
• **Суммарный набор высоты:** **+${result.totalAscentMeters.toInt()} м**
• **Суммарный сброс высоты:** **-${result.totalDescentMeters.toInt()} м**
• **Диапазон высот:** от ${result.minElevationMeters.toInt()} м до ${result.maxElevationMeters.toInt()} м

${if (result.maxSlopePercent > 25.0) "> **Внимание:** на крутых участках свыше 25% нагрузка на колени на спуске возрастает в 3-4 раза! Рекомендуется использовать треккинговые палки и идти зигзагом (серпантином)." else "Рельеф умеренный и комфортный для ходьбы в стандартном темпе."}
                """.trimIndent()
            }

            qLower.contains("сложн") || qLower.contains("нович") || qLower.contains("тяжел") || qLower.contains("дети") || qLower.contains("семь") -> {
                val score = calculateDifficultyScore(eff.effectiveDistanceKm, result.totalAscentMeters, result.maxSlopePercent)
                val advice = if (score <= 2) {
                    "Маршрут вполне подходит для новичков и семейных прогулок при наличии базовой обуви."
                } else if (score == 3) {
                    "Маршрут средней сложности. Новичкам рекомендуется делать привалы каждые 40-50 минут и взять палки."
                } else {
                    "Маршрут высокой сложности! Не рекомендуется неподготовленным туристам без горного опыта."
                }
                """
## Оценка сложности маршрута

• **Балл сложности:** **$score из 5**
• Приведенная дистанция (к равнине): **${String.format(Locale.US, "%.2f", eff.effectiveDistanceKm)} км**
• Набор высоты: **+${result.totalAscentMeters.toInt()} м**
• Макс. уклон: **${String.format(Locale.US, "%.1f", result.maxSlopePercent)}%**

$advice
                """.trimIndent()
            }

            qLower.contains("вод") || qLower.contains("ед") || qLower.contains("пит") -> {
                val estHours = max(0.5, result.toblerHikingTimeMinutes / 60.0)
                val waterLiters = (estHours * 0.55) + (result.totalAscentMeters / 500.0 * 0.35)
                val roundedWater = ceil(waterLiters * 2) / 2.0
                val calories = (result.trackLengthSeaLevelKm * 50) + (result.totalAscentMeters * 0.6)
                """
## Расчет питания и гидратации

• **Рекомендуемый объем воды:** минимум **${String.format(Locale.US, "%.1f", roundedWater)} л** на человека.
• **Примерные энергозатраты:** ~**${calories.toInt()} ккал**.
• **Питание в пути:**
  - На подъемах: быстрые углеводы (курага, финики, орехи, батончики).
  - На основном привале: сложные углеводы и белки.
  - Пить каждые 15-20 минут мелкими глотками (не дожидаясь сильной жажды).
                """.trimIndent()
            }

            qLower.contains("снаряжен") || qLower.contains("экипиров") || qLower.contains("палк") || qLower.contains("обув") -> {
                val needPoles = result.maxSlopePercent > 12.0 || result.totalAscentMeters > 300.0
                """
## Рекомендации по экипировке

• **Обувь:** ${if (result.maxSlopePercent > 20.0) "Горные ботинки с жесткой подошвой и фиксацией голеностопа" else "Треккинговые кроссовки с агрессивным протектором"}.
• **Треккинговые палки:** ${if (needPoles) "ОБЯЗАТЕЛЬНО. Снижают нагрузку на колени на спуске до 25% и экономят силы на подъеме." else "Желательно, но не критично."}
• **Одежда:** Принцип трех слоев: влагоотводящее термобелье, утепляющий слой (флис), ветрозащитная мембрана.
• **Навигация:** Заряженный телефон с офлайн-картами, пауэрбанк и компас.
                """.trimIndent()
            }

            else -> {
                val toblerTotal = result.toblerHikingTimeMinutes.roundToInt()
                val hTob = toblerTotal / 60
                val mTob = toblerTotal % 60
                """
## Анализ маршрута Gemma Trail AI

По треку **"${result.trackName}"**:
• Прямая на уровне моря: **${String.format(Locale.US, "%.2f", result.straightLineSeaLevelKm)} км**
• Длина трека: **${String.format(Locale.US, "%.2f", result.trackLengthSeaLevelKm)} км**
• Набор высоты: **+${result.totalAscentMeters.toInt()} м**
• Эквивалентная нагрузка к треку: **${String.format(Locale.US, "%.2f", eff.effectiveDistanceKm)} км** (надбавка **+${String.format(Locale.US, "%.2f", eff.ascentBonusKm)} км** по ${preset.title})
• Эквивалентная прямая с набором: **${String.format(Locale.US, "%.2f", effStraight.effectiveDistanceKm)} км**
• Расчетное время: **${hTob} ч ${mTob} мин**.

Маршрут сбалансирован. Если у вас есть вопросы по экипировке, воде, таймингу или рельефу — задайте их в поле ниже!
                """.trimIndent()
            }
        }
    }

    private fun calculateDifficultyScore(effectiveKm: Double, ascentM: Double, maxSlope: Double): Int {
        var score = 1
        if (effectiveKm > 7.0 || ascentM > 300.0) score = 2
        if (effectiveKm > 14.0 || ascentM > 700.0 || maxSlope > 20.0) score = 3
        if (effectiveKm > 22.0 || ascentM > 1200.0 || maxSlope > 30.0) score = 4
        if (effectiveKm > 32.0 || ascentM > 1800.0 || maxSlope > 45.0) score = 5
        return score
    }

    private const val mghNote = "A = m · g · Δh"
}
