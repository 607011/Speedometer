/*

    Copyright (c) 2017-2018 Oliver Lau <ola@ct.de>, Heise Medien GmbH & Co. KG

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


import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.hardware.*
import android.location.Location
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast


class SpeedometerView : View, SensorEventListener {
    private var density = 0f
    private var minSpeed = 0
    private var maxSpeed = 0
    private var minorTickInterval = 0
    private var majorTickInterval = 0
    private var specialTick: MutableList<Int> = mutableListOf()
    private var doDrawGDiagrams = false
    private var topAccel = 0f
    private var topDecel = 0f
    private var topG = Vector3()
    private var gravity = Vector3()
    private var gv = AccelerationSequence(AccelerationSequenceLength)
    private var topA = Vector3()
    private var acceleration = Vector3()
    private var rawA = FloatArray(3, { 0f })
    private var av = AccelerationSequence(AccelerationSequenceLength)
    private var magneticField = Vector3()
    private var speed = 0.0
    private var topSpeed = 0.0
    private var lat = 0.0
    private var lon = 0.0
    private var startAngle = 0f
    private var availableAngle = 0f
    private var majorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var minorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var specialTickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var axisPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var needlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var needle2Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var speedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var topSpeedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var unitTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var speedoTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var locTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var topAccelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var topDecelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lateralAccelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var topAccelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var topDecelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gBarBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var aPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var orientationHelperInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var orientationHelperOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var orientationCircleInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var orientationCircleOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var diagramPath = Path()
    private var gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            reset()
            resetListener?.onReset()
            return true
        }
    })
    private var resetListener: IOnResetListener? = null
    private var t0 = 0L
    private var dt = 0L
    private var rotationMatrix = FloatArray(9, { 0f })
    private var orientationAngles = FloatArray(3, { 0f })
    private var rotationVector = FloatArray(5, { 0f })
    private var hasSetOff = false


    constructor(context: Context) : super(context) {
        init(context)
    }


    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }


    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context)
    }


    private fun init(context: Context) {
        density = resources.displayMetrics.density
        doDrawGDiagrams = true
        speed = 0.0
        minSpeed = 0
        maxSpeed = 300
        minorTickInterval = 10
        majorTickInterval = 40
        startAngle = 0f
        availableAngle = Math.PI.toFloat()
        minorTickPaint.strokeWidth = 3f * density
        minorTickPaint.style = Paint.Style.STROKE
        minorTickPaint.color = ContextCompat.getColor(context, R.color.minorTick)
        majorTickPaint.strokeWidth = 8f * density
        majorTickPaint.style = Paint.Style.STROKE
        majorTickPaint.color = ContextCompat.getColor(context, R.color.majorTick)
        specialTickPaint.strokeWidth = 8f * density
        specialTickPaint.style = Paint.Style.STROKE
        specialTickPaint.color = ContextCompat.getColor(context, R.color.specialTick)
        axisPaint.style = Paint.Style.FILL
        axisPaint.color = ContextCompat.getColor(context, R.color.axis)
        needlePaint.strokeWidth = 18.5f * density
        needlePaint.style = Paint.Style.STROKE
        needlePaint.color = ContextCompat.getColor(context, R.color.speed)
        needle2Paint.strokeWidth = 18.5f * density
        needle2Paint.style = Paint.Style.STROKE
        needle2Paint.color = ContextCompat.getColor(context, R.color.topSpeed)
        borderPaint.strokeWidth = 5f * density
        borderPaint.style = Paint.Style.STROKE
        borderPaint.color = ContextCompat.getColor(context, R.color.tachoBorder)
        speedTextPaint.textAlign = Paint.Align.CENTER
        speedTextPaint.textSize = 52f * density
        speedTextPaint.isLinearText = true
        speedTextPaint.color = ContextCompat.getColor(context, R.color.tachoText)
        speedTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        topSpeedTextPaint.textAlign = Paint.Align.CENTER
        topSpeedTextPaint.textSize = 18f * density
        topSpeedTextPaint.isLinearText = true
        topSpeedTextPaint.color = ContextCompat.getColor(context, R.color.topSpeedText)
        topSpeedTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        unitTextPaint.textAlign = Paint.Align.CENTER
        unitTextPaint.textSize = 12f * density
        unitTextPaint.isLinearText = true
        unitTextPaint.color = ContextCompat.getColor(context, R.color.tachoText)
        speedoTextPaint.textAlign = Paint.Align.CENTER
        speedoTextPaint.textSize = 24f * density
        speedoTextPaint.isLinearText = true
        speedoTextPaint.color = ContextCompat.getColor(context, R.color.tachoText)
        speedoTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        locTextPaint.textAlign = Paint.Align.CENTER
        locTextPaint.textSize = 8f * density
        locTextPaint.isLinearText = true
        locTextPaint.color = ContextCompat.getColor(context, R.color.tachoText)
        topAccelTextPaint.textAlign = Paint.Align.LEFT
        topAccelTextPaint.textSize = 16f * density
        topAccelTextPaint.isLinearText = true
        topAccelTextPaint.color = ContextCompat.getColor(context, R.color.topAccel)
        topDecelTextPaint.textAlign = Paint.Align.RIGHT
        topDecelTextPaint.textSize = 16f * density
        topDecelTextPaint.isLinearText = true
        topDecelTextPaint.color = ContextCompat.getColor(context, R.color.topDecel)
        gBarPaint.style = Paint.Style.FILL
        gBarPaint.strokeWidth = 2 * density
        gBarPaint.color = ContextCompat.getColor(context, R.color.gBar)
        topAccelPaint.style = Paint.Style.FILL
        topAccelPaint.strokeWidth = 3.3f * density
        topAccelPaint.color = ContextCompat.getColor(context, R.color.topAccel)
        topDecelPaint.style = Paint.Style.FILL
        topDecelPaint.strokeWidth = 3.3f * density
        topDecelPaint.color = ContextCompat.getColor(context, R.color.topDecel)
        lateralAccelTextPaint = Paint()
        lateralAccelTextPaint.textAlign = Paint.Align.CENTER
        lateralAccelTextPaint.textSize = 16f * density
        lateralAccelTextPaint.isLinearText = true
        lateralAccelTextPaint.color = ContextCompat.getColor(context, R.color.lateralAccel)
        gBarBorderPaint.style = Paint.Style.STROKE
        gBarBorderPaint.strokeWidth = 1.54f * density
        gBarBorderPaint.color = ContextCompat.getColor(context, R.color.gBarBorder)
        gPaint.strokeWidth = 1.2f * density
        gPaint.style = Paint.Style.STROKE
        gPaint.color = ContextCompat.getColor(context, R.color.g)
        aPaint.strokeWidth = .78f * density
        aPaint.style = Paint.Style.STROKE
        aPaint.color = ContextCompat.getColor(context, R.color.a)
        orientationHelperInnerPaint.strokeWidth = 1.1f * density
        orientationHelperInnerPaint.style = Paint.Style.STROKE
        orientationHelperInnerPaint.color = ContextCompat.getColor(context, R.color.orientationHelperInner)
        orientationHelperInnerPaint.pathEffect = DashPathEffect(floatArrayOf(2.5f * density, 2.5f * density), 0f)
        orientationHelperOuterPaint.strokeWidth = 1.1f * density
        orientationHelperOuterPaint.style = Paint.Style.STROKE
        orientationHelperOuterPaint.color = ContextCompat.getColor(context, R.color.orientationHelperOuter)
        orientationHelperOuterPaint.pathEffect = DashPathEffect(floatArrayOf(5f * density, 5f * density), 0f)
        orientationCircleInnerPaint.style = Paint.Style.FILL
        orientationCircleInnerPaint.color = ContextCompat.getColor(context, R.color.orientationHelperInner)
        orientationCircleOuterPaint.style = Paint.Style.FILL
        orientationCircleOuterPaint.color = ContextCompat.getColor(context, R.color.orientationHelperOuter)
        reset()
    }


    fun setOnResetListener(listener: IOnResetListener) {
        resetListener = listener
    }


    fun removeSpecialTicks() {
        specialTick.clear()
        invalidate()
    }


    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> setG(event.values)
            Sensor.TYPE_ACCELEROMETER -> setA(event.values)
            Sensor.TYPE_MAGNETIC_FIELD -> setMF(event.values, event.timestamp)
            Sensor.TYPE_ROTATION_VECTOR -> setRot(event.values)
        }
    }


    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.d("ACCURACY", "$accuracy")
    }


    fun reset() {
        speed = 0.0
        topG = Vector3(x=0f, y=0f, z=0f)
        topA = Vector3(x=0f, y=0f, z=0f)
        gravity = Vector3(x=0f, y=0f, z=0f)
        acceleration = Vector3(x=0f, y=0f, z=0f)
        gv.reset()
        av.reset()
        topSpeed = 0.0
        topAccel = 0f
        topDecel = 0f
        setBackgroundResource(R.drawable.tacho_bg)
        invalidate()
    }


    fun setSmoothingAlpha(alpha: Float) {
        gv.alpha = alpha
        av.alpha = alpha
    }


    private fun setG(g: FloatArray) {
        g[0] /= SensorManager.GRAVITY_EARTH
        g[1] /= SensorManager.GRAVITY_EARTH
        g[2] /= SensorManager.GRAVITY_EARTH
        gv.add(g)
        gravity = Vector3(x=gv.last.x, y=gv.last.z, z=gv.last.z)
        if (gravity.z < 0f) {
            topAccel = Math.max(topAccel, -gravity.z)
        } else {
            topDecel = Math.max(topDecel, gravity.z)
        }
        topG.absMax(gravity)
        invalidate()
    }


    private fun setA(a: FloatArray) {
        av.add(a)
        acceleration = Vector3(x=av.last.x, y=av.last.z, z=av.last.z)
        rawA = a.copyOf()
        topA.absMax(acceleration)
        invalidate()
    }


    private fun setMF(magnetometerReading: FloatArray, timestamp: Long) {
        magneticField = Vector3(x=magnetometerReading[0], y=magnetometerReading[1], z=magnetometerReading[2])
        SensorManager.getRotationMatrix(rotationMatrix, null, rawA, magnetometerReading)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        dt = timestamp - t0
        t0 = timestamp
    }


    private fun setRot(a: FloatArray) {
        rotationVector = a.copyOf()
        invalidate()
    }


    fun setLocation(loc: Location) {
        lat = loc.latitude
        lon = loc.longitude
    }


    fun setSpeed(speed: Float) {
        var v = speed.toDouble()
        if (v > maxSpeed) {
            v = maxSpeed.toDouble()
        }
        else if (v < minSpeed) {
            v = minSpeed.toDouble()
        }
        this.speed = v
        if (v > topSpeed) {
            topSpeed = v
        }
        invalidate()
    }


    fun setOff() {
        hasSetOff = true
        setBackgroundResource(R.drawable.tacho_bg_ready)
        invalidate()
    }


    private fun signalReady() {
        Toast.makeText(context, "Ready for action!", Toast.LENGTH_SHORT).show()
    }


    fun addSpecialTick(tick: Int) {
        if (tick != 0 && tick !in specialTick) {
            specialTick.add(tick)
        }
        invalidate()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
        var width = if (widthMode == View.MeasureSpec.EXACTLY || widthMode == View.MeasureSpec.AT_MOST)
            widthSize
        else
            -1
        var height = if (heightMode == View.MeasureSpec.EXACTLY || heightMode == View.MeasureSpec.AT_MOST)
            heightSize
        else
            -1
        if (height >= 0 && width >= 0) {
            width = Math.min(widthSize, heightSize)
            height = 3 * width / 4
        } else {
            width = 0
            height = 0
        }
        setMeasuredDimension(width, height)
        val r = .5 * width
        val d = height - r
        val h = Math.sqrt(r * r + d * d)
        val extraAngle = rad2deg(Math.asin(d / h))
        startAngle = -180 - extraAngle.toFloat()
        availableAngle = 180 + 2 * extraAngle.toFloat()
    }


    private fun speedToAngle(speed: Double): Double {
        return startAngle + availableAngle * (speed - minSpeed) / (maxSpeed - minSpeed)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = .5f * canvas.width
        val cy = cx
        val r = .93f * cx
        val r2 = .91f * r
        val r3 = .90f * r

        // draw g diagrams
        if (doDrawGDiagrams) {
            val height = 20f * density
            val x0 = cx - .5f * AccelerationSequenceLength * density
            var ys: Float
            var y0: Float
            diagramPath.reset()
            ys = height / Math.abs(topG.overallMax())
            y0 = cy - 1.5f * height * 2.5f
            diagramPath.moveTo(x0, gv[0].x + y0)
            for (i in 1 until gv.size) {
                diagramPath.lineTo(x0 + i * density, y0 + gv[i].x * ys)
            }
            y0 = cy - .5f * height * 2.5f
            diagramPath.moveTo(x0, gv[0].y + y0)
            for (i in 1 until gv.size) {
                diagramPath.lineTo(x0 + i * density, y0 + gv[i].y * ys)
            }
            y0 = cy + .5f * height * 2.5f
            diagramPath.moveTo(x0, gv[0].z + y0)
            for (i in 1 until gv.size) {
                diagramPath.lineTo(x0 + i * density, y0 + gv[i].z * ys)
            }
            canvas.drawPath(diagramPath, gPaint)
            diagramPath.reset()
            ys = height / Math.abs(topA.overallMax())
            y0 = cy - 1.5f * height * 2.5f
            diagramPath.moveTo(x0, av.current.x + y0)
            for (i in 1 until av.size) {
                diagramPath.lineTo(x0 + i * density, y0 + av[i].x * ys)
            }
            y0 = cy - .5f * height * 2.5f
            diagramPath.moveTo(x0, av.current.y + y0)
            for (i in 1 until av.size) {
                diagramPath.lineTo(x0 + i * density, y0 + (av[i].y - SensorManager.GRAVITY_EARTH) * ys)
            }
            y0 = cy + .5f * height * 2.5f
            diagramPath.moveTo(x0, av.current.z + y0)
            for (i in 1 until av.size) {
                diagramPath.lineTo(x0 + x * density, y0 + av[i].z * ys)
            }
            canvas.drawPath(diagramPath, aPaint)
        }

        // draw orientation helper
        val rOrientationHelper = 30f * density
        val r2OrientationHelper = 10.2f * density
        val r3OrientationHelper = r2OrientationHelper * .9f
        val r4OrientationHelper = rOrientationHelper * .1f
        val cxOrientationHelper = cx + 50f * density
        val cyOrientationHelper = cx + 40f * density
        canvas.drawCircle(
                cxOrientationHelper,
                cyOrientationHelper,
                rOrientationHelper,
                orientationHelperOuterPaint)
        canvas.drawCircle(
                cxOrientationHelper,
                cyOrientationHelper,
                r2OrientationHelper,
                orientationHelperInnerPaint)
        val xdev = acceleration.x - gravity.x
        val ydev = acceleration.z - gravity.z
        canvas.drawCircle(
                cxOrientationHelper + xdev * r4OrientationHelper,
                cyOrientationHelper + ydev * r4OrientationHelper,
                r3OrientationHelper,
                if (sqr(xdev * r4OrientationHelper) + sqr(ydev * r4OrientationHelper) < sqr(r2OrientationHelper / density * 1.1f))
                    orientationCircleInnerPaint
                else
                    orientationCircleOuterPaint)
        canvas.drawText(
                "%.1fg".format(Math.abs(xdev) / SensorManager.GRAVITY_EARTH),
                cxOrientationHelper,
                cyOrientationHelper - rOrientationHelper * 1.3f,
                lateralAccelTextPaint)

        // draw border
        val arcW = canvas.width - borderPaint.strokeWidth
        val arcH = arcW
        val arcX0 = borderPaint.strokeWidth
        val arcY0 = borderPaint.strokeWidth
        canvas.drawArc(arcX0, arcY0, arcW, arcH, startAngle, availableAngle, false, borderPaint)

        // draw ticks
        var spd = minSpeed
        while (spd <= maxSpeed) {
            if (spd !in specialTick && (spd - minSpeed) % majorTickInterval != 0) {
                val angle = speedToAngle(spd.toDouble())
                val dx = Math.cos(deg2rad(angle)).toFloat()
                val dy = Math.sin(deg2rad(angle)).toFloat()
                canvas.drawLine(
                        cx + r2 * dx,
                        cy + r2 * dy,
                        cx + r * dx,
                        cy + r * dy,
                        minorTickPaint)
            }
            spd += minorTickInterval
        }
        val d = .77f * r
        spd = minSpeed
        while (spd <= maxSpeed) {
            val angle = speedToAngle(spd.toDouble())
            val dx = Math.cos(deg2rad(angle)).toFloat()
            val dy = Math.sin(deg2rad(angle)).toFloat()
            if (spd !in specialTick) {
                canvas.drawLine(
                        cx + r3 * dx,
                        cy + r3 * dy,
                        cx + r * dx,
                        cy + r * dy,
                        majorTickPaint)
            }
            canvas.drawText(
                    "$spd",
                    cx + d * dx,
                    cy + d * dy - .5f * (speedoTextPaint.descent() + speedoTextPaint.ascent()),
                    speedoTextPaint)
            spd += majorTickInterval
        }
        for (tick in specialTick) {
            val angle = speedToAngle(tick.toDouble())
            val dx = Math.cos(deg2rad(angle)).toFloat()
            val dy = Math.sin(deg2rad(angle)).toFloat()
            canvas.drawLine(
                    cx + r3 * dx,
                    cy + r3 * dy,
                    cx + r * dx,
                    cy + r * dy,
                    specialTickPaint)
        }

        // draw speed
        canvas.drawText(
                "${speed.toInt()}",
                cx,
                cy - 40f * density,
                speedTextPaint)
        canvas.drawText(
                UnitText,
                cx,
                cy - 40f * density - 52f * density,
                unitTextPaint)

        if (topSpeed > .1f) {
            canvas.drawText(
                    "%.1f".format(topSpeed),
                    cx,
                    cy + .5f * density,
                    topSpeedTextPaint)
            if (topSpeed > speed) {
                val offset = 19f * density
                canvas.drawArc(
                        arcX0 + offset,
                        arcY0 + offset,
                        arcW - offset,
                        arcH - offset,
                        speedToAngle(topSpeed).toFloat() - .5f,
                        1f,
                        false,
                        needle2Paint)
            }
        }

        // draw g bar
        val barW = 17 * density
        val barH = 100 * density
        val pad = 2 * density
        val xBarOffset = -73 * density
        val yBarOffset = -58 * density + barH
        val maxG = 1f
        canvas.drawRect(
                cx + xBarOffset,
                cy + yBarOffset - barH,
                cx + xBarOffset + barW,
                cy + yBarOffset,
                gBarBorderPaint)
        val z = gravity.z / maxG * (barH - 2 * pad)
        if (z > 0f) {
            canvas.drawRect(
                    cx + xBarOffset + pad,
                    cy + yBarOffset - z,
                    cx + xBarOffset - pad + barW / 2,
                    cy + yBarOffset - pad,
                    gBarPaint)
        } else {
            canvas.drawRect(
                    cx + xBarOffset + pad + barW / 2,
                    cy + yBarOffset + z,
                    cx + xBarOffset - pad + barW,
                    cy + yBarOffset - pad,
                    gBarPaint)
        }
        if (topDecel > .01f) {
            val gMaxY = cy + (yBarOffset - topDecel / maxG * (barH - 2 * pad))
            canvas.drawLine(
                    cx + xBarOffset + pad,
                    gMaxY,
                    cx + xBarOffset - pad + barW / 2,
                    gMaxY,
                    topDecelPaint)
            canvas.drawText(
                    "%.1fg".format(topDecel),
                    cx + xBarOffset - pad - 5f * density,
                    gMaxY,
                    topDecelTextPaint)
        }
        if (topAccel > .01f) {
            val gMaxY = cy + (yBarOffset - topAccel / maxG * (barH - 2 * pad))
            canvas.drawLine(
                    cx + xBarOffset + pad + barW / 2,
                    gMaxY,
                    cx + xBarOffset - pad + barW,
                    gMaxY,
                    topAccelPaint)
            canvas.drawText(
                    "%.1fg".format(topAccel),
                    cx + (xBarOffset - pad + barW) + 7f * density,
                    gMaxY,
                    topAccelTextPaint)
        }

        // draw MG info
/*
        canvas.drawText(
                "%.3f".format(rotationVector[0]),
                cx + cx / 3,
                cy - 40 * density - 35f * density,
                locTextPaint)
        canvas.drawText(
                "%.3f".format(rotationVector[1]),
                cx + cx / 3,
                cy - 40 * density - 25f * density,
                locTextPaint)
        canvas.drawText(
                "%.3f".format(rotationVector[2]),
                cx + cx / 3,
                cy - 40 * density - 15f * density,
                locTextPaint)
        canvas.drawText(
                "%.3f".format(rotationVector[3]),
                cx + cx / 3,
                cy - 40 * density - 5f * density,
                locTextPaint)
        canvas.drawText(
                "%.3f".format(rotationVector[4]),
                cx + cx / 3,
                cy - 40 * density + 5f * density,
                locTextPaint)
        canvas.drawText(
                "${dt / 1000000L} ms",
                cx + cx / 3,
                cy - 40 * density + 20f * density,
                locTextPaint)
*/
        canvas.drawText(
                "%.1f".format(orientationAngles[0] / Math.PI * 180),
                cx + cx / 3,
                cy - 40 * density - 37.5f * density,
                locTextPaint)
        canvas.drawText(
                "%.1f".format(orientationAngles[1] / Math.PI * 180),
                cx + cx / 3,
                cy - 40 * density - 25f * density,
                locTextPaint)
        canvas.drawText(
                "%.1f".format(orientationAngles[2] / Math.PI * 180),
                cx + cx / 3,
                cy - 40 * density - 12.5f * density,
                locTextPaint)
        canvas.drawText(
                "${dt / 1000000L} ms",
                cx + cx / 3,
                cy - 40 * density + 7.5f * density,
                locTextPaint)


        // draw lat/lon
        canvas.drawText(
                "lat: %.6f°".format(lat),
                cx - cx / 3,
                cy + 40 * density + 25f * density,
                locTextPaint)
        canvas.drawText(
                "lon: %.6f°".format(lon),
                cx - cx / 3,
                cy + 40 * density + 37.5f * density,
                locTextPaint)

        // draw "needle"
        if (speed > .1f) {
            val offset = 16f * density
            canvas.drawArc(
                    arcX0 + offset,
                    arcY0 + offset,
                    arcW - offset,
                    arcH - offset,
                    startAngle,
                    speedToAngle(speed).toFloat() - startAngle,
                    false,
                    needlePaint)
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }


    companion object {
        private val TAG = SpeedometerView::class.java.simpleName
        private val AccelerationSequenceLength = 200
        private val UnitText = "km/h"
        private fun deg2rad(angle: Double): Double {
            return (Math.PI * angle / 180)
        }
        private fun rad2deg(angle: Double): Double {
            return (180 * angle / Math.PI)
        }
        private fun sqr(x: Float): Float {
            return x * x
        }
    }

}
