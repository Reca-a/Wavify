package pl.edu.ur.ar131498.wavify

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.edu.ur.ar131498.wavify.data.LyricsRepository
import androidx.core.net.toUri
import pl.edu.ur.ar131498.wavify.data.AudioFile

class SongDetailsBottomSheet(
    context: Context,
    private val scope: CoroutineScope,
    private val song: AudioFile
) : BottomSheetDialog(context) {

    private val repository = LyricsRepository()
    private lateinit var lyricsText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchButton: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bottom_sheet_song_details)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)!!
        val detailsView = findViewById<View>(R.id.detailsView)!!
        val lyricsView = findViewById<View>(R.id.lyricsView)!!

        lyricsText = findViewById(R.id.lyricsText)!!
        progressBar = findViewById(R.id.lyricsProgressBar)!!
        searchButton = findViewById(R.id.searchOnlineButton)!!

        val detailsText = findViewById<TextView>(R.id.detailsText)!!

        val details = buildString {
            append("${context.getString(R.string.song_title)}: ${song.title}\n")
            append("${context.getString(R.string.song_artist)}: ${song.artist ?: context.getString(R.string.unknown_artist)}\n")
            append("${context.getString(R.string.file_path)}: ${song.uri.path}\n")
        }
        detailsText.text = details

        // Przełączanie zakładek
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when(tab?.position) {
                    0 -> {
                        detailsView.visibility = View.VISIBLE
                        lyricsView.visibility = View.GONE
                    }
                    1 -> {
                        detailsView.visibility = View.GONE
                        lyricsView.visibility = View.VISIBLE
                        loadLyrics()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Przycisk "Szukaj w przeglądarce"
        searchButton.setOnClickListener {
            val query = "${song.artist} ${song.title} lyrics"
            val intent = Intent(Intent.ACTION_VIEW, "https://www.google.com/search?q=$query".toUri())
            context.startActivity(intent)
        }
    }

    private fun loadLyrics() {
        if (lyricsText.text != context.getString(R.string.lyrics_not_found)) return

        progressBar.visibility = View.VISIBLE
        lyricsText.visibility = View.GONE
        searchButton.visibility = View.GONE

        scope.launch {
            // Wyszukanie tekstu przez API
            val lyrics = repository.fetchLyrics(song.artist ?: "", song.title)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                lyricsText.visibility = View.VISIBLE

                if (lyrics != null) {
                    lyricsText.text = lyrics
                } else {
                    lyricsText.text = context.getString(R.string.lyrics_not_found)
                    searchButton.visibility = View.VISIBLE
                }
            }
        }
    }
}
