package com.smarttask.service;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;

@Component
public class PasswordResetRateLimiter {
    private record Window(Instant until, int count) {}
    private final Map<String, Window> windows = new HashMap<>();
    public synchronized boolean allow(String key, int limit) {
        Instant now = Instant.now();
        windows.entrySet().removeIf(e -> !e.getValue().until().isAfter(now));
        Window window = windows.get(key);
        if (window == null) {
            if (windows.size() >= 10000) return false;
            windows.put(key, new Window(now.plusSeconds(900), 1));
            return true;
        }
        if (window.count() >= limit) return false;
        windows.put(key, new Window(window.until(), window.count() + 1));
        return true;
    }
}
