package com.example.tapticapp.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * Service for Speech-to-Text using Android's built-in SpeechRecognizer.
 * Provides continuous listening similar to desktop's Google Cloud Speech.
 */
public class SttService {
    private static final String TAG = "SttService";

    private final Context context;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private boolean isListening = false;

    public interface SttCallback {
        void onTextRecognized(String text);

        void onError(String error);
    }

    private SttCallback callback;

    public SttService(Context context) {
        this.context = context;
        initializeRecognizer();
    }

    private void initializeRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available on this device");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Audio level changed
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // More audio data
            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "End of speech");
            }

            @Override
            public void onError(int error) {
                String errorMessage = getErrorMessage(error);
                Log.e(TAG, "Recognition error: " + errorMessage);

                if (callback != null) {
                    callback.onError(errorMessage);
                }

                // Restart listening if still active
                if (isListening && error != SpeechRecognizer.ERROR_CLIENT) {
                    restartListening();
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String text = matches.get(0);
                    Log.d(TAG, "Recognized: " + text);

                    if (callback != null) {
                        callback.onTextRecognized(text);
                    }
                }

                // Restart listening for continuous mode
                if (isListening) {
                    restartListening();
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // Partial results during speech
                ArrayList<String> matches = partialResults.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    Log.d(TAG, "Partial: " + matches.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Reserved for future use
            }
        });
    }

    /**
     * Start listening for speech.
     * Returns true if started successfully, false otherwise.
     */
    public boolean start(SttCallback callback) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            if (callback != null) {
                callback.onError("Speech recognition not available");
            }
            return false;
        }

        this.callback = callback;

        if (speechRecognizer == null) {
            initializeRecognizer();
        }

        try {
            isListening = true;
            speechRecognizer.startListening(recognizerIntent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start listening", e);
            isListening = false;
            return false;
        }
    }

    /**
     * Stop listening for speech.
     */
    public void stop() {
        isListening = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping speech recognizer", e);
            }
        }
    }

    /**
     * Clean up resources. Call this when done (e.g., in onDestroy).
     */
    public void destroy() {
        isListening = false;
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Error destroying speech recognizer", e);
            }
            speechRecognizer = null;
        }
    }

    private void restartListening() {
        if (speechRecognizer != null && isListening) {
            try {
                // Small delay before restarting
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (isListening) {
                        speechRecognizer.startListening(recognizerIntent);
                    }
                }, 100);
            } catch (Exception e) {
                Log.e(TAG, "Error restarting listening", e);
            }
        }
    }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error";
        }
    }

    public boolean isListening() {
        return isListening;
    }
}
