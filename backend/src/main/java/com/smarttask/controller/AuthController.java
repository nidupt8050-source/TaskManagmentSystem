package com.smarttask.controller;

import com.smarttask.model.User;
import com.smarttask.service.AuthService;
import com.smarttask.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;
    private final TaskService taskService;

    public AuthController(AuthService authService, TaskService taskService) {
        this.authService = authService;
        this.taskService = taskService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        try {
            User user = authService.login(email, password);
            startSession(session, user);
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("email", email);
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           HttpSession session,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "Please enter your name.");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }

        if (password == null || password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters.");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }

        try {
            User user = authService.register(name, email, password);
            startSession(session, user);
            redirectAttributes.addFlashAttribute("success", "Welcome to SmartTask. Your account is ready.");
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private void startSession(HttpSession session, User user) {
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userEmail", user.getEmail());
        // Load this user's tasks into HashMap / ArrayList / PriorityQueue / BST memory layer.
        // Failures here must not block a successful login.
        try {
            taskService.getUserTasks(user.getId());
        } catch (Exception ignored) {
            // Tasks will load again when the dashboard is opened.
        }
    }
}
