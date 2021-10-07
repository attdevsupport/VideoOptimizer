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
package com.att.arotracedata;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.att.arocollector.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AROCpuTempService extends AROMonitorService {

    public static final String TAG = AROCpuTempService.class.getSimpleName();
    private Handler mHandler;
    private ThermalInfoUtil thermalInfoUtil;
    private int notifyID = 2;
    private NotificationCompat.Builder mBuilder;
    public static final String CHANNEL_ID = "CPU_Temperature";

    public String getThermalStatus() {
        return thermalStatus;
    }

    public void setThermalStatus(String thermalStatus) {
        this.thermalStatus = thermalStatus;
    }

    private String thermalStatus = "";
    NotificationManager mNotificationManager;
    PowerManager pm;

    ExecutorService executor = Executors.newSingleThreadExecutor();
    protected File traceDir;

    @Override
    public void onCreate() {
        super.onCreate();
        thermalInfoUtil = new ThermalInfoUtil(executor);
        mHandler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNotificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(mNotificationManager);
        if (mBuilder == null) {
            mBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.icon)
                    .setAutoCancel(false);
            mBuilder.setContentTitle("CPU Temperature: " + " - " + " \u2103\n")
                    .setContentText(thermalStatus);
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(notifyID, mBuilder.build());
        }
        mHandler.post(getCpuTempNotification);
        pm = (PowerManager) getSystemService(getApplicationContext().POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.addThermalStatusListener(new PowerManager.OnThermalStatusChangedListener() {
                @Override
                public void onThermalStatusChanged(int status) {
                    Log.d(TAG, "Status:" + status);
                    String thermalStatus = getStatus(status);
                    setThermalStatus("Thermal status: " + thermalStatus);
                    Toast toast = Toast.makeText(getApplicationContext(), "Thermal status: " + getStatus(status), Toast.LENGTH_LONG);
                    toast.show();
                    sendTemperatureNotification(thermalStatus);
                    writeTraceLineToAROTraceFile(status + "", true);
                }
            });
            int currentTemp = pm.getCurrentThermalStatus();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private final Runnable getCpuTempNotification = new Runnable() {
        @Override
        public void run() {
            startCpuMeasurement();
            mHandler.postDelayed(getCpuTempNotification, 5000);
        }
    };

    public void startCpuMeasurement() {
        thermalInfoUtil.startCPUTemp(new ThermalInfoUtil.ThermalCallBack() {
            @Override
            public void callbackResult(ThermalInfoUtil.ResultCPUThermal result) {
                if (result != null) {
                    sendTemperatureNotification(result.getThermalLocal());
                } else {
                    mHandler.removeCallbacks(getCpuTempNotification);
                    Log.e(TAG, "no data");
                }
            }
        }, mHandler);
    }


    public void sendTemperatureNotification(double temperatureValue) {
        mBuilder.setContentTitle("CPU Temperature: " + Math.round(temperatureValue) + " \u2103\n");
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifyID, mBuilder.build());
    }

    public void sendTemperatureNotification(String thermalStatus) {
        mBuilder.setContentText(thermalStatus);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifyID, mBuilder.build());
    }


    public void stopCpuMeasurementTimer() {
        Log.d(TAG, "stopCpuMeasurementTimer()");
        if (thermalInfoUtil != null) {
            thermalInfoUtil.closeFile();
        }
        mHandler.removeCallbacks(getCpuTempNotification);
        mNotificationManager.cancelAll();
    }

    private void createNotificationChannel(NotificationManager mNotificationManager) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "VPN Collector CPU Temperature",
                    NotificationManager.IMPORTANCE_LOW);
            serviceChannel.setShowBadge(false);
            mNotificationManager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        stopCpuMeasurementTimer();
        super.onDestroy();
    }

    @Override
    protected void stopMonitor() {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private String getStatus(int status) {
        String statusString;
        switch (status) {
            case 0://none
                statusString = "No throttling";
                break;
            case 1://light
                statusString = "Light throttling. UX isn't impacted.";
                break;
            case 2://moderate
                statusString = "Moderate throttling. UX isn't greatly impacted.";
                break;
            case 3://severe
                statusString = "Severe throttling. UX is largely impacted.";
                break;
            case 4://critical
                statusString = "Platform has done everything to reduce power.";
                break;
            case 5://emergency
                statusString = "Key components in the platform are shutting down due to thermal conditions.";
                break;
            case 6://shutdown
                statusString = "Shutdown immediately.";
                break;
            default:
                statusString = "UNKNOWN";
        }
        return statusString;
    }

}
