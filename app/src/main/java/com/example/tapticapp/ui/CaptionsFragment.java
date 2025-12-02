package com.example.tapticapp.ui;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tapticapp.R;
import com.example.tapticapp.data.CaptionMessage;
import com.example.tapticapp.services.SttService;
import com.example.tapticapp.services.TtsHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for the "Text (live captions)" tab.
 * Provides a chat-like interface with STT and TTS capabilities.
 */
public class CaptionsFragment extends Fragment {

    private ListView captionListView;
    private EditText captionInputField;
    private MaterialButton captionMicToggle;
    private MaterialButton captionSendButton;

    private List<CaptionMessage> messages;
    private CaptionsAdapter adapter;

    private TtsHelper ttsHelper;
    private SttService sttService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_captions, container, false);

        captionListView = view.findViewById(R.id.captionListView);
        captionInputField = view.findViewById(R.id.captionInputField);
        captionMicToggle = view.findViewById(R.id.captionMicToggle);
        captionSendButton = view.findViewById(R.id.captionSendButton);

        messages = new ArrayList<>();
        adapter = new CaptionsAdapter(requireContext(), messages, this::onReplayMessage);
        captionListView.setAdapter(adapter);

        // Initialize TTS
        ttsHelper = new TtsHelper(requireContext(), success -> {
            if (!success) {
                postSystemMessage("TTS initialization failed");
            }
        });

        // Initialize STT
        sttService = new SttService(requireContext());

        // Send button
        captionSendButton.setOnClickListener(v -> sendMessage());

        // Enter key sends message
        captionInputField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Mic toggle
        captionMicToggle.setOnClickListener(v -> toggleMicrophone());

        return view;
    }

    /**
     * Send a text message from the user.
     */
    private void sendMessage() {
        String text = captionInputField.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        captionInputField.setText("");

        CaptionMessage message = new CaptionMessage("You", text);
        messages.add(message);
        adapter.notifyDataSetChanged();
        captionListView.smoothScrollToPosition(messages.size() - 1);

        // Speak the message immediately using TTS
        speakText(text);
    }

    /**
     * Toggle microphone on/off for STT.
     */
    private void toggleMicrophone() {
        if (sttService.isListening()) {
            // Stop listening
            sttService.stop();
            captionMicToggle.setText("Start mic");
        } else {
            // Start listening
            boolean started = sttService.start(new SttService.SttCallback() {
                @Override
                public void onTextRecognized(String text) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> pushCaptionText(text));
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Only show error if it's not "No speech match" (too noisy)
                            if (!error.contains("No speech match") &&
                                    !error.contains("No speech input")) {
                                postSystemMessage("STT: " + error);
                            }
                        });
                    }
                }
            });

            if (started) {
                captionMicToggle.setText("Listening…");
            } else {
                Toast.makeText(requireContext(), "Failed to start microphone",
                        Toast.LENGTH_SHORT).show();
                postSystemMessage("Microphone not available. Check permissions.");
            }
        }
    }

    /**
     * Add a caption from "Them" (recognized speech from remote).
     */
    public void pushCaptionText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        CaptionMessage message = new CaptionMessage("Them", text);
        messages.add(message);
        adapter.notifyDataSetChanged();
        captionListView.smoothScrollToPosition(messages.size() - 1);
    }

    /**
     * Add a system message.
     */
    public void postSystemMessage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        CaptionMessage message = new CaptionMessage("System", text);
        messages.add(message);
        adapter.notifyDataSetChanged();
        captionListView.smoothScrollToPosition(messages.size() - 1);
    }

    /**
     * Replay a message using TTS.
     */
    private void onReplayMessage(CaptionMessage message) {
        speakText(message.getText());
    }

    /**
     * Speak text using TTS.
     */
    private void speakText(String text) {
        if (ttsHelper != null && ttsHelper.isInitialized()) {
            ttsHelper.speak(text);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Stop STT
        if (sttService != null) {
            sttService.stop();
            sttService.destroy();
        }

        // Shutdown TTS
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
    }
}
