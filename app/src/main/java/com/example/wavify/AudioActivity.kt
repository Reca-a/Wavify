package com.example.wavify

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.media3.exoplayer.ExoPlayer
import com.example.wavify.databinding.ActivityAudioBinding
import androidx.media3.common.MediaItem
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import coil.load
import com.google.android.material.slider.Slider
import androidx.core.net.toUri
import com.google.android.material.button.MaterialButton

class AudioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAudioBinding
    private var player: ExoPlayer? = null
    private var songs: List<AudioFile> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private var startIndex: Int = 0
    private var isUserSeeking = false
    private var isShuffleEnabled = false
    private var repeatMode = Player.REPEAT_MODE_OFF

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriStrings = intent.getStringArrayListExtra("SONG_URIS") ?: arrayListOf()
        val titles = intent.getStringArrayListExtra("SONG_TITLES") ?: arrayListOf()
        val artists = intent.getStringArrayListExtra("SONG_ARTISTS") ?: arrayListOf()
        val albumArtStrings = intent.getStringArrayListExtra("ALBUM_ART_URIS") ?: arrayListOf()
        startIndex = intent.getIntExtra("START_INDEX", 0)

        // Rekonstrukcja obiektów AudioFile
        songs = uriStrings.indices.map { i ->
            AudioFile(
                uri = uriStrings[i].toUri(),
                title = titles.getOrNull(i) ?: getString(R.string.unknown_title),
                artist = artists.getOrNull(i),
                albumArtUri = albumArtStrings.getOrNull(i)?.let {
                    if (it.isNotEmpty()) it.toUri() else null
                }
            )
        }

        // Inicjalizacja ExoPlayera
        player = ExoPlayer.Builder(this).build().apply {
            val items = songs.map { MediaItem.fromUri(it.uri) }
            setMediaItems(items, startIndex, 0)
            prepare()
            play()

            binding.playPauseButton.setOnClickListener { playPause() }
            binding.nextButton.setOnClickListener { nextSong() }
            binding.prevButton.setOnClickListener { previousSong() }

            // Ustawienie początkowych danych
            updateSongInfo(startIndex)

            binding.shuffleButton.setOnClickListener { toggleShuffle() }
            binding.repeatButton.setOnClickListener { toggleRepeat() }
        }

        setupTimeBar()
        startProgressUpdate()

        // Opóźnione przewijanie długiego tytułu utworu
        binding.songTitleText.postDelayed({
            binding.songTitleText.isSelected = true
        }, 3000)

        findViewById<MaterialButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        player?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player?.currentMediaItemIndex ?: 0
                updateSongInfo(index)
            }
        })
    }

    // Funkcja do aktualizacji informacji o utworze
    private fun updateSongInfo(index: Int) {
        if (index in songs.indices) {
            val song = songs[index]
            binding.songTitleText.text = song.title
            if (song.artist != null && song.artist != "<unknown>") {
                binding.songArtistText.text = song.artist
            } else {
                binding.songArtistText.text = getString(R.string.unknown_artist)
            }

            binding.albumArtImageView.load(song.albumArtUri) {
                placeholder(R.drawable.default_album_art)
                error(R.drawable.default_album_art)
                crossfade(true)
            }

        }
    }

    private fun playPause(){
        player?.let {
            if (it.isPlaying) {
                it.pause()
                binding.playPauseButton.setIconResource(R.drawable.ic_play_32)
            } else {
                it.play()
                binding.playPauseButton.setIconResource(R.drawable.ic_pause_32)
            }
        }
    }

    private fun nextSong() {
        player?.seekToNextMediaItem()
    }

    private fun previousSong() {
        player?.seekToPreviousMediaItem()
    }

    // Funkcja wczytująca slidebar z czasem utworu
    private fun setupTimeBar() {
        binding.timeBar.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                isUserSeeking = true
                binding.currentTimeText.text = formatTime(value.toLong())
            }
        }

        // Listenery do zatrzymywania aktualizacji funkcji startProgressUpdate() podczas interakcji użytkownika
        binding.timeBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                player?.seekTo(slider.value.toLong())
                isUserSeeking = false
            }
        })
    }

    // Funkcja aktualizująca upłynięty czas utworu
    private fun startProgressUpdate() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isUserSeeking) {
                    val currentPos = player?.currentPosition
                    val duration = player?.duration

                    if (duration != null && duration > 0) {
                        binding.timeBar.valueTo = duration.toFloat()
                        if (currentPos != null) {
                            binding.timeBar.value = currentPos.toFloat()
                        } else {
                            binding.timeBar.value = 0F
                        }
                        if (currentPos != null ) {
                            binding.currentTimeText.text = formatTime(currentPos)
                        } else {
                            binding.currentTimeText.text = "00:00"
                        }
                        binding.totalTimeText.text = formatTime(duration)
                    }
                }
                handler.postDelayed(this, 100)
            }
        })
    }

    // Przełączanie losowej kolejności utworów
    private fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        player?.shuffleModeEnabled = isShuffleEnabled

        binding.shuffleButton.iconTint = ColorStateList.valueOf(
            getThemeColor(
                if (isShuffleEnabled) com.google.android.material.R.attr.colorOnSecondary
                else com.google.android.material.R.attr.colorOnPrimaryContainer
            )
        )
    }

    // Przełączanie powtarzania utworów
    private fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> {
                binding.repeatButton.icon = AppCompatResources.getDrawable(this, R.drawable.ic_repeat_one_32)
                Player.REPEAT_MODE_ONE
            }
            Player.REPEAT_MODE_ONE -> {
                binding.repeatButton.icon = AppCompatResources.getDrawable(this, R.drawable.ic_repeat_32)
                Player.REPEAT_MODE_ALL
            }
            else -> {
                Player.REPEAT_MODE_OFF
            }
        }

        binding.repeatButton.iconTint = ColorStateList.valueOf(
            getThemeColor(
                if (repeatMode != Player.REPEAT_MODE_OFF) com.google.android.material.R.attr.colorOnSecondary
                else com.google.android.material.R.attr.colorOnPrimaryContainer
            )
        )

        player?.repeatMode = repeatMode
    }

    // Funkcja pomocnicza dla toggleRepeat() oraz toggleShuffle()
    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    // Funkcja pomocnicza do formatowania wyświetlanego czasu
    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60

        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
        handler.removeCallbacksAndMessages(null)
    }
}