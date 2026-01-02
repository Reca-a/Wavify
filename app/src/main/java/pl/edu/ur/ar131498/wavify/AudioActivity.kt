package pl.edu.ur.ar131498.wavify

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import pl.edu.ur.ar131498.wavify.databinding.ActivityAudioBinding
import androidx.media3.common.MediaItem
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import coil.load
import com.google.android.material.slider.Slider
import androidx.core.net.toUri
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class AudioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAudioBinding
    private lateinit var controller: MediaController
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private var items: List<MediaItem> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private var startIndex: Int = 0
    private var isUserSeeking = false
    private var isShuffleEnabled = false
    private var repeatMode = Player.REPEAT_MODE_OFF
    private lateinit var gestureController: GestureController
    private lateinit var prefs: SharedPreferences
    private lateinit var preferenceListener: SharedPreferences.OnSharedPreferenceChangeListener
    private val dimHandler = Handler(Looper.getMainLooper())
    private var originalBrightness = -1f
    private var isHandGesture = false
    private val dimRunnable = Runnable {
        if (isHandGesture) {
            dimScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Odczyt zapisanych ustawień
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Odczyt danych z intent
        val uriStrings = intent.getStringArrayListExtra("SONG_URIS") ?: arrayListOf()
        val titles = intent.getStringArrayListExtra("SONG_TITLES") ?: arrayListOf()
        val artists = intent.getStringArrayListExtra("SONG_ARTISTS") ?: arrayListOf()
        val albumArtStrings = intent.getStringArrayListExtra("ALBUM_ART_URIS") ?: arrayListOf()
        startIndex = intent.getIntExtra("START_INDEX", 0)

        // Rekonstrukcja obiektów AudioFile
        val songs = uriStrings.indices.map { i ->
            AudioFile(
                uri = uriStrings[i].toUri(),
                title = titles.getOrNull(i) ?: getString(R.string.unknown_title),
                artist = artists.getOrNull(i),
                albumArtUri = albumArtStrings.getOrNull(i)?.let {
                    if (it.isNotEmpty()) it.toUri() else null
                }
            )
        }
        // Tworzenie listy utworów
        items = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(
                            if (!song.artist.isNullOrEmpty() && song.artist != "<unknown>")
                                song.artist
                            else
                                getString(R.string.unknown_artist)
                        )
                        .setArtworkUri(
                            song.albumArtUri ?: "@drawable/default_album_art.jpg".toUri()
                        )
                        .build()
                )
                .build()
        }

        // Obsługa gestów
        val hand = HandGestureController(
            context = this,
            lifecycleOwner = this
        ) { action ->
            handleGestureAction(action)
        }
        val motion = MotionGestureController(this) { action ->
            handleGestureAction(action)
        }
        gestureController = GestureController(motion, hand)

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

                val hasNewPlaylist = intent.hasExtra("SONG_URIS") && songs.isNotEmpty()

                if (hasNewPlaylist) {
                    controller.setMediaItems(items, startIndex, 0)
                    controller.prepare()
                    controller.play()
                }

                initializePlayer()
                applyGestureSettings()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())

        setupTimeBar()
        startProgressUpdate()

        // Aktualizowanie ustawień gestów
        preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "pref_gesture_control") {
                applyGestureSettings()
            }

            if (key == "pref_gesture_sensitivity") {
                gestureController.updateMotionSens()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)

        // Opóźnione przewijanie długiego tytułu utworu
        binding.songTitleText.postDelayed({
            binding.songTitleText.isSelected = true
        }, 3000)

        findViewById<MaterialButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun initializePlayer() {
        // Listenery przycisków odtwarzacza
        binding.playPauseButton.setOnClickListener { playPause() }
        binding.nextButton.setOnClickListener { nextSong() }
        binding.prevButton.setOnClickListener { previousSong() }
        binding.shuffleButton.setOnClickListener { toggleShuffle() }
        binding.repeatButton.setOnClickListener { toggleRepeat() }

        // Listener dla odtwarzacza
        controller.addListener(object : Player.Listener {
            // Zmiana utworu
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshFromController()
            }

            // Pauza / odtworzenie
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                binding.playPauseButton.setIconResource(
                    if (isPlaying) R.drawable.ic_pause_32
                    else R.drawable.ic_play_32
                )
            }
        })

        refreshFromController()
    }

    private fun refreshFromController() {
        val mediaItem = controller.currentMediaItem ?: return
        val metadata = mediaItem.mediaMetadata

        binding.songTitleText.text =
            metadata.title?.toString() ?: getString(R.string.unknown_title)

        binding.songArtistText.text =
            metadata.artist?.toString() ?: getString(R.string.unknown_artist)

        binding.albumArtImageView.load(metadata.artworkUri) {
            placeholder(R.drawable.default_album_art)
            error(R.drawable.default_album_art)
        }

        if (controller.isPlaying)
            binding.playPauseButton.setIconResource(R.drawable.ic_pause_32)
        else
            binding.playPauseButton.setIconResource(R.drawable.ic_play_32)

        if (controller.shuffleModeEnabled) {
            isShuffleEnabled = true
            binding.shuffleButton.iconTint = ColorStateList.valueOf(
                getThemeColor(com.google.android.material.R.attr.colorOnSecondary)
            )
        }
        if (controller.repeatMode == Player.REPEAT_MODE_ONE) {
            repeatMode = Player.REPEAT_MODE_ONE
            binding.repeatButton.iconTint = ColorStateList.valueOf(
                getThemeColor(com.google.android.material.R.attr.colorOnSecondary)
            )
        }
    }

    private fun handleGestureAction(action: GestureAction) {
        when (action) {
            GestureAction.NEXT -> nextSong()
            GestureAction.PREVIOUS -> previousSong()
            GestureAction.PLAY_PAUSE -> playPause()
            GestureAction.PLAY -> controller.play()
            GestureAction.PAUSE -> controller.pause()
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

        repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF
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

    private fun applyGestureSettings() {
        val gestureMode = prefs.getString("pref_gesture_control", "none")

        if (gestureMode == "hand") {
            isHandGesture = true
            originalBrightness = window.attributes.screenBrightness
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            resetDimTimer()
        } else{
            isHandGesture = false
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            restoreBrightness()
            dimHandler.removeCallbacks(dimRunnable)
        }

        when (gestureMode) {
            "shake" -> gestureController.enableMotion()
            "hand" -> gestureController.enableHand()
            "none" -> gestureController.disableAll()
        }
    }

    private fun dimScreen() {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 0.01f
        window.attributes = layoutParams
    }

    private fun restoreBrightness() {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = if (originalBrightness >= 0) {
            originalBrightness
        } else {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE // -1f = systemowa jasność
        }
        window.attributes = layoutParams
    }

    fun resetDimTimer() {
        // Przywróć pełną jasność
        restoreBrightness()

        // Resetuj timer
        dimHandler.removeCallbacks(dimRunnable)
        if (isHandGesture) {
            dimHandler.postDelayed(dimRunnable, 10000) // 10 sekund
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (isHandGesture) {
            resetDimTimer()
        }
    }

    // Funkcja pomocnicza do formatowania wyświetlanego czasu
    @SuppressLint("DefaultLocale")
    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60

        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onStart() {
        super.onStart()
        if (::controller.isInitialized) {
            refreshFromController()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::controllerFuture.isInitialized) {
            MediaController.releaseFuture(controllerFuture)
            gestureController.disableAll()
            prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        }

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        restoreBrightness()
        dimHandler.removeCallbacks(dimRunnable)

        handler.removeCallbacksAndMessages(null)
    }
}