package pl.edu.ur.ar131498.wavify.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LyricsRepository {
    suspend fun fetchLyrics(artist: String, title: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (artist.isEmpty() || title.isEmpty() || artist == "<unknown>" || title == "<unknown>") {
                    return@withContext null
                }

                val encodedArtist = URLEncoder.encode(artist, "UTF-8")
                val encodedTitle = URLEncoder.encode(title, "UTF-8")
                val url = URL("https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val lyrics = json.optString("lyrics", "")
                    if (lyrics.isNotBlank()) lyrics else null
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
