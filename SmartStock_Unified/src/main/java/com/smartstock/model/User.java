package com.smartstock.model;

public class User implements PhantomEntity {
    private int id;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
    private String role;    // ADMIN, MANAGER, CASHIER (legacy ENUM — kept)
    private Integer roleId; // FK → roles.role_id (new)
    private Integer branchId;
    private boolean isActive;

    public User() {}

    public User(String username, String password, String fullName, String role, Integer branchId) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.branchId = branchId;
        this.isActive = true;
    }

    @Override
    public int getId() { return id; }
    @Override
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isManager() { return "MANAGER".equals(role); }
    public boolean isCashier() { return "CASHIER".equals(role); }
}
