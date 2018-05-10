/*
 *  Copyright 2018 AT&T
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.att.arocollector.attenuator;

import android.util.Log;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Created by Lihsin Shih on 6/14/17.
 * Place for Attenuation function utility method
 */

public class AttenuatorUtil {

    public static final String TAG = AttenuatorUtil.class.getSimpleName();
    public static final int DEFAULT_THROTTLE_SPEED = -1;
    private static AttenuatorUtil amInstance = new AttenuatorUtil();

    public AttenuatorUtil() {}

    public static AttenuatorUtil getInstance() {
        return amInstance;
    }

    public String notificationMessage() {
        return "Speed Control - DL: " +
                messageConvert(AttenuatorManager.getInstance().getThrottleDL())
                + " UL: "
                + messageConvert(AttenuatorManager.getInstance().getThrottleUL())
                ;
    }

    public String toastMessage(int kbps) {
        return messageConvert(kbps);
    }

    public String messageConvert(int kbps) {
        String message = "";
        if (kbps >= 1024 * 100 || kbps < 0) {
            message = " None";
        } else if (kbps > 1024) {
            message = " " + unitConvert(kbps) + " Mbps";
        } else {
            message = " " + kbps + " Kbps";
        }
        Log.i(TAG, "Throttle message : " + message);
        return message;
    }

    private String unitConvert(int kbps) {
        double mTemp = kbps / 1024;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(mTemp);

    }

}
