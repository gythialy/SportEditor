package io.github.gythialy.sporteditor.ui

import android.content.Context
import androidx.annotation.StringRes
import io.github.gythialy.sporteditor.R

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
