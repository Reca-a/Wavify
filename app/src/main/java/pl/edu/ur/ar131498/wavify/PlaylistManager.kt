package pl.edu.ur.ar131498.wavify

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PlaylistManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wavify_playlists", Context.MODE_PRIVATE)
    private val PLAYLISTS_KEY = "playlists"
    private val DELIMITER = ";"

    fun createPlaylist(name: String): Boolean {
        val playlists = getPlaylists().toMutableSet()
        if (playlists.contains(name)) return false
        
        playlists.add(name)
        prefs.edit { putStringSet(PLAYLISTS_KEY, playlists) }
        return true
    }

    fun deletePlaylist(name: String) {
        val playlists = getPlaylists().toMutableSet()
        if (playlists.remove(name)) {
            prefs.edit {
                putStringSet(PLAYLISTS_KEY, playlists)
                    .remove("playlist_$name")
            }
        }
    }

    fun getPlaylists(): Set<String> {
        return prefs.getStringSet(PLAYLISTS_KEY, emptySet()) ?: emptySet()
    }

    fun addSongToPlaylist(playlistName: String, uri: String) {
        val currentSongs = getSongUris(playlistName).toMutableList()
        currentSongs.add(uri)
        saveSongs(playlistName, currentSongs)
    }

    fun removeSongFromPlaylist(playlistName: String, uri: String) {
        val currentSongs = getSongUris(playlistName).toMutableList()
        currentSongs.remove(uri)
        saveSongs(playlistName, currentSongs)
    }

    fun getSongUris(playlistName: String): List<String> {
        val raw = prefs.getString("playlist_$playlistName", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(DELIMITER).filter { it.isNotEmpty() }
    }

    fun isSongInPlaylist(playlistName: String, uri: String): Boolean {
        return getSongUris(playlistName).contains(uri)
    }

    private fun saveSongs(playlistName: String, songs: List<String>) {
        val raw = songs.joinToString(DELIMITER)
        prefs.edit { putString("playlist_$playlistName", raw) }
    }
}
