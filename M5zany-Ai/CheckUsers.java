import java.sql.*;
public class CheckUsers {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/smartstock_erp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
            "root", "kero2006");

        System.out.println("=== BRANCHES ===");
        ResultSet branches = conn.createStatement().executeQuery("SELECT id, name, location FROM branches");
        while (branches.next()) {
            System.out.println("ID:" + branches.getInt("id") + " | " + branches.getString("name") + " | " + branches.getString("location"));
        }

        System.out.println("\n=== USERS ===");
        ResultSet users = conn.createStatement().executeQuery("SELECT id, full_name, role, branch_id FROM users");
        while (users.next()) {
            System.out.println("ID:" + users.getInt("id") + " | " + users.getString("full_name") + " | Role:" + users.getString("role") + " | Branch:" + users.getObject("branch_id"));
        }

        System.out.println("\n=== INVENTORY (branch 1, first 5) ===");
        ResultSet inv = conn.createStatement().executeQuery("SELECT i.branch_id, p.name, i.quantity FROM inventory i JOIN products p ON i.product_id=p.id WHERE i.branch_id=1 LIMIT 5");
        while (inv.next()) {
            System.out.println("Branch:" + inv.getInt("branch_id") + " | " + inv.getString("name") + " | Qty:" + inv.getInt("quantity"));
        }
        conn.close();
    }
}
