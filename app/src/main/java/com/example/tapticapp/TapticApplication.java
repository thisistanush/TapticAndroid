package com.example.tapticapp;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class TapticApplication extends Application {

    public static final String CHANNEL_ID_NORMAL = "taptic_normal";
    public static final String CHANNEL_ID_EMERGENCY = "taptic_emergency";
    public static final String CHANNEL_ID_SERVICE = "taptic_service";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            NotificationChannel normalChannel = new NotificationChannel(
                    CHANNEL_ID_NORMAL,
                    "Sound Detections",
                    NotificationManager.IMPORTANCE_DEFAULT);
            normalChannel.setDescription("Notifications for detected sounds");
            normalChannel.enableVibration(true);

            NotificationChannel emergencyChannel = new NotificationChannel(
                    CHANNEL_ID_EMERGENCY,
                    "Emergency Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            emergencyChannel.setDescription("Critical alerts for emergency sounds");
            emergencyChannel.enableVibration(true);
            emergencyChannel.setShowBadge(true);

            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID_SERVICE,
                    "Audio Monitoring",
                    NotificationManager.IMPORTANCE_LOW);
            serviceChannel.setDescription("Persistent notification while Taptic is listening");
            serviceChannel.setShowBadge(false);

            notificationManager.createNotificationChannel(normalChannel);
            notificationManager.createNotificationChannel(emergencyChannel);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }
}
