package com.smarttask.service;

import com.smarttask.config.PasswordResetSessions;
import com.smarttask.database.PasswordResetTokenDao;
import com.smarttask.database.UserDao;
import com.smarttask.model.PasswordResetToken;
import com.smarttask.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetService {

    private final UserDao users;
    private final PasswordResetTokenDao tokens;
    private final AuthService auth;
    private final JavaMailSender mail;
    private final PasswordResetSessions sessions;
    private final String baseUrl;
    private final String from;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserDao users, PasswordResetTokenDao tokens, AuthService auth,
                                JavaMailSender mail, PasswordResetSessions sessions,
                                @Value("${smarttask.reset.base-url}") String baseUrl,
                                @Value("${smarttask.reset.from}") String from) {
        this.users = users;
        this.tokens = tokens;
        this.auth = auth;
        this.mail = mail;
        this.sessions = sessions;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.from = from;
    }

    public void checkMailConfiguration() {
        if (mail instanceof org.springframework.mail.javamail.JavaMailSenderImpl sender) {
            boolean authenticated = Boolean.parseBoolean(
                    sender.getJavaMailProperties().getProperty("mail.smtp.auth", "false"));
            if (sender.getHost() == null || sender.getHost().isBlank()
                    || from.isBlank() || from.equals("no-reply@localhost")
                    || (authenticated && (sender.getUsername() == null || sender.getUsername().isBlank()
                    || sender.getPassword() == null || sender.getPassword().isBlank()))) {
                throw new org.springframework.mail.MailPreparationException(
                        "Password recovery SMTP settings are missing.");
            }
        }
    }

    public void request(String email) {
        checkMailConfiguration();
        Optional<User> found = users.findByEmail(email == null ? "" : email.trim().toLowerCase());
        if (found.isEmpty()) {
            return;
        }
        User user = found.get();
        tokens.deleteByUserId(user.getId());

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new PasswordResetToken(hash(token), user.getId(), Instant.now().plusSeconds(900)));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Reset your SmartTask password");
        message.setText("Reset your SmartTask password using this single-use link, valid for 15 minutes:\n\n"
                + baseUrl + "/reset-password?token=" + token
                + "\n\nIf you did not request this, ignore this email.");
        mail.send(message);
    }

    public boolean valid(String token) {
        if (!wellFormed(token)) {
            return false;
        }
        return tokens.findByHash(hash(token))
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    public void reset(String token, String password, String confirm) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (!password.equals(confirm)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at most 72 bytes.");
        }
        if (!wellFormed(token)) {
            throw invalid();
        }

        PasswordResetToken current = tokens.findByHash(hash(token)).orElseThrow(PasswordResetService::invalid);
        if (!current.getExpiresAt().isAfter(Instant.now())) {
            throw invalid();
        }

        User user = users.findById(current.getUserId()).orElseThrow(PasswordResetService::invalid);
        user.setPassword(auth.encodePassword(password));
        users.save(user);
        tokens.deleteByHash(current.getTokenHash());
        sessions.invalidate(user.getId());
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("This reset link is invalid or expired. Please request a new link.");
    }

    private static boolean wellFormed(String token) {
        return token != null && token.matches("[A-Za-z0-9_-]{43}");
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }
}
