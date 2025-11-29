package com.example.tapticapp.data;

/**
 * Data class representing a caption message.
 */
public class CaptionMessage {
    private final String speaker;
    private final String text;
    private final long timestamp;

    public CaptionMessage(String speaker, String text) {
        this.speaker = speaker;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
