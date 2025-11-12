package com.example.wavify

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavify.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var songs: List<AudioFile> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        songs = MusicRepository.getLocalAudioFiles(this)

        if (songs.isEmpty()) {
            Toast.makeText(this, "Brak plików audio", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = AudioAdapter(songs) { position ->
            val intent = Intent(this, AudioActivity::class.java).apply {
                putExtra("START_INDEX", position)
                putStringArrayListExtra("SONG_URIS", ArrayList(songs.map { it.uri.toString() }))
                putStringArrayListExtra("SONG_TITLES", ArrayList(songs.map { it.title }))
                putStringArrayListExtra("SONG_ARTISTS", ArrayList(songs.map { it.artist ?: "Nieznany artysta" }))
            }
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
}