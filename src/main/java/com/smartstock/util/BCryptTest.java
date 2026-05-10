package com.smartstock.util;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptTest {
    public static void main(String[] args) {
        String password = "admin123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("Hash for '" + password + "': " + hash);
        System.out.println("Verify: " + BCrypt.checkpw(password, hash));
        System.out.println("\nRun this in MySQL Workbench:");
        System.out.println("USE smartstock_erp;");
        System.out.println("UPDATE users SET password = '" + hash + "' WHERE username = 'admin';");
    }
}
