package com.example.trainaware

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var etTrainLength: EditText
    private lateinit var etTrainNumber: EditText
    private lateinit var btnStart: Button

    private var trainLength: Double = 0.0
    private var isDeveloperMode = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etTrainLength = findViewById(R.id.etTrainLength)
        etTrainNumber = findViewById(R.id.etTrainNumber)
        btnStart = findViewById(R.id.btnStart)

        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        val tvVersion: TextView = findViewById(R.id.tvVersion)
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) {
            "0.2"
        }
        tvVersion.text = getString(R.string.app_version_format, versionName)

        btnStart.setOnClickListener {
            val input = etTrainLength.text.toString().trim()
            val parsedLength = input.toDoubleOrNull()
            if (parsedLength != null && parsedLength > 0) {
                trainLength = parsedLength
                val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
                isDeveloperMode = prefs.getBoolean(SettingsActivity.KEY_DEVELOPER_MODE, false)

                checkLocationPermissionAndStart()
            } else {
                Toast.makeText(this, "Bitte gültige Zuglänge eingeben!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        val isTrainNumberEnabled = prefs.getBoolean(SettingsActivity.KEY_TRAIN_NUMBER_ENABLED, true)
        etTrainNumber.visibility = if (isTrainNumberEnabled) View.VISIBLE else View.GONE
    }

    private fun checkLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startMonitoringActivity()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startMonitoringActivity()
            } else {
                Toast.makeText(this, "Standortberechtigung verweigert!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startMonitoringActivity() {
        val intent = Intent(this, MonitoringActivity::class.java).apply {
            putExtra(MonitoringActivity.EXTRA_TRAIN_LENGTH, trainLength)
            putExtra(MonitoringActivity.EXTRA_DEVELOPER_MODE, isDeveloperMode)
        }
        startActivity(intent)
    }
}
