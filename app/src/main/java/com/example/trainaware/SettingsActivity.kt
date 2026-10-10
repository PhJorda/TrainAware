package com.example.trainaware

import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "train_aware_prefs"
        const val KEY_DEVELOPER_MODE = "developer_mode"
        const val KEY_HEARTBEAT_ENABLED = "heartbeat_enabled"
        const val KEY_TRAIN_NUMBER_ENABLED = "train_number_enabled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val cbDeveloperMode = findViewById<CheckBox>(R.id.cbDeveloperMode)
        cbDeveloperMode.isChecked = prefs.getBoolean(KEY_DEVELOPER_MODE, false)
        cbDeveloperMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit {
                putBoolean(KEY_DEVELOPER_MODE, isChecked)
            }
        }

        val cbHeartbeat = findViewById<CheckBox>(R.id.cbHeartbeat)
        cbHeartbeat.isChecked = prefs.getBoolean(KEY_HEARTBEAT_ENABLED, true)
        cbHeartbeat.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit {
                putBoolean(KEY_HEARTBEAT_ENABLED, isChecked)
            }
        }

        val cbTrainNumber = findViewById<CheckBox>(R.id.cbTrainNumber)
        cbTrainNumber.isChecked = prefs.getBoolean(KEY_TRAIN_NUMBER_ENABLED, true)
        cbTrainNumber.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit {
                putBoolean(KEY_TRAIN_NUMBER_ENABLED, isChecked)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
