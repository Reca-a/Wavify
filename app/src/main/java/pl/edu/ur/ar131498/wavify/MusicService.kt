package pl.edu.ur.ar131498.wavify

import android.app.PendingIntent
import android.content.Intent
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider

class MusicService : MediaSessionService() {
    private val timerHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val timerRunnable = Runnable {
        player.pause()
        Toast.makeText(this, getString(R.string.timer_finished), Toast.LENGTH_SHORT).show()
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Inicjalizacja ExoPlayera
        player = ExoPlayer.Builder(this).build()

        val notificationProvider = DefaultMediaNotificationProvider(this).apply {
            setSmallIcon(R.drawable.ic_notification)
        }

        setMediaNotificationProvider(notificationProvider)

        // Tworzenie MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, AudioActivity::class.java).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            "ACTION_SLEEP_TIMER" -> {
                val minutes = intent.getIntExtra("DURATION_MINUTES", 0)
                timerHandler.removeCallbacks(timerRunnable)
                if (minutes > 0) {
                    timerHandler.postDelayed(timerRunnable, minutes * 60 * 1000L)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.run {
            stop()
            clearMediaItems()
        }
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        timerHandler.removeCallbacks(timerRunnable)
        super.onDestroy()
    }
}