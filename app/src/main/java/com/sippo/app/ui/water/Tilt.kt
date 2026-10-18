package com.sippo.app.ui.water

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.atan2
import kotlin.math.sqrt

/** Side-to-side device tilt mapped to a water slope target, like the prototype's deviceorientation gamma. */
class Tilt {
    /** Target slope in the simulation's units (±0.4); positive means the right edge of the phone is up. */
    @Volatile var target: Float = 0f
        internal set
    @Volatile var available: Boolean = false
        internal set
}

@Composable
fun rememberTilt(): Tilt {
    val tilt = remember { Tilt() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            var smooth = 0f
            override fun onSensorChanged(e: SensorEvent) {
                val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
                // Roll around the phone's long axis, in radians; 0 when held level.
                val roll = atan2(x, sqrt(y * y + z * z))
                smooth += (roll - smooth) * .2f
                tilt.target = (smooth / (Math.PI.toFloat() / 2f) * .8f).coerceIn(-.4f, .4f)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        fun start() { if (sensor != null) tilt.available = sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME) }
        fun stop() { sm.unregisterListener(listener); tilt.target = 0f }
        val obs = LifecycleEventObserver { _, ev ->
            when (ev) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); stop() }
    }
    return tilt
}
