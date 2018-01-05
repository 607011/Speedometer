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


data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {

    fun absMax(o: Vector3) {
        x = Math.max(Math.abs(o.x), x)
        y = Math.max(Math.abs(o.y), y)
        z = Math.max(Math.abs(o.z), z)
    }


    fun overallMax(): Float {
        return Math.max(x, Math.max(y, z))
    }
}


// data class Vector4(var s: Float = 0f, var t: Float = 0f, var u: Float = 0f, var v: Float = 0f)

