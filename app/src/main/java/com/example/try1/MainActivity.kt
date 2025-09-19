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

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startButton = Button(this).apply {
            text = "Start Motion Sensor Service"
            setOnClickListener {
                if (hasPermissions()) {
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

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(startButton)
            addView(stopButton)
        }

        setContentView(layout)
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
        startService(intent)
        Toast.makeText(this, "Motion Sensor Service Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopMotionService() {
        val intent = Intent(this, MotionSensorService::class.java)
        stopService(intent)
        Toast.makeText(this, "Motion Sensor Service Stopped", Toast.LENGTH_SHORT).show()
    }
}
