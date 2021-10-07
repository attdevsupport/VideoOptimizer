package com.att.arocollector;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.att.arocollector.utils.BundleKeyUtil;
import com.att.arocollector.video.Orientation;
import com.att.arocollector.video.VideoCapture;
import com.att.arocollector.attenuator.AttenuatorUtil;
import com.att.arocollector.video.VideoOption;

public class ScreenRecorderService extends Service {
    private static MediaProjectionManager smediaProjectionManager;
    private static final String CHANNEL_ID = "VPN Collector VPN Notification";
    private static String TAG = ScreenRecorderService.class.getSimpleName();
    private int notifyID = 1;
    private NotificationCompat.Builder mBuilder;

    public static void setMediaProjection(MediaProjectionManager mediaProjectionManager) {
        if (mediaProjectionManager == null) {
            return;
        }
        smediaProjectionManager =  mediaProjectionManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent notificationIntent = new Intent(this, AROCollectorActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);
        createNotificationChannel();

        if (mBuilder == null) {
        	String notification = AttenuatorUtil.getInstance().notificationMessage() + "\n" + "Screen Recording in Progress";
            mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.icon)
                    .setContentTitle("Video Optimizer VPN Collector")
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(notification))
                    .setContentText(notification);
        }
        startForeground(notifyID,mBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        WindowManager window = (WindowManager) getSystemService(WINDOW_SERVICE);
        int resultCode = intent.getIntExtra("code",-1);
        MediaProjection mediaProjection = smediaProjectionManager.getMediaProjection(resultCode,  intent);
        int bitRate = intent.getIntExtra(BundleKeyUtil.BIT_RATE,0);
        String screenSize = intent.getStringExtra(BundleKeyUtil.SCREEN_SIZE);
        Orientation videoOrient = (Orientation) intent.getSerializableExtra(BundleKeyUtil.VIDEO_ORIENTATION);
        VideoCapture videoCapture = new VideoCapture(
                getApplicationContext(),
                window,
                mediaProjection,
                bitRate,
                screenSize,
                videoOrient
        );
        videoCapture.start();

        return super.onStartCommand(intent, flags, startId);

    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "VPN Collector",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if(manager!=null)
                manager.createNotificationChannel(serviceChannel);
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
