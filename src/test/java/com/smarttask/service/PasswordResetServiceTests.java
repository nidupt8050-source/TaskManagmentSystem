package com.smarttask.service;

import com.smarttask.config.PasswordResetSessions;
import com.smarttask.model.*;
import com.smarttask.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTests {
    UserRepository users;
    PasswordResetTokenRepository tokens;
    JavaMailSender mail;
    PasswordResetSessions sessions;
    AuthService auth;
    PasswordResetService service;
    User user;
    String raw = "a".repeat(43);
    @BeforeEach void setup() {
        users = mock(UserRepository.class); tokens = mock(PasswordResetTokenRepository.class);
        mail = mock(JavaMailSender.class); sessions = mock(PasswordResetSessions.class);
        auth = new AuthService(users);
        service = new PasswordResetService(users, tokens, auth, mail, sessions, "https://example.com", "sender@example.com");
        user = new User(); user.setId(1L); user.setEmail("user@example.com");
        user.setPassword(auth.encodePassword("oldpass"));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(users.lockById(1L)).thenReturn(Optional.of(user));
    }
    @AfterEach void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }
    @Test void registeredEmailGetsRandomTokenWithOnlyHashStored() {
        final PasswordResetToken[] stored = new PasswordResetToken[1];
        doAnswer(i -> { stored[0] = i.getArgument(0); return stored[0]; }).when(tokens).saveAndFlush(any());
        service.request(user.getEmail());
        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        String text = message.getValue().getText();
        String rawToken = text.substring(text.indexOf("?token=") + 7).split("\\n")[0];
        assertEquals(43, rawToken.length());
        assertEquals(PasswordResetService.hash(rawToken), stored[0].getTokenHash());
        assertNotEquals(rawToken, stored[0].getTokenHash());
        assertTrue(stored[0].getExpiresAt().isAfter(Instant.now().plusSeconds(890)));
        assertTrue(stored[0].getExpiresAt().isBefore(Instant.now().plusSeconds(901)));
    }
    @Test void incompleteSmtpConfigurationIsRejectedBeforeLookingUpAnAccount() {
        var sender = new org.springframework.mail.javamail.JavaMailSenderImpl();
        sender.setHost("smtp.gmail.com");
        sender.getJavaMailProperties().setProperty("mail.smtp.auth", "true");
        var unconfigured = new PasswordResetService(users, tokens, auth, sender, sessions, "http://localhost:8080", "");
        assertThrows(org.springframework.mail.MailPreparationException.class, unconfigured::checkMailConfiguration);
        verifyNoInteractions(users, tokens);
    }
    @Test void unknownEmailDoesNotSendMail() {
        service.request("unknown@example.com"); verifyNoInteractions(tokens, mail);
    }
    private PasswordResetToken token(Instant expiry) {
        var token = new PasswordResetToken(PasswordResetService.hash(raw), user, expiry);
        when(tokens.findById(token.getTokenHash())).thenReturn(Optional.of(token));
        when(tokens.findByUserId(1L)).thenReturn(Optional.of(token)); return token;
    }
    @Test void resetHashesPasswordAndExistingLoginAcceptsOnlyNewPassword() {
        var token = token(Instant.now().plusSeconds(900));
        TransactionSynchronizationManager.initSynchronization();
        service.reset(raw, "newpass", "newpass");
        assertNotEquals("newpass", user.getPassword());
        assertTrue(user.getPassword().startsWith("$2"));
        assertSame(user, auth.login(user.getEmail(), "newpass"));
        assertThrows(IllegalArgumentException.class, () -> auth.login(user.getEmail(), "oldpass"));
        verify(tokens).delete(token);
        verifyNoInteractions(sessions);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        verify(sessions).invalidate(1L);
        when(tokens.findByUserId(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.reset(raw, "newpass", "newpass"));
    }
    @Test void expiredAndInvalidTokensAreRejected() {
        token(Instant.now().minusSeconds(1));
        assertFalse(service.valid(raw));
        assertThrows(IllegalArgumentException.class, () -> service.reset(raw, "newpass", "newpass"));
        assertFalse(service.valid("invalid"));
        assertThrows(IllegalArgumentException.class, () -> service.reset("invalid", "newpass", "newpass"));
        verify(users, never()).save(any());
    }
    @Test void mismatchAndShortPasswordsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.reset(raw, "newpass", "different"));
        assertThrows(IllegalArgumentException.class, () -> service.reset(raw, "short", "short"));
        verifyNoInteractions(tokens);
    }
    @Test void registrationStillHashesAndLoginWorks() {
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
        User registered = auth.register("Example", "new@example.com", "secret");
        when(users.findByEmail("new@example.com")).thenReturn(Optional.of(registered));
        assertSame(registered, auth.login("new@example.com", "secret"));
    }
    @Test void rateLimitsBoundRequests() {
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter();
        assertTrue(limiter.allow("email:test", 1)); assertFalse(limiter.allow("email:test", 1));
        assertTrue(limiter.allow("email:other", 1));
    }
}
