package com.example.wavify

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.example.wavify.databinding.ActivityAudioBinding
import androidx.media3.common.MediaItem
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import coil.load
import com.google.android.material.slider.Slider
import androidx.core.net.toUri
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class AudioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAudioBinding
    private lateinit var controller: MediaController
    private lateinit var controllerFuture: ListenableFuture<MediaController>
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

        // Uruchomienie serwisu
        val serviceIntent = Intent(this, MusicService::class.java)
        startService(serviceIntent)

        // Połączenie z MediaController
        val sessionToken = SessionToken(
            this,
            ComponentName(this, MusicService::class.java)
        )

        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                controller = controllerFuture.get()
                initializePlayer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())

        setupTimeBar()
        startProgressUpdate()

        // Opóźnione przewijanie długiego tytułu utworu
        binding.songTitleText.postDelayed({
            binding.songTitleText.isSelected = true
        }, 3000)

        findViewById<MaterialButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun initializePlayer() {
        // Tworzenie listy utworów
        val items = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(
                            if (!song.artist.isNullOrEmpty() && song.artist != "<unknown>")
                                song.artist
                            else
                                R.string.unknown_artist.toString()
                        )
                        .setArtworkUri(
                            song.albumArtUri ?: "@drawable/default_album_art.jpg".toUri()           // TODO Do naprawy lub usunięcia
                        )
                        .build()
                )
                .build()
        }

        controller.setMediaItems(items, startIndex, 0)
        controller.prepare()
        controller.play()

        // Listenery przycisków odtwarzacza
        binding.playPauseButton.setOnClickListener { playPause() }
        binding.nextButton.setOnClickListener { nextSong() }
        binding.prevButton.setOnClickListener { previousSong() }
        binding.shuffleButton.setOnClickListener { toggleShuffle() }
        binding.repeatButton.setOnClickListener { toggleRepeat() }

        // Ustawienie początkowych danych
        updateSongInfo(startIndex)

        // Listener dla odtwarzacza
        controller.addListener(object : Player.Listener {
            // Zmiana utworu
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = controller.currentMediaItemIndex
                updateSongInfo(index)
            }

            // Pauza / odtworzenie
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                binding.playPauseButton.setIconResource(
                    if (isPlaying) R.drawable.ic_pause_32
                    else R.drawable.ic_play_32
                )
            }
        })
    }

    // Funkcja do aktualizacji informacji o utworze
    private fun updateSongInfo(index: Int) {
        if (index in songs.indices) {
            val song = songs[index]
            binding.songTitleText.text = song.title
            if (!song.artist.isNullOrEmpty() && song.artist != "<unknown>") {
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

    private fun playPause() {
        if (!::controller.isInitialized) return

        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    private fun nextSong() {
        if (!::controller.isInitialized) return
        controller.seekToNextMediaItem()
    }

    private fun previousSong() {
        if (!::controller.isInitialized) return
        controller.seekToPreviousMediaItem()
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
                if (::controller.isInitialized) {
                    controller.seekTo(slider.value.toLong())
                }
                isUserSeeking = false
            }
        })
    }

    // Funkcja aktualizująca upłynięty czas utworu
    private fun startProgressUpdate() {
        handler.post(object : Runnable {
            override fun run() {
                if (::controller.isInitialized && !isUserSeeking) {
                    val currentPos = controller.currentPosition
                    val duration = controller.duration

                    if (duration > 0) {
                        binding.timeBar.valueTo = duration.toFloat()
                        binding.timeBar.value = currentPos.toFloat()
                        binding.currentTimeText.text = formatTime(currentPos)
                        binding.totalTimeText.text = formatTime(duration)
                    }
                }
                handler.postDelayed(this, 100)
            }
        })
    }

    // Przełączanie losowej kolejności utworów
    private fun toggleShuffle() {
        if (!::controller.isInitialized) return

        isShuffleEnabled = !isShuffleEnabled
        controller.shuffleModeEnabled = isShuffleEnabled

        binding.shuffleButton.iconTint = ColorStateList.valueOf(
            getThemeColor(
                if (isShuffleEnabled) com.google.android.material.R.attr.colorOnSecondary
                else com.google.android.material.R.attr.colorOnPrimaryContainer
            )
        )
    }

    // Przełączanie powtarzania utworów
    private fun toggleRepeat() {
        if (!::controller.isInitialized) return

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

        controller.repeatMode = repeatMode
    }

    // Funkcja pomocnicza dla toggleRepeat() oraz toggleShuffle()
    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    // Funkcja pomocnicza do formatowania wyświetlanego czasu
    @SuppressLint("DefaultLocale")
    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60

        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::controllerFuture.isInitialized) {
            MediaController.releaseFuture(controllerFuture)
        }
        handler.removeCallbacksAndMessages(null)
    }
}