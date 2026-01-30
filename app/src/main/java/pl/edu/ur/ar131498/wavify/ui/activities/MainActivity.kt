package pl.edu.ur.ar131498.wavify.ui.activities

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.view.Gravity
import android.view.View
import android.view.Window
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import androidx.appcompat.widget.SearchView
import androidx.activity.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import coil.load
import com.google.android.material.button.MaterialButton
import pl.edu.ur.ar131498.wavify.ui.fragments.AuthorsFragment
import pl.edu.ur.ar131498.wavify.ui.fragments.FavoritesFragment
import pl.edu.ur.ar131498.wavify.ui.viewmodel.MainViewModel
import pl.edu.ur.ar131498.wavify.ui.fragments.PlaylistsFragment
import pl.edu.ur.ar131498.wavify.R
import pl.edu.ur.ar131498.wavify.ui.viewmodel.SortOrder
import pl.edu.ur.ar131498.wavify.ui.fragments.TracksFragment
import pl.edu.ur.ar131498.wavify.data.AudioFile
import pl.edu.ur.ar131498.wavify.data.MusicRepository
import pl.edu.ur.ar131498.wavify.databinding.ActivityMainBinding
import pl.edu.ur.ar131498.wavify.service.MusicService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private var mediaController: MediaController? = null

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
                // Ewentualna logika
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wczytanie ostatnio wybranego motywu
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("pref_theme_mode", "system")!!
        applyTheme(theme)
        
        // Pierwsze uruchomienie
        if (prefs.getBoolean("is_first_run", true)) {
            showOnboardingDialog()
        }

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

        setupSearch()
        setupPager()

        // Pobranie utworów
        reloadSongs()

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
    
    private fun showOnboardingDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.welcome_title))
            .setMessage(getString(R.string.onboarding_message))
            .setPositiveButton(getString(R.string.understand)) { _, _ ->
                PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean("is_first_run", false)
                    .apply()
            }
            .setCancelable(false)
            .show()
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

    fun openAudioActivity(list: List<AudioFile>, position: Int) {
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

    private fun setupSearch() {
        findViewById<android.widget.ImageButton>(R.id.sortButton).setOnClickListener {
            showSortDialog()
        }
        
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.updateSearchQuery(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != viewModel.searchQuery.value) {
                    viewModel.updateSearchQuery(newText ?: "")
                }
                return true
            }
        })

        viewModel.searchQuery.observe(this) { query ->
            if (binding.searchView.query.toString() != query) {
                binding.searchView.setQuery(query, false)
            }
        }
    }

    fun switchToTracksTab() {
        binding.viewPager.currentItem = 0
    }

    private fun setupPager() {
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when(position) {
                0 -> getString(R.string.songs)
                1 -> getString(R.string.authors)
                2 -> getString(R.string.playlists)
                3 -> getString(R.string.favorite)
                else -> ""
            }
        }.attach()
    }

    private fun reloadSongs() {
        lifecycleScope.launch {
             val songs = MusicRepository.getLocalAudioFiles(this@MainActivity)
             viewModel.loadSongs(songs)
        }
    }
    
    
    private inner class ScreenSlidePagerAdapter(fa: AppCompatActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when(position) {
                0 -> TracksFragment()
                1 -> AuthorsFragment()
                2 -> PlaylistsFragment()
                3 -> FavoritesFragment()
                else -> TracksFragment()
            }
        }
    }
    
    fun showSortDialog() {
        val currentTab = binding.viewPager.currentItem
        
        val options = if (currentTab == 2) { // Playlists tab
             arrayOf(
                getString(R.string.title_az),
                getString(R.string.title_za)
            )
        } else {
             arrayOf(
                getString(R.string.title_az),
                getString(R.string.title_za),
                getString(R.string.author_az),
                getString(R.string.author_za),
                getString(R.string.date_newest),
                getString(R.string.date_oldest)
            )
        }

        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.sort))
            .setItems(options) { _, which ->
                // Map the index based on which list was shown
                val order = if (currentTab == 2) {
                     when(which) {
                         0 -> SortOrder.TITLE_ASC
                         1 -> SortOrder.TITLE_DESC
                         else -> SortOrder.TITLE_ASC
                     }
                } else {
                     when (which) {
                        0 -> SortOrder.TITLE_ASC
                        1 -> SortOrder.TITLE_DESC
                        2 -> SortOrder.ARTIST_ASC
                        3 -> SortOrder.ARTIST_DESC
                        4 -> SortOrder.DATE_DESC
                        else -> SortOrder.DATE_ASC
                    }
                }
                viewModel.setSortOrder(order)
            }
            .show()
    }

    private fun applyTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}