package com.example.try1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.try1.MotionSensorService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100

    private lateinit var positionXTextView: TextView
    private lateinit var positionYTextView: TextView
    private lateinit var positionZTextView: TextView

    private lateinit var pathView: PathView
    private val sensorDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MotionSensorService.ACTION_SENSOR_DATA_UPDATE) {
                val posX = intent.getFloatExtra(MotionSensorService.EXTRA_POSITION_X, 0f)
                val posY = intent.getFloatExtra(MotionSensorService.EXTRA_POSITION_Y, 0f)
                val posZ = intent.getFloatExtra(MotionSensorService.EXTRA_POSITION_Z, 0f)

                // Update the text views with the new data
                positionXTextView.text = "Position X: ${"%.2f".format(posX)} m"
                positionYTextView.text = "Position Y: ${"%.2f".format(posY)} m"
                positionZTextView.text = "Position Z: ${"%.2f".format(posZ)} m"

                pathView.addPoint(posX, posY)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startButton = Button(this).apply {
            text = "Start Motion Sensor Service"
            setOnClickListener {
                if (hasPermissions()) {
                    pathView.clearPath()
                    startMotionService()
                } else {
                    requestPermissions()
                }
            }
            // Add top margin to create space from top (e.g. 50dp)
            val topMarginPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics).toInt()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, topMarginPx, 0, 20)  // Bottom margin 20px to separate buttons
            layoutParams = params
        }

        val stopButton = Button(this).apply {
            text = "Stop Motion Sensor Service"
            setOnClickListener {
                stopMotionService()
            }
            // Same width params used for consistent spacing below start button
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 0)
            layoutParams = params
        }

        positionXTextView = TextView(this).apply { text = "Position X: 0.00 m" }
        positionYTextView = TextView(this).apply { text = "Position Y: 0.00 m" }
        positionZTextView = TextView(this).apply { text = "Position Z: 0.00 m" }

        pathView = PathView(this)
        // Set layout params to make it fill the remaining space
        pathView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1.0f // This 'weight' makes it expand
        )

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(startButton)
            addView(stopButton)
            // NEW: Add the TextViews to the layout
            addView(positionXTextView)
            addView(positionYTextView)
            addView(positionZTextView)

            addView(pathView)
        }

        setContentView(layout)
    }
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(MotionSensorService.ACTION_SENSOR_DATA_UPDATE)
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
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                startMotionService()
            } else {
                Toast.makeText(this, "Permission denied. Cannot start service.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startMotionService() {
        val intent = Intent(this, MotionSensorService::class.java)
        // CHANGE THIS LINE
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "Motion Sensor Service Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopMotionService() {
        val intent = Intent(this, MotionSensorService::class.java)
        stopService(intent)
        Toast.makeText(this, "Motion Sensor Service Stopped", Toast.LENGTH_SHORT).show()
    }
}
