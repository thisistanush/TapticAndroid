package com.example.tapticapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room database for storing detection history.
 */
@Database(entities = { DetectionEvent.class }, version = 1, exportSchema = false)
public abstract class HistoryDatabase extends RoomDatabase {

    private static volatile HistoryDatabase INSTANCE;

    public abstract HistoryDao historyDao();

    public static HistoryDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (HistoryDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            HistoryDatabase.class,
                            "detection_history_db").build();
                }
            }
        }
        return INSTANCE;
    }
}
