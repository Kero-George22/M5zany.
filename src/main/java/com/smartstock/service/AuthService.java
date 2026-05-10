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
        User user = userDAO.findByUsername(username.trim());
        if (user == null || !user.isActive()) {
            return null;
        }
        try {
            if (BCrypt.checkpw(password, user.getPassword())) {
                this.currentUser = user;
                return user;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
