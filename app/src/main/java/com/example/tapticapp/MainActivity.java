package com.example.tapticapp;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tapticapp.core.Interpreter;
import com.example.tapticapp.services.AudioClassificationService;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AudioClassificationService audioService;
    private boolean isBound = false;

    // UI Elements
    private TextView topSoundText;
    private ProgressBar soundLevelBar;
    private TextView sound1Text, sound2Text, sound3Text;
    private ProgressBar sound1Bar, sound2Bar, sound3Bar;

    // Permissions
    private final ActivityResultLauncher<String> micPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    checkAndRequestNotificationPermission();
                } else {
                    Toast.makeText(this, R.string.error_mic_permission, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> startAudioService());

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            AudioClassificationService.LocalBinder binder = (AudioClassificationService.LocalBinder) service;
            audioService = binder.getService();
            isBound = true;

            // Set callback for UI updates
            audioService.setCallback((top3, level) -> runOnUiThread(() -> updateUI(top3, level)));
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        topSoundText = findViewById(R.id.topSoundText);
        soundLevelBar = findViewById(R.id.soundLevelBar);
        sound1Text = findViewById(R.id.sound1Text);
        sound2Text = findViewById(R.id.sound2Text);
        sound3Text = findViewById(R.id.sound3Text);
        sound1Bar = findViewById(R.id.sound1Bar);
        sound2Bar = findViewById(R.id.sound2Bar);
        sound3Bar = findViewById(R.id.sound3Bar);

        checkPermissions();
    }

    private void checkPermissions() {
        // if (ContextCompat.checkSelfPermission(this,
        // Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        // checkAndRequestNotificationPermission();
        // } else {
        // micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        // }
        Toast.makeText(this, "Permissions Check Disabled", Toast.LENGTH_SHORT).show();
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                startAudioService();
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            startAudioService();
        }
    }

    private void startAudioService() {
        // Intent intent = new Intent(this, AudioClassificationService.class);
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // startForegroundService(intent);
        // } else {
        // startService(intent);
        // }
        // bindService(intent, connection, Context.BIND_AUTO_CREATE);
        Toast.makeText(this, "Audio Service Disabled for Debugging", Toast.LENGTH_SHORT).show();
    }

    private void updateUI(List<Interpreter.DetectionResult> top3, double level) {
        // Update sound level meter
        soundLevelBar.setProgress((int) (level * 100));

        if (!top3.isEmpty()) {
            Interpreter.DetectionResult top = top3.get(0);
            topSoundText.setText(top.label);

            updateRow(sound1Text, sound1Bar, top3.get(0));
            if (top3.size() > 1)
                updateRow(sound2Text, sound2Bar, top3.get(1));
            if (top3.size() > 2)
                updateRow(sound3Text, sound3Bar, top3.get(2));
        }
    }

    private void updateRow(TextView text, ProgressBar bar, Interpreter.DetectionResult result) {
        text.setText(result.label + " (" + (int) (result.score * 100) + "%)");
        bar.setProgress((int) (result.score * 100));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
}
