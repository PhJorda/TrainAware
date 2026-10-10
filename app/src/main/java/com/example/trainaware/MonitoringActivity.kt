package com.example.trainaware

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class MonitoringActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRAIN_LENGTH = "extra_train_length"
        const val EXTRA_DEVELOPER_MODE = "extra_developer_mode"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var tvSpeed: TextView
    private lateinit var tvTrainInfo: TextView
    private lateinit var heartbeatView: HeartbeatView

    private var trainLength: Double = 0.0
    private var isDeveloperMode = false
    private var isBlinking = false

    private var isTrainInfoVisible = false
    private var hasBlinkedForThisCycle = false
    private var hasReachedHighSpeed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bildschirmsperre/Dimmen verhindern
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Systemleisten (Akkustand, Uhrzeit, Navigationsleiste) ausblenden (Immersive Fullscreen)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContentView(R.layout.activity_monitoring)

        trainLength = intent.getDoubleExtra(EXTRA_TRAIN_LENGTH, 0.0)
        isDeveloperMode = intent.getBooleanExtra(EXTRA_DEVELOPER_MODE, false)

        val btnReturn = findViewById<ImageButton>(R.id.btnReturn)
        btnReturn.setOnClickListener {
            finish()
        }

        tvSpeed = findViewById(R.id.tvSpeed)
        tvTrainInfo = findViewById(R.id.tvTrainInfo)
        heartbeatView = findViewById(R.id.heartbeatView)

        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        val isHeartbeatEnabled = prefs.getBoolean(SettingsActivity.KEY_HEARTBEAT_ENABLED, true)
        if (isHeartbeatEnabled) {
            heartbeatView.visibility = View.VISIBLE
            heartbeatView.startAnimation()
        } else {
            heartbeatView.visibility = View.GONE
            heartbeatView.stopAnimation()
        }

        if (isDeveloperMode) {
            tvSpeed.visibility = View.VISIBLE
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationTracking()
    }

    private fun startLocationTracking() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    processSpeed(location)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            Toast.makeText(this, "Standortberechtigung nicht vorhanden!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun processSpeed(location: Location) {
        if (!location.hasSpeed()) return

        val speedKmh = location.speed * 3.6

        if (isDeveloperMode) {
            tvSpeed.text = getString(R.string.speed_format, speedKmh)
        }

        // 1. Stillstand / Halt-Erkennung (< 5 km/h)
        if (speedKmh < 5) {
            isTrainInfoVisible = false
            tvTrainInfo.text = ""
            hasReachedHighSpeed = false
            hasBlinkedForThisCycle = false
            return
        }

        // 2. Highspeed-Erkennung (wartet auf 70 km/h)
        if (speedKmh >= 70) {
            hasReachedHighSpeed = true
            hasBlinkedForThisCycle = false
        }

        // Überwachung greift erst nach einmaliger Überschreitung von 70 km/h
        if (hasReachedHighSpeed) {
            if (!isTrainInfoVisible && speedKmh < 60) {
                isTrainInfoVisible = true
            } else if (isTrainInfoVisible && speedKmh >= 70) {
                isTrainInfoVisible = false
            }

            if (isTrainInfoVisible) {
                tvTrainInfo.text = getString(R.string.train_length_format, trainLength)
            } else {
                tvTrainInfo.text = ""
            }

            if (speedKmh < 40 && !hasBlinkedForThisCycle) {
                hasBlinkedForThisCycle = true
                triggerTextBlink()
            }
        }
    }

    private fun triggerTextBlink() {
        if (isBlinking) return
        isBlinking = true

        lifecycleScope.launch {
            val orangeColor = "#FFA500".toColorInt()
            val whiteColor = Color.WHITE

            repeat(4) {
                tvTrainInfo.setTextColor(orangeColor)
                delay(250.milliseconds)
                tvTrainInfo.setTextColor(whiteColor)
                delay(250.milliseconds)
            }

            tvTrainInfo.setTextColor(whiteColor)
            isBlinking = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
