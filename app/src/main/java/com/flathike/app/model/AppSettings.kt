package com.flathike.app.model

/**
 * Display theme modes for the application.
 */
enum class ThemeMode(val titleRu: String, val titleEn: String) {
    SYSTEM("Системная тема", "System Default"),
    LIGHT("Светлая тема", "Light Theme"),
    DARK("Тёмная тема", "Dark Theme")
}

/**
 * Supported UI languages.
 */
enum class AppLanguage(val code: String, val titleRu: String, val titleEn: String) {
    SYSTEM("system", "По языку системы", "System Default"),
    RU("ru", "Русский (RU)", "Russian (RU)"),
    EN("en", "English (EN)", "English (EN)")
}
