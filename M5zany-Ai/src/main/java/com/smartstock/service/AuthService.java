package com.smartstock.service;

import com.smartstock.dao.UserDAO;
import com.smartstock.model.User;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
    private final UserDAO userDAO;
    private User currentUser;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public User login(String username, String password) {
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            return null;
        }
        System.out.println("====== LOGIN ATTEMPT ======");
        System.out.println("Username entered: " + username);
        
        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            System.out.println("ERROR: User not found in DB! (Check if database 'smartstock_erp' exists and has users)");
            return null;
        }
        if (!user.isActive()) {
            System.out.println("ERROR: User is inactive!");
            return null;
        }
        
        System.out.println("User found in DB! (Bypassing password check for debugging...)");
        this.currentUser = user;
        return user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean hasRole(String role) {
        return currentUser != null && currentUser.getRole().equals(role);
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public boolean isManager() {
        return currentUser != null && currentUser.isManager();
    }

    public boolean isCashier() {
        return currentUser != null && currentUser.isCashier();
    }

    public String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }
}
