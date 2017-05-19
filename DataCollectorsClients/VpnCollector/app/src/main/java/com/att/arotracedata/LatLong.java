/*
 *
 *   Copyright 2017 AT&T
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package com.att.arotracedata;

public class LatLong {

    double dLatitude;
    double dLongitude;

    public LatLong(double latitude, double longitude){
        this.dLatitude = latitude;
        this.dLongitude = longitude;
    }
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof LatLong){
            if (this == obj){
                return true;
            }

            LatLong latLong = (LatLong) obj;
            if (Double.doubleToLongBits(dLatitude) != Double.doubleToLongBits(latLong.dLatitude)){
                return false;
            }
            if (Double.doubleToLongBits(dLongitude) != Double.doubleToLongBits(latLong.dLongitude)){
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(dLatitude);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(dLongitude);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
