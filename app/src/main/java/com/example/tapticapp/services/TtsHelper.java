package com.example.tapticapp.services;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Helper class for Text-to-Speech functionality.
 * Equivalent to the desktop's 'say' command.
 */
public class TtsHelper {
    private static final String TAG = "TtsHelper";
    private TextToSpeech tts; // Not final due to lambda initialization timing
    private boolean isInitialized = false;

    public interface OnInitListener {
        void onInitialized(boolean success);
    }

    public TtsHelper(Context context, final OnInitListener listener) {
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    if (tts != null) {
                        int result = tts.setLanguage(Locale.US);
                        if (result == TextToSpeech.LANG_MISSING_DATA ||
                                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e(TAG, "Language not supported");
                            isInitialized = false;
                            if (listener != null) {
                                listener.onInitialized(false);
                            }
                        } else {
                            // Set speech rate to match desktop (240 wpm is ~1.5x normal speed)
                            tts.setSpeechRate(1.5f);
                            isInitialized = true;
                            if (listener != null) {
                                listener.onInitialized(true);
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "TTS initialization failed");
                    isInitialized = false;
                    if (listener != null) {
                        listener.onInitialized(false);
                    }
                }
            }
        });
    }

    /**
     * Speak the given text using TTS.
     * Stops any currently speaking text first.
     */
    public void speak(String text) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized");
            return;
        }
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        // Stop any current speech first
        tts.stop();

        // Speak the new text
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    /**
     * Stop any currently speaking text.
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    /**
     * Shutdown the TTS engine. Call this when done (e.g., in onDestroy).
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        isInitialized = false;
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}
