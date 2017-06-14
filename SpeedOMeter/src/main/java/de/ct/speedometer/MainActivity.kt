/*

    Copyright (c) 2017 Oliver Lau <ola@ct.de>, Heise Medien GmbH & Co. KG

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package de.ct.speedometer

import android.Manifest
import android.content.pm.PackageManager
import android.support.v4.app.NotificationCompat.CarExtender
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.Menu
import android.widget.SeekBar
import android.widget.Toast


class MainActivity : AppCompatActivity(), IBaseGpsListener, ISpeedRangeSelectionListener, IOnResetListener {
    private var intervals = mutableListOf<SpeedInterval>()
    private var locationManager: LocationManager? = null
    private var speedometer: SpeedometerView? = null
    private var sensorManager: SensorManager? = null
    private var linearAccelerometer: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magneticField: Sensor? = null
    private var rotationVector: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        checkPermissions()
        initAccelerationSensors()
        updateSpeedometer()
    }


    private fun initViews() {
        intervals.addAll(listOf(
                SpeedInterval(this, R.id.stopwatch1, 0, 100),
                SpeedInterval(this, R.id.stopwatch2, 80, 120),
                SpeedInterval(this, R.id.stopwatch3, 0, 200),
                SpeedInterval(this, R.id.stopwatch4, 0, 250)))
        speedometer = findViewById(R.id.speedometer) as SpeedometerView
        speedometer!!.setOnResetListener(this)
        var alphaSeekBar: SeekBar = findViewById(R.id.alphaSeekBar) as SeekBar
        alphaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val alpha = .5f + .4999f * progress / alphaSeekBar.max
                speedometer!!.setSmoothingAlpha(alpha)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // do nothing ...
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val prefs = getPreferences(Context.MODE_PRIVATE)
                val alpha = seekBar.progress.toFloat() / seekBar.max
                prefs.edit().putFloat("alpha", alpha).apply()
            }
        })
        val prefs = getPreferences(Context.MODE_PRIVATE)
        val alpha = prefs.getFloat("alpha", DEFAULT_SMOOTHING_ALPHA)
        alphaSeekBar.progress = (alpha * alphaSeekBar.max).toInt()
    }


    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            /*
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
            */
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (PackageManager.PERMISSION_GRANTED in grantResults) {
                    initLocationManager()
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings, menu)
        return true
    }


    override fun onStart() {
        super.onStart()
        startLocationUpdates()
    }


    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        sensorManager!!.registerListener(speedometer, linearAccelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager!!.registerListener(speedometer, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager!!.registerListener(speedometer, magneticField, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(speedometer, rotationVector, SensorManager.SENSOR_DELAY_NORMAL)
        keepAlive()
    }


    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        sensorManager!!.unregisterListener(speedometer, linearAccelerometer)
        sensorManager!!.unregisterListener(speedometer, accelerometer)
        sensorManager!!.unregisterListener(speedometer, magneticField)
        sensorManager!!.unregisterListener(speedometer, rotationVector)
        allowSleep()
    }


    override fun onStop() {
        super.onStop()
    }


    override fun onDestroy() {
        super.onDestroy()
    }


    private fun keepAlive() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RaceOMeterWakelockTag")
        wakeLock!!.acquire(10L * 60L * 100L)
    }


    private fun allowSleep() {
        wakeLock!!.release()
    }


    private fun updateSpeedometer() {
        speedometer!!.removeSpecialTicks()
        intervals.forEach {
            speedometer!!.addSpecialTick(it.lo)
            speedometer!!.addSpecialTick(it.hi)
        }
    }


    override fun onSelectedSpeedRange(lo: Int, hi: Int) {
        updateSpeedometer()
    }


    private fun createSpeedRunnable(t: Float): Runnable {
        return Runnable {
            val location = Location("")
            location.speed = 300 * t / (t + 6.7f) / SPEED_FACTOR
            onLocationChanged(location)
        }
    }


    private fun startLocationUpdates() {
        try {
            if (locationManager == null) {
                locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            }
            locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
            locationManager!!.addGpsStatusListener(this)
        } catch (e: java.lang.SecurityException) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }

        if (SimulationEnabled) {
            val MaxT = 23f
            val h = android.os.Handler()
            var t = .5f
            while (t < MaxT) {
                h.postDelayed(createSpeedRunnable(t), (1e3f * t).toLong())
                t += .3f
            }
            h.postDelayed(createSpeedRunnable(0f), (1e3f * (MaxT + 1)).toLong())
        }
    }


    private fun reset() {
        val location = Location("")
        location.speed = 0f
        onLocationChanged(location)
    }


    override fun onReset() {
        reset()
    }


    private fun stopLocationUpdates() {
        locationManager!!.removeUpdates(this)
        locationManager = null
    }


    private fun initLocationManager() {
        startLocationUpdates()
        try {
            if (locationManager != null) {
                locationManager!!.addGpsStatusListener(this)
            }
        } catch (e: java.lang.SecurityException) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
        updateSpeed(null)
    }


    private fun initAccelerationSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        linearAccelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magneticField = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        rotationVector = sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }


    private fun updateSpeed(location: Location?) {
        if (location == null) {
            intervals.forEach { it.reset() }
            speedometer!!.setSpeed(0f)
        } else {
            speedometer!!.setSpeed(SPEED_FACTOR * location.speed)
            speedometer!!.setLocation(location)
        }
    }


    override fun onLocationChanged(location: Location) {
        val speed = SPEED_FACTOR * location.speed
        intervals.forEach { it.process(speed) }
        updateSpeed(location)
    }


    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "onProviderDisabled(\"$provider\")")
    }


    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "onProviderEnabled(\"$provider\")")
    }


    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        Log.d(TAG, "onStatusChanged(\"$provider\", $status)")
    }


    override fun onGpsStatusChanged(event: Int) {
        Log.d(TAG, "onGpsStatusChanged($event)")
    }


    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0x4001
        private val SPEED_FACTOR = 3.6e3f * 1e-3f
        private val DEFAULT_SMOOTHING_ALPHA = .955f
        private val SimulationEnabled = false
    }
}
