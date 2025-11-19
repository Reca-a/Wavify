package com.example.wavify

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.view.Gravity
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavify.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var songs: List<AudioFile> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Animacja pojawiania się utworów podczas przewijania
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        window.enterTransition = Slide(Gravity.END).apply { duration = 300 }
        window.exitTransition = Fade().apply { duration = 300 }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        songs = MusicRepository.getLocalAudioFiles(this)

        if (songs.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_audio_files), Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = AudioAdapter(songs) { position ->
            val intent = Intent(this, AudioActivity::class.java).apply {
                putExtra("START_INDEX", position)
                putStringArrayListExtra("SONG_URIS", ArrayList(songs.map { it.uri.toString() }))
                putStringArrayListExtra("SONG_TITLES", ArrayList(songs.map { it.title }))
                putStringArrayListExtra("SONG_ARTISTS", ArrayList(songs.map { it.artist ?: getString(R.string.unknown_artist) }))
                putStringArrayListExtra("ALBUM_ART_URIS", ArrayList(songs.map { it.albumArtUri?.toString() ?: "" }))
            }
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        findViewById<MaterialButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}