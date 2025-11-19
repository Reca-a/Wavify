package com.example.wavify

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

            // Obsługa kliknięcia w "O aplikacji"
            findPreference<Preference>("pref_about")?.setOnPreferenceClickListener {
                showAboutDialog()
                true
            }

            // Obsługa kliknięcia w "Tryb motywu"
            findPreference<ListPreference>("pref_theme_mode")?.setOnPreferenceChangeListener { _, newValue ->
                applyTheme(newValue.toString())
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