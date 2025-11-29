package com.example.tapticapp.services;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.example.tapticapp.audio.YamnetAudioClassifier;
import com.example.tapticapp.config.AppConfig;
import com.example.tapticapp.core.Interpreter;
import com.example.tapticapp.network.BroadcastListener;
import com.example.tapticapp.network.BroadcastSender;
import com.example.tapticapp.notifications.TapticNotificationManager;

import java.util.List;

/**
 * Foreground service that runs audio classification continuously.
 */
public class AudioClassificationService extends Service {

    private static final String TAG = "AudioService";
    private static final int NOTIFICATION_ID = 9001;

    private YamnetAudioClassifier audioClassifier;
    private Interpreter interpreter;
    private AppConfig appConfig;
    private TapticNotificationManager notificationManager;
    private BroadcastSender broadcastSender;
    private BroadcastListener broadcastListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final IBinder binder = new LocalBinder();
    private ServiceCallback serviceCallback;

    public class LocalBinder extends Binder {
        public AudioClassificationService getService() {
            return AudioClassificationService.this;
        }
    }

    public interface ServiceCallback {
        void onAudioUpdate(List<Interpreter.DetectionResult> top3, double level);
    }

    public void setCallback(ServiceCallback callback) {
        this.serviceCallback = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        appConfig = AppConfig.getInstance(getApplicationContext());
        audioClassifier = new YamnetAudioClassifier(getApplicationContext());
        broadcastSender = new BroadcastSender();
        notificationManager = new TapticNotificationManager(getApplicationContext());

        interpreter = new Interpreter(
                getApplicationContext(),
                appConfig,
                broadcastSender,
                this::handleNotification);

        broadcastListener = new BroadcastListener(this::handleBroadcastEvent);
        broadcastListener.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notificationManager.createForegroundNotification("Listening..."),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, notificationManager.createForegroundNotification("Listening..."));
        }

        startAudioClassification();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        if (audioClassifier != null)
            audioClassifier.close();
        if (broadcastListener != null)
            broadcastListener.stop();
    }

    private void startAudioClassification() {
        audioClassifier.startListening((scores, labels, level) -> {
            List<Interpreter.DetectionResult> top3 = interpreter.onFrame(scores, labels, level);

            // Update foreground notification
            if (!top3.isEmpty()) {
                String topLabel = top3.get(0).label;
                notificationManager.updateForegroundNotification(NOTIFICATION_ID, topLabel);
            }

            // Update UI via callback
            if (serviceCallback != null) {
                mainHandler.post(() -> serviceCallback.onAudioUpdate(top3, level));
            }
        });
    }

    private void handleNotification(String label, double score, boolean isEmergency, boolean isLocal,
            String deviceName) {
        mainHandler.post(() -> {
            String emoji = appConfig.getNotificationEmoji();
            boolean playSound = appConfig.isPlaySound();
            boolean flashEmergency = appConfig.isFlashEmergency();

            notificationManager.showNotification(label, score, isEmergency, isLocal, deviceName, emoji, playSound);

            if (isEmergency && flashEmergency) {
                triggerEmergencyFlash();
            }
        });
    }

    private void handleBroadcastEvent(String label, String deviceName) {
        interpreter.handleBroadcastEvent(label, deviceName);
    }

    private void triggerEmergencyFlash() {
        // TODO: Implement overlay activity launch
    }
}
