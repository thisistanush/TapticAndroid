package com.example.tapticapp.data;

/**
 * Represents a caption message in the live captions chat.
 * Can be from "You", "Them", or "System".
 */
public class CaptionMessage {
    private final String speaker;
    private final String text;

    public CaptionMessage(String speaker, String text) {
        this.speaker = speaker;
        this.text = text;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getText() {
        return text;
    }

    public boolean isFromUser() {
        return "You".equals(speaker);
    }

    public boolean isFromThem() {
        return "Them".equals(speaker);
    }

    public boolean isFromSystem() {
        return "System".equals(speaker);
    }
}
