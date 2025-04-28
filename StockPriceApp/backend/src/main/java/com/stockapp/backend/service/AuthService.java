package com.stockapp.backend.service;

import com.stockapp.backend.model.User;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    private static final String VALID_USERNAME = "Chaitu786";
    private static final String VALID_PASSWORD = "Manu123$";
    
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
               VALID_PASSWORD.equals(user.getPassword());
    }
}