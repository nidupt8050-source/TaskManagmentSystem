package com.smarttask.controller;

import com.smarttask.service.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.ui.ExtendedModelMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetControllerTests {
    PasswordResetService service;
    PasswordResetController controller;
    MockHttpServletRequest request;
    MockHttpServletResponse response;
    ExtendedModelMap model;
    jakarta.validation.ValidatorFactory factory;
    @BeforeEach void setup() {
        service = mock(PasswordResetService.class);
        factory = Validation.buildDefaultValidatorFactory();
        controller = new PasswordResetController(service, new PasswordResetRateLimiter(), factory.getValidator());
        request = new MockHttpServletRequest(); response = new MockHttpServletResponse(); model = new ExtendedModelMap();
        controller.protect(request, response, model);
    }
    @AfterEach void close() { factory.close(); }
    String csrf() { return (String) model.get("resetCsrf"); }
    @Test void registeredAndUnknownRequestsRedirectWithoutConfirmationPopup() {
        String registered = controller.request("User@Example.com", csrf(), request, model);
        String unknown = controller.request("unknown@example.com", csrf(), request, model);
        assertEquals("redirect:/reset-password?sent=true", registered);
        assertEquals(registered, unknown);
        assertNull(model.get("success"));
        verify(service).request("user@example.com"); verify(service).request("unknown@example.com");
    }
    @Test void invalidEmailAndMissingCsrfDoNotSendMail() {
        controller.request("invalid", csrf(), request, model);
        assertNotNull(model.get("error"));
        controller.request("user@example.com", "", request, model);
        verifyNoInteractions(service);
    }
    @Test void mailFailureShowsAnErrorInsteadOfPretendingToSend() {
        doThrow(new IllegalStateException("SMTP unavailable")).when(service).request(anyString());
        controller.request("user@example.com", csrf(), request, model);
        assertNull(model.get("success")); assertNotNull(model.get("error"));
    }
    @Test void emailRateLimitShowsRetryMessage() {
        for (int i = 0; i < 4; i++) controller.request("user@example.com", csrf(), request, model);
        verify(service, times(3)).request("user@example.com"); assertNotNull(model.get("error"));
    }
    @Test void missingMailSettingsStopTheRequestAndShowAnError() {
        doThrow(new org.springframework.mail.MailPreparationException("Missing settings")).when(service).checkMailConfiguration();
        assertEquals("forgot-password", controller.request("user@example.com", csrf(), request, model));
        verify(service, never()).request(anyString());
        assertNotNull(model.get("error"));
    }
    @Test void resetErrorsAndSuccessAreDisplayedWithoutAutoLogin() {
        doThrow(new IllegalArgumentException("Passwords do not match.")).when(service).reset("token", "newpass", "different");
        controller.reset("token", "newpass", "different", csrf(), request, model);
        assertEquals("Passwords do not match.", model.get("error"));
        controller.reset("token", "newpass", "newpass", csrf(), request, model);
        assertNotNull(model.get("success")); assertEquals(false, model.get("validToken"));
        assertNull(request.getSession().getAttribute("userId"));
    }
    @Test void sensitivePagesDisableCachingAndReferrers() {
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
    }
}
