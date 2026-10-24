package py.edu.ctn.sca.padres.core

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("Igual que el celular"),
    LIGHT("Claro"),
    DARK("Oscuro"),
}

/** Preferencia de tema. Va en prefs comunes (no en TokenStore) para que no se borre al cerrar sesión. */
class ThemePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("sca_ui_prefs", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY, null).orEmpty()) }.getOrDefault(ThemeMode.SYSTEM),
    )
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(value: ThemeMode) {
        prefs.edit().putString(KEY, value.name).apply()
        _mode.value = value
    }

    private companion object {
        const val KEY = "theme_mode"
    }
}
