package com.example.tapticapp.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tapticapp.R;
import com.example.tapticapp.core.Interpreter;

import java.util.List;

/**
 * Home fragment showing real-time audio detections.
 * Matches desktop's "Sound dashboard" tab with smooth animations.
 */
public class HomeFragment extends Fragment {

    private TextView statusText;
    private TextView topSoundText;
    private TextView soundLevelLabel;
    private ProgressBar soundLevelBar;
    private TextView sound1Text, sound2Text, sound3Text;
    private ProgressBar sound1Bar, sound2Bar, sound3Bar;

    // Remember last values to avoid unnecessary updates
    private String lastTopSound = "";
    private int lastLevel = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI elements
        statusText = view.findViewById(R.id.statusText);
        topSoundText = view.findViewById(R.id.topSoundText);
        soundLevelLabel = view.findViewById(R.id.soundLevelLabel);
        soundLevelBar = view.findViewById(R.id.soundLevelBar);
        sound1Text = view.findViewById(R.id.sound1Text);
        sound2Text = view.findViewById(R.id.sound2Text);
        sound3Text = view.findViewById(R.id.sound3Text);
        sound1Bar = view.findViewById(R.id.sound1Bar);
        sound2Bar = view.findViewById(R.id.sound2Bar);
        sound3Bar = view.findViewById(R.id.sound3Bar);

        // Set initial status
        if (statusText != null) {
            statusText.setText("Listening…");
        }
    }

    /**
     * Update UI with new detection results.
     * Called from MainActivity when audio service provides updates.
     */
    public void updateDetections(List<Interpreter.DetectionResult> top3, double level) {
        if (!isAdded()) {
            return;
        }

        // Update sound level meter (with smooth animation)
        int levelPercent = (int) (level * 100);
        if (levelPercent != lastLevel) {
            animateProgressBar(soundLevelBar, lastLevel, levelPercent);
            lastLevel = levelPercent;
            if (soundLevelLabel != null) {
                soundLevelLabel.setText("Sound level: " + levelPercent + "%");
            }
        }

        // Update top sound with fade animation
        if (!top3.isEmpty()) {
            Interpreter.DetectionResult top = top3.get(0);
            if (!top.label.equals(lastTopSound)) {
                fadeUpdateText(topSoundText, top.label);
                lastTopSound = top.label;
            }

            // Update top 3 rows
            updateRow(sound1Text, sound1Bar, top3.size() > 0 ? top3.get(0) : null, 1);
            updateRow(sound2Text, sound2Bar, top3.size() > 1 ? top3.get(1) : null, 2);
            updateRow(sound3Text, sound3Bar, top3.size() > 2 ? top3.get(2) : null, 3);
        } else {
            // No detections
            if (!lastTopSound.isEmpty()) {
                fadeUpdateText(topSoundText, "—");
                lastTopSound = "";
            }
            updateRow(sound1Text, sound1Bar, null, 1);
            updateRow(sound2Text, sound2Bar, null, 2);
            updateRow(sound3Text, sound3Bar, null, 3);
        }
    }

    /**
     * Update a row with smooth progress bar animation.
     */
    private void updateRow(TextView text, ProgressBar bar, Interpreter.DetectionResult result, int rank) {
        if (text == null || bar == null) {
            return;
        }

        if (result != null) {
            String label = "#" + rank + " " + result.label + " (" + (int) (result.score * 100) + "%)";
            text.setText(label);
            text.setVisibility(View.VISIBLE);
            bar.setVisibility(View.VISIBLE);

            int targetProgress = (int) (result.score * 100);
            animateProgressBar(bar, bar.getProgress(), targetProgress);
        } else {
            text.setVisibility(View.GONE);
            bar.setVisibility(View.GONE);
        }
    }

    /**
     * Fade animation for text changes matching desktop.
     */
    private void fadeUpdateText(final TextView textView, final String newText) {
        if (textView == null) {
            return;
        }

        // Fade out
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f);
        fadeOut.setDuration(150);
        fadeOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                textView.setText(newText);
                // Fade in
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f);
                fadeIn.setDuration(150);
                fadeIn.start();
            }
        });
        fadeOut.start();
    }

    /**
     * Smooth progress bar animation matching desktop.
     */
    private void animateProgressBar(final ProgressBar progressBar, int from, int to) {
        if (progressBar == null) {
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            progressBar.setProgress(value);
        });
        animator.start();
    }
}
