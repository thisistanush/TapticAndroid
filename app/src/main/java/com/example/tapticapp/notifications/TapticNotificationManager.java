package com.example.tapticapp.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import com.example.tapticapp.MainActivity;
import com.example.tapticapp.R;
import com.example.tapticapp.TapticApplication;

/**
 * Manages creation and display of Android notifications for sound detections.
 */
public class TapticNotificationManager {

    private static final int NOTIFICATION_ID_BASE = 1000;
    private static int notificationIdCounter = NOTIFICATION_ID_BASE;
    private final Context context;
    private final NotificationManager notificationManager;

    public TapticNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void showNotification(String label, double score, boolean isEmergency, boolean isLocal, String deviceName,
            String emoji, boolean playSound) {
        String channelId = isEmergency ? TapticApplication.CHANNEL_ID_EMERGENCY : TapticApplication.CHANNEL_ID_NORMAL;

        String title = emoji != null && !emoji.isEmpty() ? "Taptic " + emoji : "Taptic";
        int percentage = (int) (score * 100);
        String source = isLocal ? "This Device" : (deviceName != null ? deviceName : "Remote Device");
        String text = source + " • " + label + " (" + percentage + "%)";

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(isEmergency ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(isEmergency ? NotificationCompat.CATEGORY_ALARM : NotificationCompat.CATEGORY_STATUS);

        if (playSound) {
            Uri soundUri = RingtoneManager.getDefaultUri(
                    isEmergency ? RingtoneManager.TYPE_ALARM : RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(soundUri);
        }

        notificationManager.notify(notificationIdCounter++, builder.build());
    }

    public Notification createForegroundNotification(String soundLabel) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, TapticApplication.CHANNEL_ID_SERVICE)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Taptic is listening")
                .setContentText("Now hearing: " + soundLabel)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    public void updateForegroundNotification(int notificationId, String soundLabel) {
        Notification notification = createForegroundNotification(soundLabel);
        notificationManager.notify(notificationId, notification);
    }
}
