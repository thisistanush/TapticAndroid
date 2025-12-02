package com.example.tapticapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tapticapp.R;
import com.example.tapticapp.data.CaptionMessage;

import java.util.List;

/**
 * Adapter for displaying caption messages in the live captions list.
 * Shows speaker and text, with a "Replay" button for user's messages.
 */
public class CaptionsAdapter extends ArrayAdapter<CaptionMessage> {

    public interface OnReplayClickListener {
        void onReplayClick(CaptionMessage message);
    }

    private final OnReplayClickListener replayListener;

    public CaptionsAdapter(@NonNull Context context, @NonNull List<CaptionMessage> messages,
            @Nullable OnReplayClickListener replayListener) {
        super(context, 0, messages);
        this.replayListener = replayListener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_caption_message, parent, false);
        }

        CaptionMessage message = getItem(position);
        if (message == null) {
            return convertView;
        }

        TextView messageText = convertView.findViewById(R.id.captionMessageText);
        Button replayButton = convertView.findViewById(R.id.captionReplayButton);

        String displayText = message.getSpeaker() + ": " + message.getText();
        messageText.setText(displayText);

        if (message.isFromUser() && replayListener != null) {
            replayButton.setVisibility(View.VISIBLE);
            replayButton.setOnClickListener(v -> replayListener.onReplayClick(message));
        } else {
            replayButton.setVisibility(View.GONE);
        }

        return convertView;
    }
}
