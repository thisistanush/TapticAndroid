package com.example.tapticapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tapticapp.config.AppConfig;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Settings activity matching desktop's SettingsController exactly.
 * Provides comprehensive configuration for:
 * - Notification behavior (sounds, emoji, flash, sensitivity)
 * - Emergency sound classifications
 */
public class SettingsActivity extends AppCompatActivity {

    private AppConfig config;

    // UI Components
    private CheckBox playSoundCheckbox;
    private CheckBox flashCheckbox;
    private Spinner notificationSoundSpinner;
    private Spinner emergencySoundSpinner;
    private Spinner notificationEmojiSpinner;
    private SeekBar sensitivitySlider;
    private TextView sensitivityValue;

    // Emergency sounds
    private Spinner emergencyLabelSpinner;
    private Button addEmergencyButton;
    private LinearLayout emergencyChipsContainer;

    private Button saveButton;

    // All possible sound labels (will be populated from Interpreter)
    private List<String> allSoundLabels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        config = AppConfig.getInstance(this);

        initializeViews();
        setupSpinners();
        loadSettings();
        setupListeners();
    }

    private void initializeViews() {
        playSoundCheckbox = findViewById(R.id.playSoundCheckbox);
        flashCheckbox = findViewById(R.id.flashCheckbox);
        notificationSoundSpinner = findViewById(R.id.notificationSoundSpinner);
        emergencySoundSpinner = findViewById(R.id.emergencySoundSpinner);
        notificationEmojiSpinner = findViewById(R.id.notificationEmojiSpinner);
        sensitivitySlider = findViewById(R.id.sensitivitySlider);
        sensitivityValue = findViewById(R.id.sensitivityValue);
        emergencyLabelSpinner = findViewById(R.id.emergencyLabelSpinner);
        addEmergencyButton = findViewById(R.id.addEmergencyButton);
        emergencyChipsContainer = findViewById(R.id.emergencyChipsContainer);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupSpinners() {
        // Notification Sound Spinner
        ArrayAdapter<CharSequence> notificationSoundAdapter = ArrayAdapter.createFromResource(
                this, R.array.notification_sounds, R.layout.spinner_item);
        notificationSoundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        notificationSoundSpinner.setAdapter(notificationSoundAdapter);

        // Emergency Sound Spinner
        ArrayAdapter<CharSequence> emergencySoundAdapter = ArrayAdapter.createFromResource(
                this, R.array.emergency_sounds, R.layout.spinner_item);
        emergencySoundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emergencySoundSpinner.setAdapter(emergencySoundAdapter);

        // Notification Emoji Spinner
        ArrayAdapter<CharSequence> emojiAdapter = ArrayAdapter.createFromResource(
                this, R.array.notification_emojis, R.layout.spinner_item);
        emojiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        notificationEmojiSpinner.setAdapter(emojiAdapter);

        // Emergency Label Spinner (will be populated with common sounds)
        populateEmergencyLabelSpinner();
    }

    private void populateEmergencyLabelSpinner() {
        // Common sounds that might be marked as emergency
        allSoundLabels.clear();
        allSoundLabels.add("Fire alarm");
        allSoundLabels.add("Smoke alarm");
        allSoundLabels.add("Smoke detector");
        allSoundLabels.add("Siren");
        allSoundLabels.add("Emergency vehicle");
        allSoundLabels.add("Glass breaking");
        allSoundLabels.add("Gunshot");
        allSoundLabels.add("Explosion");
        allSoundLabels.add("Scream");
        allSoundLabels.add("Baby crying");
        allSoundLabels.add("Dog barking");
        allSoundLabels.add("Door knock");
        allSoundLabels.add("Doorbell");
        allSoundLabels.add("Car horn");
        allSoundLabels.add("Thunder");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.spinner_item, allSoundLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        emergencyLabelSpinner.setAdapter(adapter);
    }

    private void loadSettings() {
        // Load notification behavior
        playSoundCheckbox.setChecked(config.isPlaySound());
        flashCheckbox.setChecked(config.isFlashEmergency());

        // Load notification sound
        String notificationSound = config.getNotificationSound();
        setSpinnerValue(notificationSoundSpinner, notificationSound);

        // Load emergency sound
        String emergencySound = config.getEmergencySound();
        setSpinnerValue(emergencySoundSpinner, emergencySound);

        // Load notification emoji
        String emoji = config.getNotificationEmoji();
        setSpinnerValue(notificationEmojiSpinner, emoji);

        // Load sensitivity
        double threshold = config.getNotifyThreshold();
        int progress = (int) Math.round(threshold * 100);
        sensitivitySlider.setProgress(progress);
        sensitivityValue.setText(String.format("%.2f", threshold));

        // Load emergency sounds chips
        refreshEmergencyChips();
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void setupListeners() {
        // Sensitivity slider
        sensitivitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double value = progress / 100.0;
                sensitivityValue.setText(String.format("%.2f", value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Add emergency sound button
        addEmergencyButton.setOnClickListener(v -> {
            String label = emergencyLabelSpinner.getSelectedItem().toString();
            if (label != null && !label.isEmpty()) {
                config.setEmergencyLabel(label, true);
                refreshEmergencyChips();
                Toast.makeText(this, "Added: " + label, Toast.LENGTH_SHORT).show();
            }
        });

        // Save button
        saveButton.setOnClickListener(v -> saveSettings());
    }

    private void refreshEmergencyChips() {
        emergencyChipsContainer.removeAllViews();

        Set<String> emergencyLabels = config.getEmergencyLabels();
        for (String label : emergencyLabels) {
            Chip chip = new Chip(this);
            chip.setText(label);
            chip.setCloseIconVisible(true);
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5252")));
            chip.setTextColor(Color.WHITE);
            chip.setOnCloseIconClickListener(v -> {
                config.setEmergencyLabel(label, false);
                refreshEmergencyChips();
                Toast.makeText(this, "Removed: " + label, Toast.LENGTH_SHORT).show();
            });
            emergencyChipsContainer.addView(chip);
        }
    }

    private void saveSettings() {
        // Save notification behavior
        config.setPlaySound(playSoundCheckbox.isChecked());
        config.setFlashEmergency(flashCheckbox.isChecked());

        // Save sounds and emoji
        config.setNotificationSound(notificationSoundSpinner.getSelectedItem().toString());
        config.setEmergencySound(emergencySoundSpinner.getSelectedItem().toString());
        config.setNotificationEmoji(notificationEmojiSpinner.getSelectedItem().toString());

        // Save sensitivity
        double threshold = sensitivitySlider.getProgress() / 100.0;
        config.setNotifyThreshold(threshold);

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
