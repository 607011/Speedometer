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


class AccelerationSequence(val size: Int) {

    private var cursor = 0
    private var d = Array(size, { Vector3() })

    var alpha: Float = 0f
    var empty: Boolean = true
        private set
    var last: Vector3 = Vector3()
        private set
    val current: Vector3 = d[cursor % size]


    operator fun get(index: Int): Vector3 {
        return d[(index + cursor) % size]
    }


    fun add(e: FloatArray) {
        if (empty) {
            empty = false
        } else {
            last.x = alpha * last.x + (1 - alpha) * e[0]
            last.y = alpha * last.y + (1 - alpha) * e[1]
            last.z = alpha * last.z + (1 - alpha) * e[2]
        }
        d[cursor++] = Vector3(x=last.x, y=last.y, z=last.z)
        if (cursor >= size) {
            cursor = 0
        }
    }


    fun reset() {
        cursor = 0
        empty = true
        d.forEachIndexed { idx, _ -> d[idx] = Vector3(x=0f, y=0f, z=0f) }
    }

}
