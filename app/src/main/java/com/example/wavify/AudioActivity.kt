package com.example.wavify

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.media3.exoplayer.ExoPlayer
import com.example.wavify.databinding.ActivityAudioBinding
import androidx.media3.common.MediaItem
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import com.google.android.material.slider.Slider

class AudioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAudioBinding
    private var player: ExoPlayer? = null
    private var uris: List<Uri> = emptyList()
    private var titles: List<String> = emptyList()
    private var artists: List<String> = emptyList()
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

        // Odbieranie danych
        val uriStrings = intent.getStringArrayListExtra("SONG_URIS") ?: arrayListOf()
        uris = uriStrings.map { it.toUri() }
        titles = intent.getStringArrayListExtra("SONG_TITLES") ?: arrayListOf()
        artists = intent.getStringArrayListExtra("SONG_ARTISTS") ?: arrayListOf()
        startIndex = intent.getIntExtra("START_INDEX", 0)

        // Inicjalizacja ExoPlayera
        player = ExoPlayer.Builder(this).build().apply {
            val items = uris.map { MediaItem.fromUri(it) }
            setMediaItems(items, startIndex, 0)
            prepare()
            play()

            binding.playPauseButton.setOnClickListener { playPause() }
            binding.nextButton.setOnClickListener { nextSong() }
            binding.prevButton.setOnClickListener { previousSong() }
            binding.songTitleText.text = titles[startIndex]
            binding.songArtistText.text = artists[startIndex]
            binding.shuffleButton.setOnClickListener { toggleShuffle() }
            binding.repeatButton.setOnClickListener { toggleRepeat() }
        }

        binding.playerView.player = player

        setupTimeBar()
        startProgressUpdate()
        
        player?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player?.currentMediaItemIndex ?: 0
                binding.songTitleText.text = titles[index]
                binding.songArtistText.text = artists.getOrNull(index) ?: "Nieznany artysta"
            }
        })
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
                else com.google.android.material.R.attr.colorOnSurfaceVariant
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
                else com.google.android.material.R.attr.colorOnSurfaceVariant
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