package com.example.wavify

import android.content.Context
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
import androidx.core.content.edit

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

            themePref?.summary = themePref?.entry
            gesturePref?.summary = gesturePref?.entry
            gestureSens?.summary = gestureSens?.entry

            // Obsługa kliknięcia w "O aplikacji"
            findPreference<Preference>("pref_about")?.setOnPreferenceClickListener {
                showAboutDialog()
                true
            }

            // Obsługa kliknięcia w "Tryb motywu"
            themePref?.setOnPreferenceChangeListener { preference, newValue ->
                applyTheme(newValue.toString())

                val index = themePref.findIndexOfValue(newValue.toString())
                preference.summary = if (index >= 0) themePref.entries[index] else null
                true
            }

            // Obsługa kliknięcia w "Sterowanie gestami"
            gesturePref?.setOnPreferenceChangeListener { preference, newValue ->
                // TODO zaimplementować obsługę

                val index = gesturePref.findIndexOfValue(newValue.toString())
                preference.summary = if (index >= 0) gesturePref.entries[index] else null
                true
            }

            // Obsługa kliknięcia w "Czułość gestów"
            gestureSens?.setOnPreferenceChangeListener { preference, newValue ->
                val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
                prefs.edit (commit = true) { putString("pref_gesture_sensitivity", newValue.toString()) }

                val index = gestureSens.findIndexOfValue(newValue.toString())
                preference.summary = if (index >= 0) gestureSens.entries[index] else null
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
    }
}