package com.waypoint.app

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The language Waypoint displays and speaks. It follows Android's per-app language
 * (Settings › Apps › Waypoint › Language) or, when none is chosen, the phone language.
 *
 * Text that lives in Kotlin rather than in string resources goes through [tr], so the
 * same code paths stay testable on the JVM, where the default locale is English.
 */
internal object AppLanguage {
    /** Replaced by the application with its live resource configuration. */
    @Volatile var locale: () -> Locale = { Locale.getDefault() }

    val isHebrew: Boolean get() = locale().language.let { it == "iw" || it == "he" }

    /** BCP 47 tag for services that accept one, such as text-to-speech. */
    val speechLocale: Locale get() = if (isHebrew) Locale.forLanguageTag("he-IL") else Locale.ENGLISH

    fun install(context: Context) {
        val app = context.applicationContext
        locale = { app.resources.configuration.locales.get(0) ?: Locale.getDefault() }
    }

    /** Explicit in-app choice; null follows the phone. Android 13+ stores it per app. */
    fun current(context: Context): Choice {
        if (Build.VERSION.SDK_INT < 33) return Choice.SYSTEM
        val tags = context.getSystemService(LocaleManager::class.java)?.applicationLocales ?: return Choice.SYSTEM
        if (tags.isEmpty) return Choice.SYSTEM
        return when (tags.get(0)?.language) { "iw", "he" -> Choice.HEBREW; "en" -> Choice.ENGLISH; else -> Choice.SYSTEM }
    }

    /** Returns false where Android cannot change the app language on its own. */
    fun choose(context: Context, choice: Choice): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        val manager = context.getSystemService(LocaleManager::class.java) ?: return false
        manager.applicationLocales = when (choice) {
            Choice.SYSTEM -> LocaleList.getEmptyLocaleList()
            Choice.ENGLISH -> LocaleList.forLanguageTags("en")
            Choice.HEBREW -> LocaleList.forLanguageTags("iw")
        }
        return true
    }

    val canChooseInApp: Boolean get() = Build.VERSION.SDK_INT >= 33

    enum class Choice { SYSTEM, ENGLISH, HEBREW }
}

/** English text with its Hebrew translation, chosen by the current app language. */
internal fun tr(english: String, hebrew: String): String = if (AppLanguage.isHebrew) rightToLeft(hebrew) else english

/** Hebrew count wording: singular and dual forms read naturally, larger numbers use the plural. */
internal fun trCount(count: Int, one: String, other: String, heOne: String, heTwo: String, heOther: String): String =
    if (AppLanguage.isHebrew) rightToLeft(when (count) { 1 -> heOne; 2 -> heTwo; else -> heOther })
    else if (count == 1) one else other

/**
 * A Hebrew sentence that opens with a Latin word ("Waypoint …", "GPS …") would otherwise be laid
 * out left to right, because Android picks a paragraph's direction from its first strong letter.
 * An invisible right-to-left mark keeps it a right-to-left paragraph. Speech engines skip the mark.
 */
internal fun rightToLeft(text: String): String {
    for (c in text) {
        when (Character.getDirectionality(c)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return text
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return "\u200F$text"
        }
    }
    return text
}
