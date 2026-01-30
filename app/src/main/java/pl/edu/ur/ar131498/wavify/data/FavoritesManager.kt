package pl.edu.ur.ar131498.wavify.data

import android.content.Context
import android.content.SharedPreferences

class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wavify_favorites", Context.MODE_PRIVATE)

    fun addFavorite(uri: String) {
        prefs.edit().putBoolean(uri, true).apply()
    }

    fun removeFavorite(uri: String) {
        prefs.edit().remove(uri).apply()
    }

    fun isFavorite(uri: String): Boolean {
        return prefs.contains(uri)
    }

    fun getFavorites(): Set<String> {
        return prefs.all.keys
    }
}
