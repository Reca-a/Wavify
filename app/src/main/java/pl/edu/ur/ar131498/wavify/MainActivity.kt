package pl.edu.ur.ar131498.wavify

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.view.Gravity
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import pl.edu.ur.ar131498.wavify.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import coil.load

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var songs: List<AudioFile> = emptyList()
    private var mediaController: MediaController? = null
    private lateinit var adapter : AudioAdapter

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val audioGranted =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
                } else {
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                }

            if (audioGranted) {
                reloadSongs()
            } else {
                showEmptyState()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wczytanie ostatnio wybranego motywu
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("pref_theme_mode", "system")!!
        applyTheme(theme)

        // Usunięcie koloru górnego paska
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Prośba o uprawnienia
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            add(Manifest.permission.CAMERA)
        }.toTypedArray()

        permissionLauncher.launch(permissions)

        // Animacja pojawiania się utworów podczas przewijania
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        window.enterTransition = Slide(Gravity.END).apply { duration = 300 }
        window.exitTransition = Fade().apply { duration = 300 }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        findViewById<MaterialButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Pobranie utworów
        songs = MusicRepository.getLocalAudioFiles(this)

        if (songs.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }

        // Wczytanie utworów
        adapter = AudioAdapter { list, position ->
            openAudioActivity(list, position)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        adapter.submitList(songs)

        binding.bottomPlayer.root.setOnClickListener {
            startActivity(
                Intent(this, AudioActivity::class.java).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
        }

        binding.bottomPlayer.playPauseButton.setOnClickListener {
            mediaController?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val sessionToken = SessionToken(
            this,
            ComponentName(this, MusicService::class.java)
        )

        val controllerFuture =
            MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            connectBottomPlayer()
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        mediaController?.release()
        mediaController = null
        super.onStop()
    }

    private fun connectBottomPlayer() {
        val controller = mediaController ?: return

        controller.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {
                updateBottomPlayer()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateBottomPlayer()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateBottomPlayer()
            }
        })

        updateBottomPlayer()
    }

    private fun updateBottomPlayer() {
        val controller = mediaController ?: return

        val hasMedia = controller.mediaItemCount > 0

        binding.bottomPlayer.root.visibility =
            if (hasMedia) View.VISIBLE else View.GONE

        if (!hasMedia) return

        val mediaItem = controller.currentMediaItem ?: return
        val metadata = mediaItem.mediaMetadata

        binding.bottomPlayer.songTitleBottom.text =
            metadata.title ?: getString(R.string.unknown_title)

        binding.bottomPlayer.songArtistText.text =
            metadata.artist ?: getString(R.string.unknown_artist)

        binding.bottomPlayer.albumArtBottom.load(metadata.artworkUri) {
            placeholder(R.drawable.default_album_art)
            error(R.drawable.default_album_art)
        }

        binding.bottomPlayer.playPauseButton.setIconResource(
            if (controller.isPlaying)
                R.drawable.ic_pause_32
            else
                R.drawable.ic_play_32
        )
    }

    private fun openAudioActivity(list: List<AudioFile>, position: Int) {
        val intent = Intent(this, AudioActivity::class.java).apply {
            putExtra("START_INDEX", position)
            putStringArrayListExtra(
                "SONG_URIS",
                ArrayList(list.map { it.uri.toString() })
            )
            putStringArrayListExtra(
                "SONG_TITLES",
                ArrayList(list.map { it.title })
            )
            putStringArrayListExtra(
                "SONG_ARTISTS",
                ArrayList(list.map { it.artist ?: getString(R.string.unknown_artist) })
            )
            putStringArrayListExtra(
                "ALBUM_ART_URIS",
                ArrayList(list.map { it.albumArtUri?.toString() ?: "" })
            )
        }
        startActivity(intent)
    }

    private fun reloadSongs() {
        songs = MusicRepository.getLocalAudioFiles(this)

        if (songs.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
            adapter.submitList(songs)
        }
    }

    private fun showEmptyState() {
        binding.recyclerView.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        binding.recyclerView.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
    }

    private fun applyTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}