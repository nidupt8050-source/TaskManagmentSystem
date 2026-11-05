package com.smarttask.service;

import com.smarttask.model.User;
import com.smarttask.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String name, String email, String password) {
        String cleanEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public User login(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter your email.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Please enter your password.");
        }

        String cleanEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No account found for \"" + cleanEmail + "\". Please sign up first."));

        String storedHash = user.getPassword();
        if (storedHash == null || storedHash.isBlank()
                || !passwordEncoder.matches(password, storedHash)) {
            throw new IllegalArgumentException("Incorrect password. Please try again.");
        }

        return user;
    }
}
