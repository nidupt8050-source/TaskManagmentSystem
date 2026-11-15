package com.smarttask.service;

import com.smarttask.datastructures.SimpleMap;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PasswordResetRateLimiter {

    private record Window(Instant until, int count) {
    }

    private final SimpleMap<String, Window> windows = new SimpleMap<>();

    public synchronized boolean allow(String key, int limit) {
        Instant now = Instant.now();
        // Simple cleanup of expired keys
        var keys = windows.keys();
        for (int i = 0; i < keys.size(); i++) {
            String existing = keys.get(i);
            Window window = windows.get(existing);
            if (window != null && !window.until().isAfter(now)) {
                windows.remove(existing);
            }
        }

        Window window = windows.get(key);
        if (window == null) {
            if (windows.size() >= 10000) {
                return false;
            }
            windows.put(key, new Window(now.plusSeconds(900), 1));
            return true;
        }
        if (window.count() >= limit) {
            return false;
        }
        windows.put(key, new Window(window.until(), window.count() + 1));
        return true;
    }
}
