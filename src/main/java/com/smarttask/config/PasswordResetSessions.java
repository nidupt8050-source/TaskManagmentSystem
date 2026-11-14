package com.smarttask.config;

import jakarta.servlet.http.*;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class PasswordResetSessions implements HttpSessionAttributeListener, HttpSessionListener {
    private final ConcurrentHashMap<String, HttpSession> sessions = new ConcurrentHashMap<>();
    @Bean
    public ServletListenerRegistrationBean<PasswordResetSessions> resetSessionListener() {
        return new ServletListenerRegistrationBean<>(this);
    }
    @Override public void attributeAdded(HttpSessionBindingEvent e) { track(e); }
    @Override public void attributeReplaced(HttpSessionBindingEvent e) { track(e); }
    private void track(HttpSessionBindingEvent e) {
        if ("userId".equals(e.getName())) sessions.put(e.getSession().getId(), e.getSession());
    }
    @Override public void sessionDestroyed(HttpSessionEvent e) { sessions.remove(e.getSession().getId()); }
    public void invalidate(Long userId) {
        sessions.values().forEach(session -> {
            try {
                if (userId.equals(session.getAttribute("userId"))) session.invalidate();
            } catch (IllegalStateException ignored) { /* Already expired. */ }
        });
    }
}
