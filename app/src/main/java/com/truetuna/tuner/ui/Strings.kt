package com.truetuna.tuner.ui

import com.truetuna.tuner.model.Language

/**
 * Localization strings for RU, UA, EN.
 * Musical notation stays in international Latin.
 */
object Strings {

    fun appName(lang: Language) = "TrueTuna"

    fun permissionTitle(lang: Language) = when (lang) {
        Language.RU -> "Нужен доступ к микрофону"
        Language.UA -> "Потрібен доступ до мікрофона"
        Language.EN -> "Microphone Access Needed"
    }

    fun permissionBody(lang: Language) = when (lang) {
        Language.RU -> "TrueTuna использует микрофон для определения высоты тона.\nРазрешите доступ, чтобы начать тюнинг."
        Language.UA -> "TrueTuna використовує мікрофон для визначення висоти тону.\nДозвольте доступ, щоб почати налаштування."
        Language.EN -> "TrueTuna uses the microphone to detect pitch.\nGrant access to start tuning."
    }

    fun permissionButton(lang: Language) = when (lang) {
        Language.RU -> "Разрешить"
        Language.UA -> "Дозволити"
        Language.EN -> "Allow"
    }

    fun permissionDeniedMsg(lang: Language) = when (lang) {
        Language.RU -> "Доступ к микрофону запрещён. Разрешите в настройках приложения."
        Language.UA -> "Доступ до мікрофона заборонено. Дозвольте у налаштуваннях."
        Language.EN -> "Microphone permission denied. Please enable it in App Settings."
    }

    fun modeChromatic(lang: Language) = when (lang) {
        Language.RU -> "Хроматик"
        Language.UA -> "Хроматик"
        Language.EN -> "Chromatic"
    }

    fun modeGuitar(lang: Language) = when (lang) {
        Language.RU -> "Гитара"
        Language.UA -> "Гітара"
        Language.EN -> "Guitar"
    }

    fun inTune(lang: Language) = when (lang) {
        Language.RU -> "В строе"
        Language.UA -> "В строї"
        Language.EN -> "In Tune"
    }

    fun tooLow(lang: Language) = when (lang) {
        Language.RU -> "Ниже"
        Language.UA -> "Нижче"
        Language.EN -> "Flat"
    }

    fun tooHigh(lang: Language) = when (lang) {
        Language.RU -> "Выше"
        Language.UA -> "Вище"
        Language.EN -> "Sharp"
    }

    fun listening(lang: Language) = when (lang) {
        Language.RU -> "Слушаю..."
        Language.UA -> "Слухаю..."
        Language.EN -> "Listening..."
    }

    fun silence(lang: Language) = when (lang) {
        Language.RU -> "Тишина"
        Language.UA -> "Тиша"
        Language.EN -> "Silence"
    }

    fun selectTuning(lang: Language) = when (lang) {
        Language.RU -> "Строй"
        Language.UA -> "Стрій"
        Language.EN -> "Tuning"
    }

    fun autoDetect(lang: Language) = when (lang) {
        Language.RU -> "Авто"
        Language.UA -> "Авто"
        Language.EN -> "Auto"
    }

    fun categoryStandardDrop(lang: Language) = when (lang) {
        Language.RU -> "Стандарт и Дроп"
        Language.UA -> "Стандарт і Дроп"
        Language.EN -> "Standard & Drop"
    }

    fun categoryOpen(lang: Language) = when (lang) {
        Language.RU -> "Открытые строи"
        Language.UA -> "Відкриті строї"
        Language.EN -> "Open Tunings"
    }

    fun categoryAlternative(lang: Language) = when (lang) {
        Language.RU -> "Альтернативные"
        Language.UA -> "Альтернативні"
        Language.EN -> "Alternative"
    }

    fun categoryLabel(lang: Language, key: String) = when (key) {
        "standard_drop" -> categoryStandardDrop(lang)
        "open" -> categoryOpen(lang)
        "alternative" -> categoryAlternative(lang)
        else -> key
    }

    fun referenceA4(lang: Language) = when (lang) {
        Language.RU -> "Эталон A4"
        Language.UA -> "Еталон A4"
        Language.EN -> "Reference A4"
    }

    fun tetMode12(lang: Language) = when (lang) {
        Language.RU -> "12-TET"
        Language.UA -> "12-TET"
        Language.EN -> "12-TET"
    }

    fun tetMode24(lang: Language) = when (lang) {
        Language.RU -> "24-TET ¼"
        Language.UA -> "24-TET ¼"
        Language.EN -> "24-TET ¼"
    }

    fun stringLabel(lang: Language, num: Int) = when (lang) {
        Language.RU -> "Струна $num"
        Language.UA -> "Струна $num"
        Language.EN -> "String $num"
    }

    fun settings(lang: Language) = when (lang) {
        Language.RU -> "Настройки"
        Language.UA -> "Налаштування"
        Language.EN -> "Settings"
    }

    fun quarterTone(lang: Language) = when (lang) {
        Language.RU -> "Четверть тона"
        Language.UA -> "Чверть тону"
        Language.EN -> "Quarter Tone"
    }

    fun openSettings(lang: Language) = when (lang) {
        Language.RU -> "Открыть настройки"
        Language.UA -> "Відкрити налаштування"
        Language.EN -> "Open Settings"
    }

    fun retry(lang: Language) = when (lang) {
        Language.RU -> "Повторить запрос"
        Language.UA -> "Повторити запит"
        Language.EN -> "Try Again"
    }

    fun frequencyLabel(lang: Language) = when (lang) {
        Language.RU -> "Частота"
        Language.UA -> "Частота"
        Language.EN -> "Frequency"
    }

    fun centsLabel(lang: Language) = when (lang) {
        Language.RU -> "Отклонение"
        Language.UA -> "Відхилення"
        Language.EN -> "Deviation"
    }

    fun referenceShort(lang: Language) = when (lang) {
        Language.RU -> "Эталон"
        Language.UA -> "Еталон"
        Language.EN -> "Reference"
    }

    fun languageLabel(lang: Language) = when (lang) {
        Language.RU -> "Язык"
        Language.UA -> "Мова"
        Language.EN -> "Language"
    }

    fun playString(lang: Language) = when (lang) {
        Language.RU -> "Сыграйте струну"
        Language.UA -> "Зіграйте струну"
        Language.EN -> "Play a string"
    }

    fun lockedLabel(lang: Language) = when (lang) {
        Language.RU -> "Зафиксировано"
        Language.UA -> "Зафіксовано"
        Language.EN -> "Locked"
    }
}
