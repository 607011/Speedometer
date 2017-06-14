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

package net.ersatzworld.olau.speedometer

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.view.View


class SpeedInterval(private val activity: AppCompatActivity, viewId: Int, defaultLo: Int, defaultHi: Int) : ISpeedRangeSelectionListener, View.OnLongClickListener {
    private var t0 = 0L
    private var dt = 0L
    private val stopwatchView: StopwatchView
    private val callback: ISpeedRangeSelectionListener?
    var lo = 0
        private set
    var hi = 0
        private set


    init {
        assert(activity is ISpeedRangeSelectionListener)
        callback = activity as ISpeedRangeSelectionListener
        val param = activity.resources.getResourceEntryName(viewId)
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        lo = prefs.getInt("$param-lo", defaultLo)
        hi = prefs.getInt("$param-hi", defaultHi)
        stopwatchView = activity.findViewById(viewId) as StopwatchView
        stopwatchView.setThresholds(lo, hi)
        stopwatchView.setOnLongClickListener(this)
        reset()
    }


    override fun onSelectedSpeedRange(lo: Int, hi: Int) {
        this.lo = lo
        this.hi = hi
        stopwatchView.setThresholds(lo, hi)
        val param = activity.resources.getResourceEntryName(stopwatchView.id)
        activity.getPreferences(Context.MODE_PRIVATE).edit()
                .putInt("$param-lo", lo)
                .putInt("$param-hi", hi)
                .apply()
        callback?.onSelectedSpeedRange(lo, hi)
    }


    override fun onLongClick(v: View): Boolean {
        val dialogFragment = SelectRangeDialogFragment.newInstance(lo, hi)
        dialogFragment.setOnSelectedSpeedRange(this)
        dialogFragment.show(activity.supportFragmentManager, "Speed range selection")
        return true
    }


    fun reset() {
        t0 = 0L
        dt = 0L
        stopwatchView.setTime(0f)
    }


    fun process(v: Float) {
        val t = System.currentTimeMillis()
        if (v <= lo.toFloat()) {
            reset()
        } else {
            if (v >= lo.toFloat() && t0 == 0L) {
                t0 = t
            }
            if (v >= hi.toFloat() && dt == 0L) {
                dt = t - t0
                stopwatchView.setTime(1e-3f * dt)
            }
        }
    }
}
