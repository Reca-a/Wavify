package com.example.wavify

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.exoplayer.ExoPlayer
import com.example.wavify.databinding.ActivityAudioBinding
import androidx.media3.common.MediaItem
import androidx.core.net.toUri
import androidx.media3.common.Player

class AudioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAudioBinding
    private var player: ExoPlayer? = null
    private var uris: List<Uri> = emptyList()
    private var titles: List<String> = emptyList()
    private var artists: List<String> = emptyList()
    private var startIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        }

        binding.playerView.player = player

        player?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player?.currentMediaItemIndex ?: 0
                binding.songTitleText.text = titles[index]
                binding.songArtistText.text = artists.getOrNull(index) ?: "Nieznany artysta"
            }
        })
    }

    fun playPause(){
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    private fun nextSong() {
        player?.seekToNextMediaItem()
    }

    private fun previousSong() {
        player?.seekToPreviousMediaItem()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}