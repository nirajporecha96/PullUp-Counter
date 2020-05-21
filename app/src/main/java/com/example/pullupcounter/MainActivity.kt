package com.example.pullupcounter

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private lateinit var mSensorG: Sensor
    private var linearAcceleration: Array<Float> = arrayOf(0.0f,0.0f,0.0f,0.0f)
    private var slidingWindow: Array<Float> = arrayOf(0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f)
    private var counter:Int = 0
    private var nmax:Int = 0
    private var lastUpdate:Long = 0
    private var lastSec:Long = 0
    //private var last_x: Float = 0.0f
    //private var last_y: Float = 0.0f
    //private var last_z: Float = 0.0f
    private var shakeDetected: Int = 0
    private val beep = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        et_repnumber.isEnabled = false
        bt_setrep.isEnabled = false
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensor = if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else {
            // Sorry, there are no accelerometers on your device.
            null!!
        }
        mSensorG =  (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE))
        mSensorManager.registerListener(this, mSensor, 40000)

    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        //
    }

    override fun onSensorChanged(event: SensorEvent) {

        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER)
            return

        val curTime = System.currentTimeMillis()
        // only allow one update every 100ms.
        if ((curTime - lastUpdate) > 100) {
            lastUpdate = curTime

            linearAcceleration[0] = event.values[0]
            linearAcceleration[1] = event.values[1]
            linearAcceleration[2] = event.values[2]
            linearAcceleration[3] = sqrt((event.values[0] * event.values[0]) + (event.values[1] * event.values[1]) + (event.values[2] * event.values[2]))

            //val speed = abs(linearAcceleration[0] + linearAcceleration[1] + linearAcceleration[2] - last_x - last_y - last_z) / diffTime * 10000

            //tv_mag.text = "Accelerometer Mag values: " + linearAcceleration[3] //+ " " + slidingWindow[9]

            addToWindow(linearAcceleration[3])
            //val xval = System.currentTimeMillis() / 1000.toDouble()//graphLastXValue += 0.1
            //mSeriesXaccel!!.appendData(DataPoint(xval, linear_acceleration[0].toDouble()), true, 50)
            //SeriesYaccel!!.appendData(DataPoint(xval, linear_acceleration[1].toDouble()), true, 50)
            //mSeriesZaccel!!.appendData(DataPoint(xval, linear_acceleration[2].toDouble()), true, 50)
            //mSeriesmagaccel!!.appendData(DataPoint(xval,linear_acceleration[3].toDouble()),true, 50)

            if (shakeDetected == 0) {
                if (linearAcceleration[3] > 50) {
                    shakeDetected = 1
                    counter = 0
                    slidingWindow[0] = 0.0f
                    slidingWindow[1] = 0.0f
                    slidingWindow[2] = 0.0f
                    slidingWindow[3] = 0.0f
                    slidingWindow[4] = 0.0f
                    slidingWindow[5] = 0.0f
                    slidingWindow[6] = 0.0f
                    slidingWindow[7] = 0.0f
                    slidingWindow[8] = 0.0f
                    slidingWindow[9] = 0.0f
                    tv_counter.text = counter.toString()
                    rb_freemode.isEnabled = false
                    rb_repmode.isEnabled = false
                    if(rb_repmode.isChecked) {
                        tv_status.text = "Rep Mode: Counter Started, Continue Pull Ups till the Beep"
                    } else if (rb_freemode.isChecked) {
                        tv_status.text = "Free Mode: Counter Started, Press stop to stop counting Pull Ups"
                    }

                    //text = "Speed: $speed"
                }
            }
            //last_x = linearAcceleration[0]
            //last_y = linearAcceleration[1]
            //last_z = linearAcceleration[2]
        }
        if ((curTime - lastSec) > 1000) {
            lastSec = curTime
            countPullups()
        }
    }

    private fun addToWindow(x: Float) {
        for (i in 1 until slidingWindow.size) {
            slidingWindow[i - 1] = slidingWindow[i]
        }
        slidingWindow[slidingWindow.size - 1] = x
    }

    fun radioCheck(view:View) {
        if (view == rb_freemode){
            et_repnumber.isEnabled = false
            bt_setrep.isEnabled = false
            counter = 0
            shakeDetected = 0
        } else if (view == rb_repmode) {
            et_repnumber.isEnabled = true
            bt_setrep.isEnabled = true
            counter = 0
            shakeDetected = 0
        }
    }

    fun setRep(view: View) {
        val rep = et_repnumber.text.toString()
        if(rep != "") {
            nmax = rep.toInt()
            bt_setrep.isEnabled = false
            et_repnumber.isEnabled = false
        }
    }

    private fun countPullups() {
        //val minimum = 0.0f
        //val maximum = 0.0f
        if (rb_freemode.isChecked && shakeDetected == 1) {
            if (slidingWindow.min()!! < 7.0f && slidingWindow.max()!! > 13.0f) {
                counter += 1
                tv_counter.text = counter.toString()
            }
        }

        else if (rb_repmode.isChecked && shakeDetected == 1) {
            if (nmax != 0) {
                if (slidingWindow.min()!! < 8.0f && slidingWindow.max()!! > 12.0f) {
                    counter += 1
                    tv_counter.text = counter.toString()
                    nmax -= 1
                }
            } else {
                beep.startTone(ToneGenerator.TONE_DTMF_1, 1000)
            }
        }
        //while (shakedetected == 1) {
        //
        //    if (linearAcceleration[3] > 20) {
        //        Toast.makeText(this, "Shake two Detected", Toast.LENGTH_SHORT)
        //    }
        //}
    }

    fun stop(view: View) {
        shakeDetected = 0
        slidingWindow[0] = 0.0f
        slidingWindow[1] = 0.0f
        slidingWindow[2] = 0.0f
        slidingWindow[3] = 0.0f
        slidingWindow[4] = 0.0f
        slidingWindow[5] = 0.0f
        slidingWindow[6] = 0.0f
        slidingWindow[7] = 0.0f
        slidingWindow[8] = 0.0f
        slidingWindow[9] = 0.0f
        rb_freemode.isEnabled = true
        rb_repmode.isEnabled = true
        tv_status.text = "Shake to Start the Counter"
        if (rb_repmode.isChecked) {
            bt_setrep.isEnabled = true
            et_repnumber.isEnabled = true
            et_repnumber.setText("")
        }
    }

    override fun onResume() {
        Log.d("tag","onResume")
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d("tag","onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager.unregisterListener(this)
        beep.release()
    }
}