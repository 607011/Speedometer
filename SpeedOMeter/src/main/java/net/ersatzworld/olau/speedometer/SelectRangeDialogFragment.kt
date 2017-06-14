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

import android.support.v4.app.DialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker


class SelectRangeDialogFragment : DialogFragment() {
    private var loPicker: NumberPicker? = null
    private var hiPicker: NumberPicker? = null
    private var speedRangeSelectionCallback: ISpeedRangeSelectionListener? = null


    fun setOnSelectedSpeedRange(callback: ISpeedRangeSelectionListener) {
        speedRangeSelectionCallback = callback
    }


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.setCanceledOnTouchOutside(true)
        return inflater!!.inflate(R.layout.range_select_dialog, container)
    }


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val okButton = view!!.findViewById(R.id.okSpeedRangeButton) as Button
        okButton.setOnClickListener {
            val lo = Math.min(loPicker!!.value, hiPicker!!.value)
            val hi = Math.max(loPicker!!.value, hiPicker!!.value)
            speedRangeSelectionCallback!!.onSelectedSpeedRange(INTERVAL * lo, INTERVAL * hi)
            dismiss()
        }
        val cancelButton = view.findViewById(R.id.cancelSpeedRangeButton) as Button
        cancelButton.setOnClickListener { dismiss() }
        loPicker = view.findViewById(R.id.selectedLoSpeed) as NumberPicker
        loPicker!!.minValue = 0
        loPicker!!.maxValue = MAX_VALUES
        loPicker!!.value = arguments.getInt("lo") / INTERVAL
        loPicker!!.displayedValues = Array(MAX_VALUES + 1, { i -> (i * INTERVAL).toString() })
        hiPicker = view.findViewById(R.id.selectedHiSpeed) as NumberPicker
        hiPicker!!.minValue = 0
        hiPicker!!.maxValue = MAX_VALUES
        hiPicker!!.value = arguments.getInt("hi") / INTERVAL
        hiPicker!!.displayedValues = Array(MAX_VALUES + 1, { i -> (i * INTERVAL).toString() })
        dialog.setTitle(getString(R.string.select_range))
    }

    companion object {
        // private val TAG = SpeedometerView::class.java.simpleName;
        private val INTERVAL = 10
        private val MAX_SPEED = 300
        private val MAX_VALUES = MAX_SPEED / INTERVAL;

        fun newInstance(lo: Int, hi: Int): SelectRangeDialogFragment {
            val frag = SelectRangeDialogFragment()
            frag.arguments = Bundle()
            frag.arguments.putInt("lo", lo)
            frag.arguments.putInt("hi", hi)
            return frag
        }
    }


}
