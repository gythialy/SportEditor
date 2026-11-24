package io.github.sporteditor.ui

import android.content.Context
import androidx.annotation.StringRes
import io.github.sporteditor.R

/** User selected UI theme; [SYSTEM] follows the device dark mode. */
enum class ThemeMode(val storageKey: String, @StringRes val labelRes: Int) {
    SYSTEM("system", R.string.theme_system),
    LIGHT("light", R.string.theme_light),
    DARK("dark", R.string.theme_dark)
    ;

    companion object {
        val DEFAULT = SYSTEM

        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}

/**
 * Persists the theme choice next to the language preference.
 *
 * The value is read before the Compose tree is created so a forced light/dark theme is applied
 * without a visible flash of the system theme.
 */
object ThemeModeStore {
    private const val KEY_THEME = "theme_mode"

    fun load(context: Context): ThemeMode = ThemeMode.fromKey(preferences(context).getString(KEY_THEME, null))

    fun save(context: Context, mode: ThemeMode) {
        preferences(context).edit().putString(KEY_THEME, mode.storageKey).apply()
    }

    private fun preferences(context: Context) = context.getSharedPreferences(AppLanguageStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
}

/**
 * Persists whether the Android 12+ dynamic ("Monet") palette is used.
 *
 * Defaults to on, which is the behaviour the app had before the toggle existed. The value is
 * meaningless below API 31: there is no system palette to consume, so the theme falls back to the
 * brand scheme regardless.
 */
object DynamicColorStore {
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"

    fun load(context: Context): Boolean = preferences(context).getBoolean(KEY_DYNAMIC_COLOR, true)

    fun save(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    private fun preferences(context: Context) = context.getSharedPreferences(AppLanguageStore.PREFERENCES_NAME, Context.MODE_PRIVATE)
}
