package com.smarttask.controller;

import com.smarttask.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller
public class PasswordResetController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PasswordResetController.class);
    private record RecoveryEmail(@NotBlank @Email @Size(max = 150) String value) {}
    private final PasswordResetService service;
    private final PasswordResetRateLimiter limiter;
    private final Validator validator;
    public PasswordResetController(PasswordResetService service, PasswordResetRateLimiter limiter, Validator validator) {
        this.service = service; this.limiter = limiter; this.validator = validator;
    }
    @ModelAttribute
    public void protect(HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        HttpSession session = request.getSession();
        if (session.getAttribute("resetCsrf") == null) session.setAttribute("resetCsrf", UUID.randomUUID().toString());
        model.addAttribute("resetCsrf", session.getAttribute("resetCsrf"));
    }
    private boolean csrf(String value, HttpSession session) { return value != null && value.equals(session.getAttribute("resetCsrf")); }
    @GetMapping("/forgot-password")
    public String forgot() { return "forgot-password"; }
    @PostMapping("/forgot-password")
    public String request(@RequestParam(defaultValue = "") String email, @RequestParam(defaultValue = "") String resetCsrf,
                          HttpServletRequest request, Model model) {
        if (!csrf(resetCsrf, request.getSession())) { model.addAttribute("error", "Please reload the page and try again."); return "forgot-password"; }
        String clean = email.trim().toLowerCase(Locale.ROOT);
        if (!validator.validate(new RecoveryEmail(clean)).isEmpty()) { model.addAttribute("error", "Please enter a valid email address."); return "forgot-password"; }
        if (!limiter.allow("request-ip:" + request.getRemoteAddr(), 10) || !limiter.allow("email:" + clean, 3)) {
            model.addAttribute("error", "Too many requests. Please try again in 15 minutes.");
            return "forgot-password";
        }
        try {
            service.checkMailConfiguration();
            service.request(clean);
            return "redirect:/reset-password?sent=true";
        } catch (RuntimeException ex) {
            // Exception messages can contain message bodies or credentials; log only the type.
            log.warn("Password recovery failed ({}). Check SMTP configuration, credentials and connectivity.", ex.getClass().getSimpleName());
            model.addAttribute("error", "Unable to send a reset email right now. Please try again later.");
            return "forgot-password";
        }
    }
    @GetMapping("/reset-password")
    public String resetPage(@RequestParam(defaultValue = "") String token,
                            @RequestParam(defaultValue = "false") boolean sent, Model model) {
        model.addAttribute("awaitingEmail", sent && token.isEmpty());
        model.addAttribute("token", token);
        model.addAttribute("validToken", service.valid(token));
        return "reset-password";
    }
    @PostMapping("/reset-password")
    public String reset(@RequestParam(defaultValue = "") String token, @RequestParam(defaultValue = "") String password,
                        @RequestParam(defaultValue = "") String confirmPassword, @RequestParam(defaultValue = "") String resetCsrf,
                        HttpServletRequest request, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("validToken", true);
        if (!csrf(resetCsrf, request.getSession())) { model.addAttribute("error", "Please reload the page and try again."); return "reset-password"; }
        if (!limiter.allow("reset-ip:" + request.getRemoteAddr(), 30)) { model.addAttribute("error", "Too many attempts. Please try again in 15 minutes."); return "reset-password"; }
        try {
            service.reset(token, password, confirmPassword);
            model.addAttribute("success", "Your password has been changed. Sign in with your new password.");
            model.addAttribute("validToken", false);
        } catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); }
        return "reset-password";
    }
}
