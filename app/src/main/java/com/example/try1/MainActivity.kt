package com.example.try1

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.try1.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var binding: ActivityMainBinding

    private val sensorDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MotionSensorService.ACTION_SENSOR_DATA_UPDATE -> {
                    val posX = intent.getFloatExtra(MotionSensorService.EXTRA_POSITION_X, 0f)
                    val posY = intent.getFloatExtra(MotionSensorService.EXTRA_POSITION_Y, 0f)
                    val posZ = intent.getFloatExtra(MotionSensorService.EXTRA_POSITION_Z, 0f)
                    val variance = intent.getFloatExtra(MotionSensorService.EXTRA_VARIANCE, 0f)

                    binding.positionXTextView.text = "Position X: ${"%.2f".format(posX)} m"
                    binding.positionYTextView.text = "Position Y: ${"%.2f".format(posY)} m"
                    binding.positionZTextView.text = "Position Z: ${"%.2f".format(posZ)} m"
                    binding.varianceTextView.text = "Variance: ${"%.5f".format(variance)}"

                    // Directly tell the PathView to draw the new point
                    binding.pathView.addPoint(posX, posY)
                }
                MotionSensorService.ACTION_STATUS_UPDATE -> {
                    val message = intent.getStringExtra(MotionSensorService.EXTRA_STATUS_MESSAGE) ?: "Idle"
                    binding.statusTextView.text = "Status: $message"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startButton.setOnClickListener {
            if (hasPermissions()) {
                // Tell the PathView to clear its canvas before starting
                binding.pathView.clearPath()
                startMotionService()
            } else {
                requestPermissions()
            }
        }
        binding.stopButton.setOnClickListener {
            stopMotionService()
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(MotionSensorService.ACTION_SENSOR_DATA_UPDATE)
            addAction(MotionSensorService.ACTION_STATUS_UPDATE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(sensorDataReceiver, filter)
    }

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