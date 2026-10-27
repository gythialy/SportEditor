package io.github.sporteditor.ui

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import io.github.sporteditor.R
import java.util.Locale

/** Languages selectable inside the app. Chinese is the default. */
enum class AppLanguage(val tag: String, @StringRes val labelRes: Int) {
    CHINESE("zh-CN", R.string.language_chinese),
    ENGLISH("en", R.string.language_english)
    ;

    companion object {
        val DEFAULT = CHINESE

        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: DEFAULT
    }
}

/**
 * Persists the UI language and applies it to a Context.
 *
 * The preference is read in [MainActivity.attachBaseContext] so resources are resolved with the
 * selected locale before the Compose tree is created.
 */
object AppLanguageStore {
    internal const val PREFERENCES_NAME = "sporteditor_ui"
    private const val KEY_LANGUAGE = "language"

    fun load(context: Context): AppLanguage = AppLanguage.fromTag(preferences(context).getString(KEY_LANGUAGE, null))

    fun save(context: Context, language: AppLanguage) {
        preferences(context).edit().putString(KEY_LANGUAGE, language.tag).apply()
    }

    fun wrap(context: Context): Context {
        val language = load(context)
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    private fun preferences(context: Context) = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
