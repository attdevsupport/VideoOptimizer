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
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.att.arocollector.video.Orientation;
import com.att.arocollector.video.VideoCapture;
import com.att.arocollector.attenuator.AttenuatorUtil;

public class ScreenRecorderService extends Service {
    private static MediaProjectionManager smediaProjectionManager;
    private int notifyID = 1;
    private NotificationCompat.Builder mBuilder;
    public static final String CHANNEL_ID = "VPN Collector VPN Notification";

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
        Orientation videoOrient = ((Orientation)intent.getSerializableExtra("videoorientation")==null)
                ?(Orientation)intent.getSerializableExtra("videoorientation"):Orientation.PORTRAIT;
					intent.putExtra("code", resultCode);
					intent.putExtra("bitrate",8000000);
					intent.putExtra("screensize","1280x720");
					intent.putExtra("videoorientation",videoOrient);
        VideoCapture videoCapture = new VideoCapture(getApplicationContext(), window
                , mediaProjection, intent.getIntExtra("bitrate",0),
                intent.getStringExtra("screensize" ), videoOrient);
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
            manager.createNotificationChannel(serviceChannel);
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
