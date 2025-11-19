package com.example.wavify

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.view.Gravity
import android.view.Window
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavify.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var songs: List<AudioFile> = emptyList()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wczytanie ostatnio wybranego motywu
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("pref_theme_mode", "system") ?: "system"
        applyTheme(theme)

        // Usunięcie koloru górnego paska
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Prośba o uprawnienia
        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            100
        )

        // Animacja pojawiania się utworów podczas przewijania
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        window.enterTransition = Slide(Gravity.END).apply { duration = 300 }
        window.exitTransition = Fade().apply { duration = 300 }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pobranie utworów
        songs = MusicRepository.getLocalAudioFiles(this)

        if (songs.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_audio_files), Toast.LENGTH_SHORT).show()
            return
        }

        // Wczytanie utworów
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

    fun applyTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}