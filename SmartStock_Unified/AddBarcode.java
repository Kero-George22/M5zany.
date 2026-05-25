import java.sql.*;
public class AddBarcode {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/smartstock_erp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", "root", "kero2006");
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO products (name, barcode, category, unit_cost, selling_price) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, "Test Product (Scanner)");
        stmt.setString(2, "6223001510451");
        stmt.setString(3, "Beverages");
        stmt.setDouble(4, 10.0);
        stmt.setDouble(5, 12.0);
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            int id = rs.getInt(1);
            PreparedStatement inv = conn.prepareStatement("INSERT INTO inventory (product_id, branch_id, quantity, min_stock) VALUES (?, ?, ?, ?)");
            inv.setInt(1, id);
            inv.setInt(2, 1);
            inv.setInt(3, 50);
            inv.setInt(4, 10);
            inv.executeUpdate();
            System.out.println("Inserted successfully as ID: " + id);
        }
    }
}
