package pl.edu.ur.ar131498.wavify.ui.activities

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import pl.edu.ur.ar131498.wavify.ui.adapters.AudioAdapter
import pl.edu.ur.ar131498.wavify.R
import pl.edu.ur.ar131498.wavify.ui.bottomsheets.SongMenuBottomSheet
import pl.edu.ur.ar131498.wavify.data.AudioFile
import pl.edu.ur.ar131498.wavify.data.MusicRepository
import pl.edu.ur.ar131498.wavify.data.PlaylistManager
import pl.edu.ur.ar131498.wavify.databinding.ActivityPlaylistDetailBinding
import pl.edu.ur.ar131498.wavify.service.MusicService

class PlaylistDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistDetailBinding
    private lateinit var playlistManager: PlaylistManager
    private lateinit var adapter: AudioAdapter
    private var currentSongs: List<AudioFile> = emptyList()
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val playlistName = intent.getStringExtra("PLAYLIST_NAME") ?: return finish()
        binding.playlistTitle.text = playlistName

        playlistManager = PlaylistManager(this)

        adapter = AudioAdapter(
            onItemClick = { _, position ->
                openAudioActivity(currentSongs, position)
            },
            onItemLongClick = { song ->
                SongMenuBottomSheet(
                    this,
                    song,
                    currentPlaylistName = playlistName,
                    onPlaylistChanged = {
                        loadSongs(playlistName)
                    },
                    onFavoriteChanged = {}
                ).show()
            }
        )

        binding.songsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.songsRecyclerView.adapter = adapter

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.playFab.setOnClickListener {
            if (currentSongs.isNotEmpty()) {
                openAudioActivity(currentSongs, 0)
            } else {
                Toast.makeText(this, R.string.empty_playlist, Toast.LENGTH_SHORT).show()
            }
        }

        binding.header.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Obsługa bottom playera
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

        loadSongs(playlistName)
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

    private fun loadSongs(playlistName: String) {
        val allSongs = MusicRepository.getLocalAudioFiles(this)
        val playlistUris = playlistManager.getSongUris(playlistName)

        val songMap = allSongs.associateBy { it.uri.toString() }
        val orderedSongs = playlistUris.mapNotNull { songMap[it] }

        currentSongs = orderedSongs
        adapter.submitList(currentSongs)

        val headerCovers = orderedSongs.mapNotNull { it.albumArtUri }.take(4)

        val singleCover = binding.singleCover
        val gridContainer = binding.gridContainer

        if (headerCovers.isEmpty()) {
            singleCover.visibility = View.VISIBLE
            gridContainer.visibility = View.GONE
            singleCover.setImageResource(R.drawable.ic_playlist_play_48)
            singleCover.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            singleCover.setPadding(80, 80, 80, 80)

            val typedValue = android.util.TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimaryContainer, typedValue, true)
            singleCover.imageTintList = android.content.res.ColorStateList.valueOf(typedValue.data)

        } else if (headerCovers.size == 1) {
            singleCover.visibility = View.VISIBLE
            gridContainer.visibility = View.GONE
            singleCover.setPadding(0,0,0,0)
            singleCover.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP

            singleCover.imageTintList = null

            singleCover.load(headerCovers[0]) {
                placeholder(R.drawable.ic_playlist_play_48)
                error(R.drawable.ic_playlist_play_48)
            }
        } else {
            singleCover.visibility = View.GONE
            gridContainer.visibility = View.VISIBLE

            val collageRow1 = binding.collageRow1
            val collageRow2 = binding.collageRow2

            if (headerCovers.size == 2) {
                collageRow1.visibility = View.VISIBLE
                collageRow2.visibility = View.GONE

                val params = collageRow1.layoutParams as android.widget.LinearLayout.LayoutParams
                params.weight = 0f
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                collageRow1.layoutParams = params

                binding.cover1.load(headerCovers[0]) { placeholder(android.R.color.transparent) }
                binding.cover2.load(headerCovers[1]) { placeholder(android.R.color.transparent) }

            } else {
                collageRow1.visibility = View.VISIBLE
                collageRow2.visibility = View.VISIBLE

                val params1 = collageRow1.layoutParams as android.widget.LinearLayout.LayoutParams
                params1.weight = 1f
                params1.height = 0
                collageRow1.layoutParams = params1

                binding.cover1.load(headerCovers.getOrNull(0)) { placeholder(android.R.color.transparent) }
                binding.cover2.load(headerCovers.getOrNull(1)) { placeholder(android.R.color.transparent) }
                binding.cover3.load(headerCovers.getOrNull(2)) { placeholder(android.R.color.transparent) }
                binding.cover4.load(headerCovers.getOrNull(3)) { placeholder(android.R.color.transparent) }
            }
        }

        if (playlistUris.isEmpty()){
            binding.songsRecyclerView.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
            binding.emptyState.text = getString(R.string.empty_playlist)
        } else {
            binding.songsRecyclerView.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun openAudioActivity(list: List<AudioFile>, position: Int) {
        if (list.isEmpty()) return

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

    override fun onResume() {
        super.onResume()
        val playlistName = intent.getStringExtra("PLAYLIST_NAME")
        if (playlistName != null) {
            loadSongs(playlistName)
        }
    }
}