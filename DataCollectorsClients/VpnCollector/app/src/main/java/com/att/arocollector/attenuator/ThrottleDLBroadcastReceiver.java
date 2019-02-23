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

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.att.arocollector.R;

/**
 * Created by Lindsay on 6/5/17.
 */

public class ThrottleDLBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = ThrottleDLBroadcastReceiver.class.getSimpleName();
    private int notifyID = 1;
    private Notification.Builder mBuilder;

    public ThrottleDLBroadcastReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int dlMs = intent.getIntExtra("dlms", AttenuatorUtil.DEFAULT_THROTTLE_SPEED);

        if (mBuilder == null) {
            mBuilder = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.icon)
                    .setContentTitle("Video Optimizor VPN Collector")
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentText(AttenuatorUtil.getInstance().notificationMessage());
        }
        Log.i(TAG, "Download stream speed throttle for: " + dlMs + " kbps");
        AttenuatorManager.getInstance().setThrottleDL(dlMs);
        String text = "Download stream throttle for " + AttenuatorUtil.getInstance().messageConvert(dlMs);
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP,0,0);
        toast.show();

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder.setContentText(AttenuatorUtil.getInstance().notificationMessage());
        mNotificationManager.notify(notifyID, mBuilder.build());
    }

}
