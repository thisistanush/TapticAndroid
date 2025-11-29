package com.example.tapticapp.core;

import android.content.Context;
import android.util.Log;
import com.example.tapticapp.config.AppConfig;
import com.example.tapticapp.network.BroadcastSender;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interpreter connects audio classification, UI, and network broadcasting.
 */
public class Interpreter {

    private static final String TAG = "Interpreter";
    private static final long COOLDOWN_MS = 5000;
    public static final double ALPHA = 0.7; // Smoothing factor

    private final AppConfig appConfig;
    private final BroadcastSender broadcastSender;
    private final NotificationCallback notificationCallback;
    private final Map<String, Long> lastNotifyTime = new HashMap<>();

    public interface NotificationCallback {
        void onNotification(String label, double score, boolean isEmergency, boolean isLocal, String deviceName);
    }

    public static class DetectionResult {
        public final String label;
        public final double score;
        public final boolean isEmergency;

        public DetectionResult(String label, double score, boolean isEmergency) {
            this.label = label;
            this.score = score;
            this.isEmergency = isEmergency;
        }
    }

    public Interpreter(Context context, AppConfig appConfig, BroadcastSender broadcastSender,
            NotificationCallback callback) {
        this.appConfig = appConfig;
        this.broadcastSender = broadcastSender;
        this.notificationCallback = callback;
    }

    public List<DetectionResult> onFrame(float[] scores, String[] labels, double level) {
        if (scores == null || scores.length == 0)
            return new ArrayList<>();

        List<Integer> top3Indices = findTop3Indices(scores);
        List<DetectionResult> results = new ArrayList<>();

        for (int index : top3Indices) {
            String label = getLabel(labels, index);
            double score = scores[index];
            boolean isEmergency = appConfig.isEmergencyLabel(label);
            results.add(new DetectionResult(label, score, isEmergency));
        }

        // Check notifications
        for (DetectionResult result : results) {
            maybeNotify(result.label, result.score, result.isEmergency, true, null);
        }

        return results;
    }

    public void handleBroadcastEvent(String eventLabel, String deviceName) {
        boolean isEmergency = appConfig.isEmergencyLabel(eventLabel);
        maybeNotify(eventLabel, 1.0, isEmergency, false, deviceName);
    }

    private void maybeNotify(String label, double score, boolean isEmergency, boolean isLocal, String deviceName) {
        // Check if confidence meets threshold
        if (score < appConfig.getNotifyThreshold())
            return;

        // Check cooldown
        long now = System.currentTimeMillis();
        Long lastTime = lastNotifyTime.get(label);
        if (lastTime != null && (now - lastTime) < COOLDOWN_MS)
            return;
        lastNotifyTime.put(label, now);

        // Broadcast if local and enabled
        if (isLocal && appConfig.isBroadcastSendEnabled(label)) {
            broadcastSender.sendEvent(label);
        }

        // Trigger notification
        if (notificationCallback != null) {
            notificationCallback.onNotification(label, score, isEmergency, isLocal, deviceName);
        }
    }

    private List<Integer> findTop3Indices(float[] scores) {
        List<Integer> indices = new ArrayList<>();
        if (scores.length < 3) {
            for (int i = 0; i < scores.length; i++)
                indices.add(i);
            return indices;
        }

        int best1 = 0, best2 = 1, best3 = 2;

        // Initial sort of first 3
        if (scores[best2] > scores[best1]) {
            int t = best1;
            best1 = best2;
            best2 = t;
        }
        if (scores[best3] > scores[best1]) {
            int t = best1;
            best1 = best3;
            best3 = best2;
            best2 = t;
        } else if (scores[best3] > scores[best2]) {
            int t = best2;
            best2 = best3;
            best3 = t;
        }

        for (int i = 3; i < scores.length; i++) {
            float current = scores[i];
            if (current > scores[best1]) {
                best3 = best2;
                best2 = best1;
                best1 = i;
            } else if (current > scores[best2]) {
                best3 = best2;
                best2 = i;
            } else if (current > scores[best3]) {
                best3 = i;
            }
        }

        indices.add(best1);
        indices.add(best2);
        indices.add(best3);
        return indices;
    }

    private String getLabel(String[] labels, int index) {
        if (index < 0 || index >= labels.length)
            return "class_" + index;
        return labels[index];
    }
}
