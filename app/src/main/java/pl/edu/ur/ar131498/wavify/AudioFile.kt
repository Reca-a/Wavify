package pl.edu.ur.ar131498.wavify

import android.net.Uri
import java.io.Serializable

// Klasa przechowująca dane o utworze
data class AudioFile(
    val uri: Uri,
    val title: String,
    val artist: String? = null,
    val albumArtUri: Uri? = null,
    val dateAdded: Long = 0
) : Serializable