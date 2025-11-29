package com.example.tapticapp.network;

import android.os.Build;
import android.util.Log;
import org.json.JSONObject;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Sends sound detection events to other Taptic devices on the network via UDP
 * broadcast.
 */
public class BroadcastSender {

    private static final String TAG = "BroadcastSender";
    private static final int PORT = 50000;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);

    public void sendEvent(String eventLabel) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("type", eventLabel);
                json.put("time", TIME_FORMAT.format(new Date()));
                json.put("host", getDeviceName());

                byte[] payload = json.toString().getBytes(StandardCharsets.UTF_8);

                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setBroadcast(true);
                    InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                    DatagramPacket packet = new DatagramPacket(payload, payload.length, broadcastAddress, PORT);
                    socket.send(packet);
                    Log.d(TAG, "Broadcast sent: " + eventLabel);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to send broadcast", e);
            }
        }).start();
    }

    private String getDeviceName() {
        String model = Build.MODEL;
        return model != null ? model : "Android Device";
    }
}
