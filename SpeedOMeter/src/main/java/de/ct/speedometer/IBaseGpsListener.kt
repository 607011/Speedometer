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

import android.location.GpsStatus
import android.location.LocationListener
import android.location.Location

import android.os.Bundle

interface IBaseGpsListener : LocationListener, GpsStatus.Listener {
    override fun onLocationChanged(location: Location)
    override fun onProviderDisabled(provider: String)
    override fun onProviderEnabled(provider: String)
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle)
    override fun onGpsStatusChanged(event: Int)
}
