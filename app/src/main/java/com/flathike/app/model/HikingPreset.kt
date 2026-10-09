package com.flathike.app.model

/**
 * Standard hiking/outdoor elevation effort coefficient presets.
 *
 * In outdoor hiking physics, ascending requires significant mechanical work
 * against gravity. An elevation gain of ΔH is often converted into an equivalent
 * flat horizontal distance:
 *   D_eff = D_flat + (k_up * Ascent_meters / 1000.0)
 *
 * For example, k_up = 10.0 means 100m of ascent equals 1.0 km of flat walking.
 */
enum class HikingPreset(
    val title: String,
    val subtitle: String,
    val ascentCoefficient: Double, // km of flat equivalent per 1000m of climb
    val description: String
) {
    SWISS_SAC(
        title = "Швейцарский SAC / Naismith",
        subtitle = "+1.0 км за 100м набора (k = 10.0)",
        ascentCoefficient = 10.0,
        description = "Классический швейцарский стандарт (SAC / Naismith). 100 м набора высоты эквивалентны 1 км ходьбы по равнине."
    ),
    PETZOLDT_NOLS(
        title = "Школа NOLS (Petzoldt)",
        subtitle = "+0.8 км за 100м набора (k = 8.0)",
        ascentCoefficient = 8.0,
        description = "Правило Пола Петцольдта (NOLS, США). 100 м набора равны 800 м равнины по затратам энергии."
    ),
    RUSSIAN_FSTR(
        title = "Горный туризм (ФСТР)",
        subtitle = "+0.6 км за 100м набора (k = 6.0)",
        ascentCoefficient = 6.0,
        description = "Методика классификации спортивных маршрутов ФСТР: на каждый километр перепада добавляется 6 км пути."
    ),
    TRAIL_RUNNING(
        title = "Трейлраннинг / Бег",
        subtitle = "+0.4 км за 100м набора (k = 4.0)",
        ascentCoefficient = 4.0,
        description = "Коэффициент для бега по пересеченной местности налегке: 100 м набора эквивалентны ~400 м плоского бега."
    ),
    FLAT_ONLY(
        title = "Без учета подъема",
        subtitle = "+0.0 км (k = 0.0)",
        ascentCoefficient = 0.0,
        description = "Только горизонтальная дистанция без надбавки за подъем."
    ),
    CUSTOM(
        title = "Пользовательский",
        subtitle = "Ручная настройка коэффициента",
        ascentCoefficient = 10.0,
        description = "Настройте любой желаемый коэффициент набора высоты."
    );

    fun getTitle(isRu: Boolean): String = when (this) {
        SWISS_SAC -> if (isRu) "Швейцарский SAC / Naismith" else "Swiss SAC / Naismith"
        PETZOLDT_NOLS -> if (isRu) "Школа NOLS (Petzoldt)" else "NOLS School (Petzoldt)"
        RUSSIAN_FSTR -> if (isRu) "Горный туризм (ФСТР)" else "Mountain Hiking (FSTR)"
        TRAIL_RUNNING -> if (isRu) "Трейлраннинг / Бег" else "Trail Running"
        FLAT_ONLY -> if (isRu) "Без учета подъема" else "Flat Distance Only"
        CUSTOM -> if (isRu) "Пользовательский" else "Custom"
    }

    fun getSubtitle(isRu: Boolean): String = when (this) {
        SWISS_SAC -> if (isRu) "+1.0 км за 100м набора (k = 10.0)" else "+1.0 km per 100m climb (k = 10.0)"
        PETZOLDT_NOLS -> if (isRu) "+0.8 км за 100м набора (k = 8.0)" else "+0.8 km per 100m climb (k = 8.0)"
        RUSSIAN_FSTR -> if (isRu) "+0.6 км за 100м набора (k = 6.0)" else "+0.6 km per 100m climb (k = 6.0)"
        TRAIL_RUNNING -> if (isRu) "+0.4 км за 100м набора (k = 4.0)" else "+0.4 km per 100m climb (k = 4.0)"
        FLAT_ONLY -> if (isRu) "+0.0 км (k = 0.0)" else "+0.0 km (k = 0.0)"
        CUSTOM -> if (isRu) "Ручная настройка коэффициента" else "Manual coefficient setting"
    }

    fun getDescription(isRu: Boolean): String = when (this) {
        SWISS_SAC -> if (isRu) "Классический швейцарский стандарт (SAC / Naismith). 100 м набора высоты эквивалентны 1 км ходьбы по равнине." else "Classic Swiss standard (SAC / Naismith). 100m of ascent equals 1 km of flat walking."
        PETZOLDT_NOLS -> if (isRu) "Правило Пола Петцольдта (NOLS, США). 100 м набора равны 800 м равнины по затратам энергии." else "Paul Petzoldt rule (NOLS, USA). 100m climb equals 800m flat in energy expenditure."
        RUSSIAN_FSTR -> if (isRu) "Методика классификации спортивных маршрутов ФСТР: на каждый километр перепада добавляется 6 км пути." else "FSTR sports route classification: 6 km added per kilometer of elevation gain."
        TRAIL_RUNNING -> if (isRu) "Коэффициент для бега по пересеченной местности налегке: 100 м набора эквивалентна ~400 м плоского бега." else "Lightweight trail running coefficient: 100m climb equals ~400m flat running."
        FLAT_ONLY -> if (isRu) "Только горизонтальная дистанция без надбавки за подъем." else "Horizontal distance only without climb bonus."
        CUSTOM -> if (isRu) "Настройте любой желаемый коэффициент набора высоты." else "Set any desired elevation gain coefficient."
    }
}

