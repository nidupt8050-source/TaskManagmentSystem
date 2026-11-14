package com.smarttask.service;

import com.smarttask.model.User;
import com.smarttask.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordResetIntegrationTests {
    @Autowired PasswordResetService reset;
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired EntityManager entities;
    @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mail;

    @Test void persistedTokenIsSingleUseAndPasswordWorksWithExistingLogin() {
        String email = "reset-test-" + UUID.randomUUID() + "@example.com";
        User user = auth.register("Password reset test", email, "oldpass");
        reset.request(email);
        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        String raw = message.getValue().getText().split("token=")[1].split("\\n")[0];
        entities.flush(); entities.clear();
        assertTrue(reset.valid(raw));
        assertTrue(tokens.findById(PasswordResetService.hash(raw)).isPresent());
        reset.reset(raw, "newpass", "newpass");
        entities.flush(); entities.clear();
        assertFalse(reset.valid(raw));
        assertThrows(IllegalArgumentException.class, () -> reset.reset(raw, "newpass", "newpass"));
        assertEquals(user.getId(), auth.login(email, "newpass").getId());
        assertThrows(IllegalArgumentException.class, () -> auth.login(email, "oldpass"));
    }

    @Test void emailedLinkOpensFormAndSubmissionSavesNewPassword() throws Exception {
        String email = "reset-browser-" + UUID.randomUUID() + "@example.com";
        auth.register("Browser reset test", email, "oldpass");
        reset.request(email);
        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(message.capture());
        String text = message.getValue().getText();
        String raw = text.split("token=")[1].split("\\n")[0];
        var page = mvc.perform(get("/reset-password").param("token", raw))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"password\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"confirmPassword\"")))
                .andReturn();
        MockHttpSession session = (MockHttpSession) page.getRequest().getSession();
        mvc.perform(post("/reset-password").session(session)
                .param("token", raw).param("resetCsrf", (String) session.getAttribute("resetCsrf"))
                .param("password", "newpass").param("confirmPassword", "newpass"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Your password has been changed")));
        entities.flush(); entities.clear();
        assertNotNull(auth.login(email, "newpass"));
        assertThrows(IllegalArgumentException.class, () -> auth.login(email, "oldpass"));
        assertFalse(reset.valid(raw));
        assertNull(session.getAttribute("userId"));
    }

    @Test void recoveryTemplatesRenderAndProtectedTasksStillRequireLogin() throws Exception {
        mvc.perform(get("/forgot-password")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Send Reset Link")));
        mvc.perform(get("/reset-password").param("sent", "true")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Open the reset link in your email")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("This reset link is invalid"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("name=\"password\""))));
        mvc.perform(get("/reset-password").param("token", "invalid")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("invalid or expired")));
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Forgot Password?")));
        mvc.perform(get("/tasks")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }
}
