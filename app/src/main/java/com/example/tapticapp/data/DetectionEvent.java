package com.example.tapticapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Database entity representing a single sound detection event.
 */
@Entity(tableName = "detections")
public class DetectionEvent {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;
    public String label;
    public double confidence;
    public boolean isEmergency;
    public boolean isRemote;
    public String deviceName; // null for local detections

    public DetectionEvent(long timestamp, String label, double confidence,
            boolean isEmergency, boolean isRemote, String deviceName) {
        this.timestamp = timestamp;
        this.label = label;
        this.confidence = confidence;
        this.isEmergency = isEmergency;
        this.isRemote = isRemote;
        this.deviceName = deviceName;
    }
}
