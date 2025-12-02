package com.example.tapticapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.tapticapp.config.AppConfig;
import com.example.tapticapp.services.AudioClassificationService;
import com.example.tapticapp.ui.CheckboxListFragment;
import com.example.tapticapp.ui.HomeFragment;
import com.example.tapticapp.ui.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main activity matching desktop's MainViewController exactly.
 * Features:
 * - Header bar with title, history toggle, settings button
 * - Two main tabs: Sound Dashboard and Text (live captions)
 * - Slide-out history drawer
 * - Expandable Monitored/Notify section at bottom
 * - Flash overlay for emergencies
 */
public class MainActivity extends AppCompatActivity {

    private AudioClassificationService audioService;
    private boolean isBound = false;
    private AppConfig config;

    // Main UI
    private DrawerLayout drawerLayout;
    private ViewPager2 mainViewPager;
    private TabLayout mainTabLayout;
    private ViewPagerAdapter pagerAdapter;

    // Header
    private TextView micWarningLabel;
    private Button historyToggleButton;
    private Button settingsButton;

    // History drawer
    private ListView historyListView;
    private Button clearHistoryButton;
    private final List<String> historyItems = new ArrayList<>();
    private ArrayAdapter<String> historyAdapter;

    // Monitored/Notify section
    private View monitoredNotifyHeader;
    private View monitoredNotifyContent;
    private TextView monitoredNotifyExpandIcon;
    private ViewPager2 monitoredNotifyViewPager;
    private TabLayout monitoredNotifyTabLayout;
    private CheckboxListFragment monitoredFragment;
    private CheckboxListFragment notifyFragment;
    private boolean isMonitoredNotifyExpanded = false;

    // Flash overlay
    private View flashOverlay;
    private ObjectAnimator flashAnimator;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

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
            audioService.setCallback(new AudioClassificationService.ServiceCallback() {
                @Override
                public void onAudioUpdate(List<com.example.tapticapp.core.Interpreter.DetectionResult> top3,
                        double level) {
                    runOnUiThread(() -> updateHomeFragment(top3, level));
                }

                @Override
                public void onEmergencyFlash() {
                    runOnUiThread(() -> flashEmergency());
                }
            });

            // Initialize monitored lists
            initMonitoredLists(audioService.getLabels());
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

        config = AppConfig.getInstance(this);

        initializeViews();
        setupMainTabs();
        setupHistoryDrawer();
        setupMonitoredNotifySection();
        setupFlashOverlay();

        checkPermissions();
    }

    private void initializeViews() {
        // Main UI
        drawerLayout = findViewById(R.id.drawerLayout);
        mainViewPager = findViewById(R.id.mainViewPager);
        mainTabLayout = findViewById(R.id.mainTabLayout);

        // Header
        micWarningLabel = findViewById(R.id.micWarningLabel);
        historyToggleButton = findViewById(R.id.historyToggleButton);
        settingsButton = findViewById(R.id.settingsButton);

        // History drawer
        historyListView = findViewById(R.id.historyListView);
        clearHistoryButton = findViewById(R.id.clearHistoryButton);

        // Monitored/Notify
        monitoredNotifyHeader = findViewById(R.id.monitoredNotifyHeader);
        monitoredNotifyContent = findViewById(R.id.monitoredNotifyContent);
        monitoredNotifyExpandIcon = findViewById(R.id.monitoredNotifyExpandIcon);
        monitoredNotifyViewPager = findViewById(R.id.monitoredNotifyViewPager);
        monitoredNotifyTabLayout = findViewById(R.id.monitoredNotifyTabLayout);

        // Flash overlay
        flashOverlay = findViewById(R.id.flashOverlay);

        // Disable drawer swipe (only toggle button opens it)
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        // Button listeners
        if (historyToggleButton != null) {
            historyToggleButton.setOnClickListener(v -> toggleHistoryDrawer());
        }

        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> openSettings());
        }

        if (clearHistoryButton != null) {
            clearHistoryButton.setOnClickListener(v -> clearHistory());
        }

        if (monitoredNotifyHeader != null) {
            monitoredNotifyHeader.setOnClickListener(v -> toggleMonitoredNotifySection());
        }

        // Hide mic warning initially
        if (micWarningLabel != null) {
            micWarningLabel.setVisibility(View.GONE);
        }
    }

    private void setupMainTabs() {
        if (mainViewPager == null || mainTabLayout == null) {
            return;
        }

        pagerAdapter = new ViewPagerAdapter(this);
        mainViewPager.setAdapter(pagerAdapter);

        // Link TabLayout and ViewPager2
        new TabLayoutMediator(mainTabLayout, mainViewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Sound dashboard");
            } else {
                tab.setText("Text (live captions)");
            }
        }).attach();

        // Manage audio service based on tab (Pause Yamnet when using STT)
        mainViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (audioService != null) {
                    if (position == 1) { // Captions tab
                        audioService.pauseAudio();
                    } else { // Home tab
                        audioService.resumeAudio();
                    }
                }
            }
        });
    }

    private void setupHistoryDrawer() {
        if (historyListView == null) {
            return;
        }

        // Custom adapter for colored history items
        historyAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, historyItems) {
            @Override
            public android.view.View getView(int position, android.view.View convertView,
                    android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);

                String item = getItem(position);
                if (item != null) {
                    textView.setText(item);

                    // Color based on type
                    if (item.startsWith("★")) {
                        // Important (notification triggered)
                        textView.setTextColor(0xFFFFC46B); // Gold
                    } else if (item.contains("[REMOTE")) {
                        // Remote device
                        textView.setTextColor(0xFF8AB4FF); // Blue
                    } else {
                        // Local
                        textView.setTextColor(0xFFE5E9F0); // Light gray
                    }
                }

                return view;
            }
        };

        historyListView.setAdapter(historyAdapter);
    }

    private void setupMonitoredNotifySection() {
        // Create monitored and notify fragments
        monitoredFragment = CheckboxListFragment.newInstance(CheckboxListFragment.TYPE_MONITORED);
        notifyFragment = CheckboxListFragment.newInstance(CheckboxListFragment.TYPE_NOTIFY);

        // Adapter for monitored/notify tabs
        androidx.viewpager2.adapter.FragmentStateAdapter adapter = new androidx.viewpager2.adapter.FragmentStateAdapter(
                this) {
            @Override
            public int getItemCount() {
                return 2;
            }

            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return monitoredFragment;
                } else {
                    return notifyFragment;
                }
            }
        };

        if (monitoredNotifyViewPager != null) {
            monitoredNotifyViewPager.setAdapter(adapter);
        }

        if (monitoredNotifyTabLayout != null && monitoredNotifyViewPager != null) {
            new TabLayoutMediator(monitoredNotifyTabLayout, monitoredNotifyViewPager,
                    (tab, position) -> {
                        if (position == 0) {
                            tab.setText("Monitored");
                        } else {
                            tab.setText("Notify");
                        }
                    }).attach();
        }

        // Populate with interesting sounds
        // This will be called after audio service is loaded with labels
    }

    /**
     * Populate monitored/notify checkboxes with interesting sound labels.
     * This matches the desktop's isInterestingLabel logic.
     */
    public void initMonitoredLists(String[] allLabels) {
        if (allLabels == null) {
            return;
        }

        List<String> interesting = new ArrayList<>();
        for (String label : allLabels) {
            if (label != null && isInterestingLabel(label)) {
                interesting.add(label);
            }
        }
        interesting.sort(String.CASE_INSENSITIVE_ORDER);

        // Populate both fragments
        if (monitoredFragment != null) {
            monitoredFragment.populateCheckboxes(interesting);
            // Set initial state
            for (String label : interesting) {
                if (!config.isMonitoredEnabled(label)) {
                    monitoredFragment.setChecked(label, false);
                }
            }
            monitoredFragment.setOnCheckboxChangeListener((label, checked, type) -> {
                config.setMonitoredEnabled(label, checked);
            });
        }
        if (notifyFragment != null) {
            notifyFragment.populateCheckboxes(interesting);
            // Set initial state
            for (String label : interesting) {
                if (!config.isNotifyEnabled(label)) {
                    notifyFragment.setChecked(label, false);
                }
            }
            notifyFragment.setOnCheckboxChangeListener((label, checked, type) -> {
                config.setNotifyEnabled(label, checked);
            });
        }
    }

    /**
     * Check if a label is "interesting" - matches desktop logic exactly.
     */
    private boolean isInterestingLabel(String label) {
        String lower = label.toLowerCase(Locale.ROOT);

        String[] bad = {
                "silence", "quiet", "room tone", "noise", "static", "hum", "hiss",
                "wind noise", "white noise", "pink noise",
                "drip", "dripping", "raindrop"
        };
        for (String b : bad) {
            if (lower.contains(b)) {
                return false;
            }
        }

        String[] good = {
                "alarm", "fire", "smoke", "siren",
                "door", "doorbell", "door bell", "door knock", "knocking",
                "door open", "door close",
                "window", "glass", "glass breaking",
                "phone", "telephone", "ring", "ringtone",
                "baby", "infant", "cry", "crying",
                "child", "kid",
                "dog", "bark", "cat", "meow",
                "microwave", "oven", "timer", "beep",
                "washing machine", "laundry", "dryer",
                "dishwasher",
                "tap", "faucet", "running water",
                "car horn", "car alarm", "horn", "engine", "motorcycle",
                "gunshot", "explosion",
                "footstep", "walking", "knock",
                "shout", "scream", "yell",
                "applause",
                "cough", "sneeze",
                "thunder"
        };
        for (String g : good) {
            if (lower.contains(g)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a sound is monitored.
     */
    public boolean isMonitored(String label) {
        if (monitoredFragment != null) {
            return monitoredFragment.isChecked(label);
        }
        return true; // Default: monitor all
    }

    /**
     * Check if a sound should trigger notifications.
     */
    public boolean isNotifyEnabled(String label) {
        if (notifyFragment != null) {
            return notifyFragment.isChecked(label);
        }
        return true; // Default: notify for all
    }

    private void setupFlashOverlay() {
        if (flashOverlay != null) {
            flashOverlay.setVisibility(View.GONE);
        }
    }

    private void toggleHistoryDrawer() {
        if (drawerLayout == null) {
            return;
        }

        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
            if (historyToggleButton != null) {
                historyToggleButton.setText("▸ History");
            }
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
            if (historyToggleButton != null) {
                historyToggleButton.setText("History ▾");
            }
        }
    }

    private void toggleMonitoredNotifySection() {
        if (monitoredNotifyContent == null || monitoredNotifyExpandIcon == null) {
            return;
        }

        isMonitoredNotifyExpanded = !isMonitoredNotifyExpanded;

        if (isMonitoredNotifyExpanded) {
            monitoredNotifyContent.setVisibility(View.VISIBLE);
            monitoredNotifyExpandIcon.setText("▲");
        } else {
            monitoredNotifyContent.setVisibility(View.GONE);
            monitoredNotifyExpandIcon.setText("▼");
        }
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void clearHistory() {
        historyItems.clear();
        if (historyAdapter != null) {
            historyAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Add an item to the history drawer.
     * Format matches desktop exactly.
     */
    public void addHistory(String label, double score, boolean emergency,
            boolean local, String host, boolean important) {
        if (label == null) {
            return;
        }

        int pct = (int) Math.round(score * 100.0);
        String src;
        if (local) {
            src = "[THIS DEVICE]";
        } else {
            String h = (host == null || host.isBlank()) ? "remote" : host;
            src = "[REMOTE " + h + "]";
        }

        String tag = emergency ? "EMERGENCY" : "normal";
        String prefix = important ? "★ " : "";
        String time = timeFormat.format(new Date());
        String entry = String.format("%s%s %s – %s [%s] (%d%%)",
                prefix, time, src, label, tag, pct);

        runOnUiThread(() -> {
            historyItems.add(0, entry); // Newest on top
            if (historyItems.size() > 400) {
                historyItems.remove(historyItems.size() - 1);
            }
            if (historyAdapter != null) {
                historyAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Flash the screen red for emergencies.
     */
    public void flashEmergency() {
        if (flashOverlay == null) {
            return;
        }

        runOnUiThread(() -> {
            // Cancel any existing animation
            if (flashAnimator != null) {
                flashAnimator.cancel();
            }

            flashOverlay.setVisibility(View.VISIBLE);
            flashOverlay.setAlpha(0f);

            // Flash 25 times over ~10 seconds (400ms per toggle)
            flashAnimator = ObjectAnimator.ofFloat(flashOverlay, "alpha",
                    0f, 0.9f, 0f, 0.9f, 0f, 0.9f, 0f, 0.9f, 0f, 0.9f,
                    0f, 0.9f, 0f, 0.9f, 0f, 0.9f, 0f, 0.9f, 0f, 0.9f,
                    0f, 0.9f, 0f, 0.9f, 0f, 0.9f, 0f, 0.9f, 0f, 0.9f,
                    0f, 0.9f, 0f, 0.9f, 0f, 0.9f, 0f, 0.9f, 0f, 0.9f,
                    0f, 0.9f, 0f, 0.9f, 0f, 0.9f, 0f, 0.9f, 0f, 0.9f, 0f);
            flashAnimator.setDuration(10000); // 10 seconds
            flashAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            flashAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    flashOverlay.setVisibility(View.GONE);
                    flashOverlay.setAlpha(0f);
                }
            });
            flashAnimator.start();
        });
    }

    /**
     * Show microphone error matching desktop.
     */
    public void showMicError(String msg) {
        runOnUiThread(() -> {
            if (micWarningLabel != null) {
                micWarningLabel.setText("Mic missing");
                micWarningLabel.setVisibility(View.VISIBLE);
            }
            Toast.makeText(this, "Microphone problem: " + msg, Toast.LENGTH_LONG).show();
        });
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            checkAndRequestNotificationPermission();
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
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
        Intent intent = new Intent(this, AudioClassificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void updateHomeFragment(java.util.List<com.example.tapticapp.core.Interpreter.DetectionResult> top3,
            double level) {
        // Get the Home fragment and update it
        if (pagerAdapter != null) {
            Fragment fragment = getSupportFragmentManager()
                    .findFragmentByTag("f" + 0); // ViewPager2 tags fragments as "f" + position

            if (fragment instanceof HomeFragment) {
                ((HomeFragment) fragment).updateDetections(top3, level);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        if (flashAnimator != null) {
            flashAnimator.cancel();
        }
    }
}
