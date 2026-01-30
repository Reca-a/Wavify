package pl.edu.ur.ar131498.wavify.ui.bottomsheets

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
import pl.edu.ur.ar131498.wavify.R
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

    fun updateSong(newSong: AudioFile) {
        // Update the current song
        val field = SongDetailsBottomSheet::class.java.getDeclaredField("song")
        field.isAccessible = true
        field.set(this, newSong)
        
        // Update UI
        val detailsText = findViewById<TextView>(R.id.detailsText)
        if (detailsText != null) {
            val details = buildString {
                append("${context.getString(R.string.song_title)}: ${newSong.title}\n")
                append("${context.getString(R.string.song_artist)}: ${newSong.artist ?: context.getString(R.string.unknown_artist)}\n")
                append("${context.getString(R.string.file_path)}: ${newSong.uri.path}\n")
            }
            detailsText.text = details
        }

        // Reset lyrics state
        if (::lyricsText.isInitialized) {
            lyricsText.text = context.getString(R.string.lyrics_not_found)
            lyricsText.visibility = View.GONE
            searchButton.visibility = View.GONE
            progressBar.visibility = View.GONE
            
            // Reload if on lyrics tab
            val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
            if (tabLayout?.selectedTabPosition == 1) {
                loadLyrics()
            }
        }
    }

    private fun loadLyrics() {
        if (!::lyricsText.isInitialized) return
        if (lyricsText.text != context.getString(R.string.lyrics_not_found)) return

        progressBar.visibility = View.VISIBLE
        lyricsText.visibility = View.GONE
        searchButton.visibility = View.GONE

        // Access current song via reflection since it's private val and we can't easily change constructor
        // OR better, just rely on the fact we supposedly updated it via reflection above
        // But wait, 'song' is a private val in constructor. I can't easily change it to var without changing the primary constructor signature in the file which is fine but replace_file_content is block based. 
        // Actually, I can just use the field reflection trick or just assume I can pass it to loadLyrics if I change loadLyrics signature? 
        // Easier: I will just use reflection to update the backing field of 'song' as I did above.
        
        // However, instead of reflection, which is hacky, I will just change the class to use 'var' in a separate edit if needed, 
        // BUT 'song' is in the primary constructor. 
        // Let's just use the reflection approach for now as it minimizes changes to the file structure, or better yet, I should check if I can just pass 'song' to loadLyrics.
        // loadLyrics uses 'song' property.
        
        scope.launch {
            // Wyszukanie tekstu przez API
            // We need to access the updated song. 
            // Since I updated the field via reflection, 'this.song' should be the new song.
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
