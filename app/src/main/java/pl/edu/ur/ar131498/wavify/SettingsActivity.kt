package pl.edu.ur.ar131498.wavify

import android.os.Bundle
import android.view.View.GONE
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.button.MaterialButton
import android.content.Intent
import androidx.core.net.toUri

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<MaterialButton>(R.id.settingsButton).visibility = GONE
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val themePref = findPreference<ListPreference>("pref_theme_mode")
            val gesturePref = findPreference<ListPreference>("pref_gesture_control")
            val gestureSens = findPreference<ListPreference>("pref_gesture_sensitivity")
            val gestureRev = findPreference<ListPreference>("pref_gesture_reversed")

            themePref?.let { updateSummary(it) }
            gesturePref?.let { updateSummary(it) }
            gestureSens?.let { updateSummary(it) }
            gestureRev?.let { updateSummary(it) }

            if (gesturePref?.value == "shake") {
                gestureSens?.isVisible = true
                gestureRev?.isVisible = true
            }

            // Obsługa kliknięcia w "O aplikacji"
            findPreference<Preference>("pref_about")?.setOnPreferenceClickListener {
                showAboutDialog()
                true
            }

            // Obsługa kliknięcia w "Polityka prywatności"
            findPreference<Preference>("pref_privacy_policy")?.setOnPreferenceClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://reca-a.github.io/Wavify-privacy-policy/".toUri()
                )
                startActivity(intent)
                true
            }

            // Obsługa kliknięcia w "Tryb motywu"
            themePref?.setOnPreferenceChangeListener { preference, newValue ->
                applyTheme(newValue.toString())

                val index = themePref.findIndexOfValue(newValue.toString())
                preference.summary = if (index >= 0) themePref.entries[index] else null
                true
            }

            // Obsługa kliknięcia w "Tryb gestów"
            gesturePref?.setOnPreferenceChangeListener { preference, newValue ->
                val index = gesturePref.findIndexOfValue(newValue.toString())
                preference.summary = if (index >= 0) gesturePref.entries[index] else null

                val motionEnabled = newValue == "shake"
                gestureSens?.isVisible = motionEnabled
                gestureRev?.isVisible = motionEnabled
                true
            }

            // Obsługa kliknięcia w "Czułość gestów"
            gestureSens?.setOnPreferenceChangeListener { preference, newValue ->
                val index = gestureSens.findIndexOfValue(newValue.toString())
                preference.summary = if (index >= 0) gestureSens.entries[index] else null
                true
            }

            // Obsługa kliknięcia w "Kierunek gestów"
            gestureRev?.setOnPreferenceChangeListener { preference, newValue ->
                val index = gestureRev.findIndexOfValue(newValue.toString())
                preference.summary = if (index >= 0) gestureRev.entries[index] else null
                true
            }

            // Obsługa kliknięcia w "Sterowanie gestami"
            findPreference<Preference>("pref_gesture_help")?.setOnPreferenceClickListener {
                showGestureHelpDialog()
                true
            }
        }

        private fun showAboutDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.about))
                .setMessage(getString(R.string.about_desc))
                .setPositiveButton("OK", null)
                .show()
        }

        private fun applyTheme(value: String) {
            when (value) {
                "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        private fun updateSummary(pref: ListPreference) {
            val index = pref.findIndexOfValue(pref.value)
            pref.summary =
                if (index >= 0) pref.entries[index] else null
        }

        private fun showGestureHelpDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.gesture_help_title)
                .setMessage(
                    getString(R.string.gesture_motion_desc) +
                            "\n\n" +
                            getString(R.string.gesture_hand_desc) +
                            "\n\n" +
                            getString(R.string.gesture_tips)
                )
                .setPositiveButton("OK", null)
                .show()
        }
    }
}