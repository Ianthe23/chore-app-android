package com.choreapp.android.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * ShakeDetector - Detects shake gestures using the accelerometer sensor (3p requirement - sensors)
 *
 * This class monitors the device's accelerometer to detect shake gestures.
 * When a shake is detected above the threshold, it triggers the onShake callback.
 */
class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime: Long = 0

    companion object {
        private const val SHAKE_THRESHOLD = 15.0f // Gravity threshold for shake detection
        private const val SHAKE_TIME_DELAY = 1000 // Minimum time between shakes (ms)
    }

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                // Calculate acceleration magnitude
                val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                // Remove gravity contribution (1g = 9.8 m/s²)
                val accelerationWithoutGravity = acceleration - SensorManager.GRAVITY_EARTH

                // Check if shake is strong enough and enough time has passed
                val currentTime = System.currentTimeMillis()
                if (accelerationWithoutGravity > SHAKE_THRESHOLD) {
                    if (currentTime - lastShakeTime > SHAKE_TIME_DELAY) {
                        lastShakeTime = currentTime
                        onShake()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for shake detection
    }
}