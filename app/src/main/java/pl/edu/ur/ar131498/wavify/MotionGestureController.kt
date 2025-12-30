package pl.edu.ur.ar131498.wavify

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.preference.PreferenceManager

class MotionGestureController(
    context: Context,
    private val onAction: (GestureAction) -> Unit
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Cooldown dla gestów
    private var lastActionTime = 0L
    private val cooldownMs = 800L

    // Czułość gestów
    private var sensitivityFactor = 1f
    private var swipeThreshold = 16f  // X
    private var dropThreshold = -3f   // Y

    // Czułość z ustawień
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val sensitivity = prefs.getString("pref_gesture_sensitivity", "medium")!!
    private val reversed = prefs.getString("pref_gesture_reversed", "normal").equals("reversed")

    // Wczytanie czułości
    init {
        updateSensitivity(sensitivity)
    }

    fun updateSensitivity(value: String) {
        sensitivityFactor = when (value) {
            "low" -> 2f
            "medium" -> 1f
            "high" -> 0.5f
            else -> 1f
        }
        swipeThreshold *= sensitivityFactor
        dropThreshold *= sensitivityFactor
    }


    fun start() {
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0] // lewo (-) / prawo (+)
        val y = event.values[1] // góra (+) / dół (-)
        val now = System.currentTimeMillis()

        if (now - lastActionTime < cooldownMs) return

        when {
            // machnięcie w prawo - następny utwór (przy odwróconym - poprzedni)
            x > swipeThreshold -> {
                lastActionTime = now
                if (reversed) onAction(GestureAction.PREVIOUS) else onAction(GestureAction.NEXT)
            }
            // machnięcie w lewo - poprzedni utwór (przy odwróconym - następny)
            x < -swipeThreshold -> {
                lastActionTime = now
                if (reversed) onAction(GestureAction.NEXT) else onAction(GestureAction.PREVIOUS)
            }
            // Machnięcie w dół lub w górę - odtwarzanie / pauza
            y < dropThreshold -> {
                lastActionTime = now
                onAction(GestureAction.PLAY_PAUSE)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}