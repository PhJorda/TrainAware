package com.example.trainaware // Passe dies an deinen echten Package-Namen an!

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var etTrainLength: EditText
    private lateinit var cbDeveloperMode: CheckBox
    private lateinit var btnStart: Button
    private lateinit var tvSpeed: TextView
    private lateinit var tvTrainInfo: TextView
    private lateinit var mainLayout: LinearLayout
    private lateinit var inputContainer: LinearLayout

    private var trainLength: Double = 0.0
    private var isDeveloperMode = false
    private var isTracking = false
    private var isBlinking = false

    // Zustandsvariablen für Hysterese, Blinken und Stillstand
    private var isTrainInfoVisible = false
    private var hasBlinkedForThisCycle = false
    private var hasReachedHighSpeed = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etTrainLength = findViewById(R.id.etTrainLength)
        cbDeveloperMode = findViewById(R.id.cbDeveloperMode)
        btnStart = findViewById(R.id.btnStart)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvTrainInfo = findViewById(R.id.tvTrainInfo)
        mainLayout = findViewById(R.id.mainLayout)
        inputContainer = findViewById(R.id.inputContainer)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnStart.setOnClickListener {
            val input = etTrainLength.text.toString()
            if (input.isNotEmpty()) {
                trainLength = input.toDouble()
                isDeveloperMode = cbDeveloperMode.isChecked

                // UI verstecken und Bildschirm schwarz machen
                inputContainer.visibility = View.GONE
                mainLayout.setBackgroundColor(Color.BLACK)

                // Falls Developer Modus aktiv ist, Geschwindigkeitsanzeige einblenden
                if (isDeveloperMode) {
                    tvSpeed.visibility = View.VISIBLE
                }

                checkLocationPermissionAndStart()
            } else {
                Toast.makeText(this, "Bitte Zuglänge eingeben!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationTracking()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationTracking()
            } else {
                Toast.makeText(this, "Standortberechtigung verweigert!", Toast.LENGTH_SHORT).show()
                inputContainer.visibility = View.VISIBLE
                mainLayout.setBackgroundColor(Color.WHITE)
            }
        }
    }

    private fun startLocationTracking() {
        isTracking = true
        Toast.makeText(this, "Überwachung aktiv", Toast.LENGTH_SHORT).show()

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
        }
    }

    private fun processSpeed(location: Location) {
        val speedKmh = location.speed * 3.6

        // Wenn Developer-Modus aktiv ist, Geschwindigkeit permanent aktualisieren
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
            // Hysterese-Logik für die Zuglänge (< 60 ein, >= 70 aus)
            if (!isTrainInfoVisible && speedKmh < 60) {
                isTrainInfoVisible = true
            } else if (isTrainInfoVisible && speedKsmOrMore(speedKmh)) { // Hilfsabfrage
                isTrainInfoVisible = false
            }

            if (isTrainInfoVisible) {
                tvTrainInfo.text = getString(R.string.train_length_format, trainLength)
            } else {
                tvTrainInfo.text = ""
            }

            // Blink-Logik beim Unterschreiten von 40 km/h (1x pro Zyklus)
            if (speedKmh < 40 && !hasBlinkedForThisCycle) {
                hasBlinkedForThisCycle = true
                triggerTextBlink()
            }
        }
    }

    // Kleine Hilfsfunktion für lesbareren Code
    private fun speedKsmOrMore(speedKmh: Double): Boolean {
        return speedKmh >= 70
    }

    private fun triggerTextBlink() {
        if (isBlinking) return
        isBlinking = true

        CoroutineScope(Dispatchers.Main).launch {
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