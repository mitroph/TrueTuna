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
        "bass" -> when (lang) {
            Language.RU -> "Бас-гитара"
            Language.UA -> "Бас-гітара"
            Language.EN -> "Bass"
        }
        "ukulele" -> when (lang) {
            Language.RU -> "Укулеле"
            Language.UA -> "Укулеле"
            Language.EN -> "Ukulele"
        }
        "orchestral" -> when (lang) {
            Language.RU -> "Смычковые"
            Language.UA -> "Смичкові"
            Language.EN -> "Orchestral"
        }
        "custom" -> when (lang) {
            Language.RU -> "Мои строи"
            Language.UA -> "Мої строї"
            Language.EN -> "Custom Tunings"
        }
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

    // ── Instruments ──────────────────────────────────────────────────────────

    fun instrumentLabel(lang: Language) = when (lang) {
        Language.RU -> "Инструмент"
        Language.UA -> "Інструмент"
        Language.EN -> "Instrument"
    }

    fun instrumentName(lang: Language, instrument: com.truetuna.tuner.model.Instrument) = when (instrument) {
        com.truetuna.tuner.model.Instrument.GUITAR_6 -> when (lang) {
            Language.RU -> "Гитара 6-струн"
            Language.UA -> "Гітара 6-струн"
            Language.EN -> "6-String Guitar"
        }
        com.truetuna.tuner.model.Instrument.BASS_4 -> when (lang) {
            Language.RU -> "Бас 4-струн"
            Language.UA -> "Бас 4-струн"
            Language.EN -> "4-String Bass"
        }
        com.truetuna.tuner.model.Instrument.BASS_5 -> when (lang) {
            Language.RU -> "Бас 5-струн"
            Language.UA -> "Бас 5-струн"
            Language.EN -> "5-String Bass"
        }
        com.truetuna.tuner.model.Instrument.UKULELE -> when (lang) {
            Language.RU -> "Укулеле (Сопрано)"
            Language.UA -> "Укулеле (Сопрано)"
            Language.EN -> "Ukulele (Soprano)"
        }
        com.truetuna.tuner.model.Instrument.UKULELE_TENOR -> when (lang) {
            Language.RU -> "Укулеле (Тенор)"
            Language.UA -> "Укулеле (Тенор)"
            Language.EN -> "Ukulele (Tenor)"
        }
        com.truetuna.tuner.model.Instrument.UKULELE_BARITONE -> when (lang) {
            Language.RU -> "Укулеле (Баритон)"
            Language.UA -> "Укулеле (Баритон)"
            Language.EN -> "Ukulele (Baritone)"
        }
        com.truetuna.tuner.model.Instrument.VIOLIN -> when (lang) {
            Language.RU -> "Скрипка"
            Language.UA -> "Скрипка"
            Language.EN -> "Violin"
        }
        com.truetuna.tuner.model.Instrument.CELLO -> when (lang) {
            Language.RU -> "Виолончель"
            Language.UA -> "Віолончель"
            Language.EN -> "Cello"
        }
    }

    // ── Capo ─────────────────────────────────────────────────────────────────

    fun capo(lang: Language) = when (lang) {
        Language.RU -> "Каподастр"
        Language.UA -> "Каподастр"
        Language.EN -> "Capo"
    }

    fun noCapo(lang: Language) = when (lang) {
        Language.RU -> "Без капо"
        Language.UA -> "Без капо"
        Language.EN -> "No Capo"
    }

    fun capoFret(lang: Language, fret: Int) = when (lang) {
        Language.RU -> "Капо $fret лад"
        Language.UA -> "Капо $fret лад"
        Language.EN -> "Capo $fret"
    }

    // ── Reference Tone ───────────────────────────────────────────────────────

    fun toneGenerator(lang: Language) = when (lang) {
        Language.RU -> "Эталонный тон"
        Language.UA -> "Еталонний тон"
        Language.EN -> "Tone Generator"
    }

    fun playTone(lang: Language) = when (lang) {
        Language.RU -> "Звук"
        Language.UA -> "Звук"
        Language.EN -> "Tone"
    }

    // ── Metronome ─────────────────────────────────────────────────────────────

    fun metronome(lang: Language) = when (lang) {
        Language.RU -> "Метроном"
        Language.UA -> "Метроном"
        Language.EN -> "Metronome"
    }

    fun bpm(lang: Language) = "BPM"

    fun beats(lang: Language) = when (lang) {
        Language.RU -> "Размер"
        Language.UA -> "Розмір"
        Language.EN -> "Beats"
    }

    fun sound(lang: Language) = when (lang) {
        Language.RU -> "Звук клика"
        Language.UA -> "Звук кліка"
        Language.EN -> "Click Sound"
    }

    // ── Display Mode ──────────────────────────────────────────────────────────

    fun displayMode(lang: Language) = when (lang) {
        Language.RU -> "Режим шкалы"
        Language.UA -> "Режим шкали"
        Language.EN -> "Display Style"
    }

    fun modeNeedle(lang: Language) = when (lang) {
        Language.RU -> "Стрелочный"
        Language.UA -> "Стрілочний"
        Language.EN -> "Needle"
    }

    fun modeStrobe(lang: Language) = when (lang) {
        Language.RU -> "Стробоскоп"
        Language.UA -> "Стробоскоп"
        Language.EN -> "Strobe"
    }

    // ── DSP Settings ──────────────────────────────────────────────────────────

    fun noiseGate(lang: Language) = when (lang) {
        Language.RU -> "Шумоподавление"
        Language.UA -> "Шумозаглушення"
        Language.EN -> "Noise Gate"
    }

    fun harmonicFilter(lang: Language) = when (lang) {
        Language.RU -> "Фильтр обертонов (бас)"
        Language.UA -> "Фільтр обертонів (бас)"
        Language.EN -> "Harmonic Low-Pass"
    }

    fun inTuneChime(lang: Language) = when (lang) {
        Language.RU -> "Звук при точной настройке"
        Language.UA -> "Звук при точному налаштуванні"
        Language.EN -> "In-Tune Chime"
    }
}
