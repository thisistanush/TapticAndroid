package com.example.tapticapp.network;

import android.os.Build;
import android.util.Log;
import org.json.JSONObject;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

/**
 * Listens for sound detection broadcasts from other Taptic devices.
 */
public class BroadcastListener {

    private static final String TAG = "BroadcastListener";
    private static final int PORT = 50000;

    public interface BroadcastCallback {
        void onEventReceived(String eventLabel, String deviceName);
    }

    private final BroadcastCallback callback;
    private DatagramSocket socket;
    private boolean isRunning = false;
    private Thread listenerThread;

    public BroadcastListener(BroadcastCallback callback) {
        this.callback = callback;
    }

    public void start() {
        if (isRunning)
            return;

        isRunning = true;
        listenerThread = new Thread(this::listenForBroadcasts);
        listenerThread.start();
        Log.d(TAG, "Broadcast listener started");
    }

    public void stop() {
        isRunning = false;
        if (socket != null) {
            socket.close();
            socket = null;
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
    }

    private void listenForBroadcasts() {
        try {
            socket = new DatagramSocket(PORT);
            socket.setReuseAddress(true);

            byte[] buffer = new byte[2048];
            String thisDeviceName = getDeviceName();

            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String jsonText = new String(packet.getData(), packet.getOffset(), packet.getLength(),
                            StandardCharsets.UTF_8);
                    JSONObject json = new JSONObject(jsonText);

                    String eventLabel = json.optString("type");
                    String deviceName = json.optString("host", "Unknown");

                    // Ignore own messages
                    if (deviceName.equals(thisDeviceName))
                        continue;

                    if (eventLabel != null && !eventLabel.isEmpty()) {
                        Log.d(TAG, "Received broadcast: " + eventLabel + " from " + deviceName);
                        callback.onEventReceived(eventLabel, deviceName);
                    }

                } catch (Exception e) {
                    if (isRunning)
                        Log.e(TAG, "Error receiving packet", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start broadcast listener", e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private String getDeviceName() {
        String model = Build.MODEL;
        return model != null ? model : "Android Device";
    }
}
