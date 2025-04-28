package com.stockapp.backend.service;

import com.stockapp.backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String VALID_USERNAME = "Chaitu786";
    // Encrypted password using BCrypt
    //one more
    private static final String ENCRYPTED_PASSWORD = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates a user with the provided credentials
     * @param user the user to authenticate
     * @return true if authentication is successful, false otherwise
     */
    public boolean authenticate(User user) {
        if (user == null) {
            return false;
        }

        return VALID_USERNAME.equals(user.getUsername()) && 
               passwordEncoder.matches(user.getPassword(), ENCRYPTED_PASSWORD);
    }
}
