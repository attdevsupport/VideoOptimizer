/*
 *  Copyright 2014 AT&T
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


import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.att.arocollector.R;

/**
 * This class deliver the toast and Notification message up load stream from the app to the user
 */

public class ThrottleULBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = ThrottleULBroadcastReceiver.class.getSimpleName();
    private int notifyID = 1;
    private Notification.Builder mBuilder;
    private Context myContext; // pass to inner class
    private int uploadThrottleKbps = 0; //upstream throttle speed

    public ThrottleULBroadcastReceiver() {
        super();
    }
    @Override
    public void onReceive(Context context, Intent intent) {

        uploadThrottleKbps = intent.getIntExtra("ulms", AttenuatorUtil.DEFAULT_THROTTLE_SPEED);

        this.myContext = context;

        if (mBuilder == null) {
            mBuilder = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.icon)
                    .setContentTitle("Video Optimizor VPN Collector")
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentText(AttenuatorUtil.getInstance().notificationMessage());
        }

        Log.i(TAG, "Upload stream throttle for: " + uploadThrottleKbps + " kbps");
        AttenuatorManager.getInstance().setThrottleUL(uploadThrottleKbps);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder.setContentText(AttenuatorUtil.getInstance().notificationMessage());
        mNotificationManager.notify(notifyID, mBuilder.build());

        //From the Android source code, Toast.LENGTH_SHORT is defined 2 seconds.
        //Therefore, we delay sending the upload toast message 2 seconds.
        //But, it won't affect the real time we send the throttle signal.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String text = "Upload stream throttle for " +
                        AttenuatorUtil.getInstance().messageConvert(uploadThrottleKbps);
                Toast toast = Toast.makeText(ThrottleULBroadcastReceiver.this.myContext, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        },2000);

    }
}
