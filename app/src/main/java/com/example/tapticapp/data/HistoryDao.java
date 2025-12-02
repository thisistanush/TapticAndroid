package com.example.tapticapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for detection events.
 */
@Dao
public interface HistoryDao {

    @Insert
    void insert(DetectionEvent event);

    @Query("SELECT * FROM detections ORDER BY timestamp DESC LIMIT 1000")
    List<DetectionEvent> getAllEvents();

    @Query("DELETE FROM detections")
    void deleteAll();

    @Query("DELETE FROM detections WHERE id NOT IN (SELECT id FROM detections ORDER BY timestamp DESC LIMIT 1000)")
    void deleteOldEvents();
}
