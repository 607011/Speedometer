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


import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.SensorManager
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View


class StopwatchView : View {
    private var infoTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var infoText = ""
    private var t = 0f
    private var g = Float.NaN
    private var density = 0f
    private var lo = 0
    private var hi = 0


    constructor(context: Context) : super(context) {
        init()
    }


    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }


    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }


    private fun init() {
        density = resources.displayMetrics.density
        infoTextPaint.textAlign = Paint.Align.CENTER
        infoTextPaint.textSize = 12f * density // TODO: calculate size from canvas size
        infoTextPaint.isLinearText = true
        infoTextPaint.color = ContextCompat.getColor(context, R.color.tachoText)
        timeTextPaint.textAlign = Paint.Align.CENTER
        timeTextPaint.textSize = 42f * density // TODO: calculate size from canvas size
        timeTextPaint.isLinearText = true
        timeTextPaint.color = ContextCompat.getColor(context, R.color.tachoText)
        timeTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        gTextPaint.textAlign = Paint.Align.CENTER
        gTextPaint.textSize = 18f * density // TODO: calculate size from canvas size
        gTextPaint.isLinearText = true
        gTextPaint.color = ContextCompat.getColor(context, R.color.tachoText)
        gTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }


    private fun reset() {
        t = 0f
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
            width = Math.min(height, width) / 2 - 5
            height = 3 * width / 4 - 50
        } else {
            width = 0
            height = 0
        }
        setMeasuredDimension(width, height)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val x = .5f * canvas.width
        canvas.drawText(infoText, x, .2f * canvas.height, infoTextPaint)
        canvas.drawText(if (t > 0) "%.1f s".format(t) else resources.getString(R.string.dummy), x, .65f * canvas.height, timeTextPaint)
        canvas.drawText(if (g.isNaN()) resources.getString(R.string.dummy) else "%.2f g".format(g), x, .86f * canvas.height, gTextPaint)
    }


    fun setThresholds(lo: Int, hi: Int) {
        this.lo = lo
        this.hi = hi
        infoText = "%d - %d km/h".format(lo, hi)
        reset()
    }


    fun setTime(t: Float) {
        this.t = t
        g = if (t > 0)
            (hi - lo).toFloat() / 3.6f / t / SensorManager.GRAVITY_EARTH
        else
            Float.NaN
        invalidate()
    }

}
