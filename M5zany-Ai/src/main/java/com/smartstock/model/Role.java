package com.smartstock.model;

public class Role implements PhantomEntity {
    private int roleId;
    private String roleName;
    private String description;

    public Role() {}

    public Role(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    @Override public int getId() { return roleId; }
    @Override public void setId(int id) { this.roleId = id; }

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() { return roleName; }
}
