package com.example.try1

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100

    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusTextView: TextView // For showing "Calibrating..."
    private lateinit var positionXTextView: TextView
    private lateinit var positionYTextView: TextView
    private lateinit var positionZTextView: TextView
    private lateinit var pathView: PathView

    private val sensorDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MotionSensorService.ACTION_SENSOR_DATA_UPDATE -> {
                    // ... (this part is unchanged)
                    val posX = intent.getFloatExtra(MotionSensorService.EXTRA_POSITION_X, 0f)
                    val posY = intent.getFloatExtra(MotionSensorService.EXTRA_POSITION_Y, 0f)
                    val posZ = intent.getFloatExtra(MotionSensorService.EXTRA_POSITION_Z, 0f)
                    positionXTextView.text = "Position X: ${"%.2f".format(posX)} m"
                    positionYTextView.text = "Position Y: ${"%.2f".format(posY)} m"
                    positionZTextView.text = "Position Z: ${"%.2f".format(posZ)} m"
                    pathView.addPoint(posX, posY)
                }
                // --- CALIBRATION UI FIX ---
                // Listen for the new status updates
                MotionSensorService.ACTION_STATUS_UPDATE -> {
                    val message = intent.getStringExtra(MotionSensorService.EXTRA_STATUS_MESSAGE) ?: "Idle"
                    statusTextView.text = "Status: $message"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Make sure you have activity_main.xml

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        statusTextView = findViewById(R.id.statusTextView)
        positionXTextView = findViewById(R.id.positionXTextView)
        positionYTextView = findViewById(R.id.positionYTextView)
        positionZTextView = findViewById(R.id.positionZTextView)
        pathView = findViewById(R.id.pathView)

        startButton.setOnClickListener {
            if (hasPermissions()) {
                pathView.clearPath() // Clear path when starting
                startMotionService()
            } else {
                requestPermissions()
            }
        }

        stopButton.setOnClickListener {
            stopMotionService()
        }
    }

    override fun onResume() {
        super.onResume()
        // Make sure the receiver listens for both types of actions
        val filter = IntentFilter().apply {
            addAction(MotionSensorService.ACTION_SENSOR_DATA_UPDATE)
            addAction(MotionSensorService.ACTION_STATUS_UPDATE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(sensorDataReceiver, filter)
    }

    // ... (rest of MainActivity is unchanged)

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorDataReceiver)
    }

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startMotionService()
            } else {
                Toast.makeText(this, "Permission denied. Cannot start service.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startMotionService() {
        val intent = Intent(this, MotionSensorService::class.java)
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "Motion Sensor Service Starting...", Toast.LENGTH_SHORT).show()
    }

    private fun stopMotionService() {
        val intent = Intent(this, MotionSensorService::class.java)
        stopService(intent)
        Toast.makeText(this, "Motion Sensor Service Stopped", Toast.LENGTH_SHORT).show()
    }
}