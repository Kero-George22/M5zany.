package com.smartstock.service;

import com.smartstock.dao.UserDAO;
import com.smartstock.dao.RoleDAO;
import com.smartstock.model.User;
import java.util.List;

public class UserService {
    private final UserDAO userDAO;
    private final RoleDAO roleDAO;
    private final AuthService authService;

    public UserService(AuthService authService) {
        this.userDAO = new UserDAO();
        this.roleDAO = new RoleDAO();
        this.authService = authService;
    }

    public User createUser(String username, String plainPassword, String fullName,
                           String email, String phone, String role, Integer branchId) {
        if (!authService.isAdmin() && !authService.isManager()) {
            throw new SecurityException("Access denied.");
        }
        if (authService.isManager() && !"CASHIER".equals(role)) {
            throw new SecurityException("Managers can only create CASHIER users.");
        }
        String hashedPassword = authService.hashPassword(plainPassword);
        User user = new User(username, hashedPassword, fullName, role, branchId);
        var dbRole = roleDAO.findByName(role);
        if (dbRole != null) {
            user.setRoleId(dbRole.getRoleId());
        }
        user.setEmail(email);
        user.setPhone(phone);
        int id = userDAO.insert(user);
        if (id > 0) {
            user.setId(id);
            return user;
        }
        return null;
    }

    public boolean updateUser(User user) {
        if (!authService.isAdmin() && !authService.isManager()) {
            throw new SecurityException("Access denied.");
        }
        if (authService.isManager()) {
            User currentManager = authService.getCurrentUser();
            if (!"CASHIER".equals(user.getRole())) {
                throw new SecurityException("Managers can only update CASHIER users.");
            }
            if (currentManager.getBranchId() == null ||
                    !currentManager.getBranchId().equals(user.getBranchId())) {
                throw new SecurityException("You can only update users from your own branch.");
            }
        }
        var dbRole = roleDAO.findByName(user.getRole());
        if (dbRole != null) {
            user.setRoleId(dbRole.getRoleId());
        }
        return userDAO.update(user);
    }

    public boolean resetPassword(int userId, String newPassword) {
        if (!authService.isAdmin() && !authService.isManager()) {
            throw new SecurityException("Access denied.");
        }
        if (authService.isManager()) {
            User target = userDAO.findById(userId);
            User mgr = authService.getCurrentUser();
            if (target == null || !"CASHIER".equals(target.getRole())
                    || mgr.getBranchId() == null
                    || !mgr.getBranchId().equals(target.getBranchId())) {
                throw new SecurityException("You can only reset passwords for CASHIERs in your branch.");
            }
        }
        String hashed = authService.hashPassword(newPassword);
        return userDAO.updatePassword(userId, hashed);
    }

    public boolean deleteUser(int userId) {
        if (!authService.isAdmin() && !authService.isManager()) {
            throw new SecurityException("Access denied. Only admins and managers can delete users.");
        }
        if (authService.isManager()) {
            User target = userDAO.findById(userId);
            User mgr = authService.getCurrentUser();
            if (target == null) {
                throw new SecurityException("User not found.");
            }
            if (!"CASHIER".equals(target.getRole())) {
                throw new SecurityException("Managers can only delete CASHIER users.");
            }
            if (mgr.getBranchId() == null || !mgr.getBranchId().equals(target.getBranchId())) {
                throw new SecurityException("You can only delete users from your own branch.");
            }
        }
        return userDAO.delete(userId);
    }

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public List<User> getUsersByBranch(int branchId) {
        return userDAO.findByBranchId(branchId);
    }

    public User getUserById(int id) {
        return userDAO.findById(id);
    }
}
