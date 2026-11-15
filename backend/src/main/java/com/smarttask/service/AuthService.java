package com.smarttask.service;

import com.smarttask.database.UserDao;
import com.smarttask.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserDao userDao;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    public User register(String name, String email, String password) {
        String cleanEmail = email.trim().toLowerCase();

        if (userDao.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(password));
        return userDao.save(user);
    }

    public User login(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter your email.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Please enter your password.");
        }

        String cleanEmail = email.trim().toLowerCase();
        User user = userDao.findByEmail(cleanEmail)
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
