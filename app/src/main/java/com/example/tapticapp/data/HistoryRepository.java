package com.example.tapticapp.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository for managing detection history.
 * Handles database operations on background thread.
 */
public class HistoryRepository {

    private static volatile HistoryRepository INSTANCE;
    private final HistoryDao historyDao;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final List<HistoryCallback> callbacks = new ArrayList<>();

    public interface HistoryCallback {
        void onHistoryChanged(List<DetectionEvent> events);
    }

    private HistoryRepository(Context context) {
        HistoryDatabase db = HistoryDatabase.getInstance(context);
        historyDao = db.historyDao();
    }

    public static HistoryRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (HistoryRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HistoryRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public void addCallback(HistoryCallback callback) {
        synchronized (callbacks) {
            callbacks.add(callback);
        }
    }

    public void removeCallback(HistoryCallback callback) {
        synchronized (callbacks) {
            callbacks.remove(callback);
        }
    }

    public void insert(DetectionEvent event) {
        executor.execute(() -> {
            historyDao.insert(event);
            historyDao.deleteOldEvents(); // Keep only last 1000
            notifyCallbacks();
        });
    }

    public void loadHistory(HistoryCallback callback) {
        executor.execute(() -> {
            List<DetectionEvent> events = historyDao.getAllEvents();
            callback.onHistoryChanged(events);
        });
    }

    public void clearAll() {
        executor.execute(() -> {
            historyDao.deleteAll();
            notifyCallbacks();
        });
    }

    private void notifyCallbacks() {
        executor.execute(() -> {
            List<DetectionEvent> events = historyDao.getAllEvents();
            synchronized (callbacks) {
                for (HistoryCallback callback : callbacks) {
                    callback.onHistoryChanged(events);
                }
            }
        });
    }
}
