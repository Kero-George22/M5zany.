import java.sql.*;
public class DBTest {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/smartstock_erp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", "root", "kero2006");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("DESCRIBE pricing_history");
        while (rs.next()) {
            System.out.println(rs.getString("Field") + " - " + rs.getString("Type"));
        }
        System.out.println("---------");
        rs = stmt.executeQuery("SELECT * FROM pricing_history LIMIT 10");
        while(rs.next()) {
            System.out.println("Row: " + rs.getInt("id") + " " + rs.getInt("product_id"));
        }
    }
}
