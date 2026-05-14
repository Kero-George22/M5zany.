package com.smartstock.dao;

import com.smartstock.model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductDAO {
    public Product findById(int id) {
        String sql = "SELECT p.*, COALESCE(i.quantity, 0) as quantity, COALESCE(i.min_stock, 10) as min_stock " +
                "FROM products p LEFT JOIN inventory i ON p.id = i.product_id AND p.branch_id = i.branch_id WHERE p.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public Product findByBarcode(String barcode) {
        String sql = "SELECT p.*, COALESCE(i.quantity, 0) as quantity, COALESCE(i.min_stock, 10) as min_stock " +
                "FROM products p LEFT JOIN inventory i ON p.id = i.product_id AND p.branch_id = i.branch_id WHERE p.barcode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, barcode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    /**
     * Returns all products that have an inventory record for the given branch.
     * Uses INNER JOIN on i.branch_id only — so a product shows up at this branch
     * as long as it exists in its inventory, regardless of p.branch_id.
     */
    public List<Product> findByBranchId(int branchId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, i.quantity, i.min_stock " +
                "FROM products p INNER JOIN inventory i ON p.id = i.product_id WHERE i.branch_id = ? ORDER BY p.name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) products.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return products;
    }

    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, COALESCE(SUM(i.quantity), 0) as quantity, COALESCE(SUM(i.min_stock), 10) as min_stock " +
                "FROM products p LEFT JOIN inventory i ON p.id = i.product_id GROUP BY p.id ORDER BY p.name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) products.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return products;
    }

    public List<Product> findLowStock(int branchId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, i.quantity, i.min_stock FROM products p " +
                "INNER JOIN inventory i ON p.id = i.product_id " +
                "WHERE i.branch_id = ? AND i.quantity < i.min_stock ORDER BY (i.min_stock - i.quantity) DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) products.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return products;
    }

    public List<Product> findFastMoving(int branchId, int threshold) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, i.quantity, i.min_stock, " +
                "(SELECT COALESCE(SUM(sm.quantity), 0) FROM stock_movements sm WHERE sm.product_id = p.id " +
                "AND sm.branch_id = ? AND sm.movement_type = 'SALE' " +
                "AND sm.created_at > DATE_SUB(NOW(), INTERVAL 30 DAY)) as sale_count " +
                "FROM products p INNER JOIN inventory i ON p.id = i.product_id " +
                "WHERE i.branch_id = ? HAVING sale_count > ? ORDER BY sale_count DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, branchId);
            stmt.setInt(3, threshold);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) products.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return products;
    }

    public List<Product> findSlowMoving(int branchId, int limit) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, i.quantity, i.min_stock, " +
                "(SELECT COALESCE(SUM(sm.quantity), 0) FROM stock_movements sm WHERE sm.product_id = p.id " +
                "AND sm.branch_id = ? AND sm.movement_type = 'SALE' " +
                "AND sm.created_at > DATE_SUB(NOW(), INTERVAL 30 DAY)) as sale_count " +
                "FROM products p INNER JOIN inventory i ON p.id = i.product_id " +
                "WHERE i.branch_id = ? ORDER BY sale_count ASC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, branchId);
            stmt.setInt(3, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) products.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return products;
    }

    /** Finds products expiring within the given number of days for a branch (checks inventory.expiry_date). */
    public List<Product> findExpiringSoon(int branchId, int withinDays) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, i.quantity, i.min_stock " +
                "FROM products p INNER JOIN inventory i ON p.id = i.product_id " +
                "WHERE i.branch_id = ? AND i.expiry_date IS NOT NULL " +
                "AND DATEDIFF(i.expiry_date, CURDATE()) BETWEEN 0 AND ? ORDER BY i.expiry_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, withinDays);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) products.add(mapRow(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return products;
    }

    public int insert(Product product) {
        String sql = "INSERT INTO products (name, barcode, qr_code, qr_code_path, category, category_id, unit, unit_cost, wholesale_price, selling_price, reorder_level, description, expiry_date, branch_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getBarcode());
            // Auto-generate QR code if not provided
            String qrCode = product.getQrCode();
            String qrCodePath = product.getQrCodePath();
            if (qrCode == null || qrCode.isEmpty()) {
                com.smartstock.service.QRCodeService qrService = new com.smartstock.service.QRCodeService();
                // Generate QR code after we get the product ID
                qrCode = "AUTO"; // Placeholder
            }
            stmt.setString(3, qrCode);
            stmt.setString(4, qrCodePath);
            
            stmt.setString(5, product.getCategory());
            if (product.getCategoryId() != null) stmt.setInt(6, product.getCategoryId());
            else stmt.setNull(6, Types.INTEGER);
            stmt.setString(7, product.getUnit());
            stmt.setDouble(8, product.getUnitCost());
            stmt.setDouble(9, product.getWholesalePrice());
            stmt.setDouble(10, product.getSellingPrice());
            stmt.setInt(11, product.getReorderLevel() > 0 ? product.getReorderLevel() : 10);
            stmt.setString(12, product.getDescription());
            if (product.getExpiryDate() != null) stmt.setDate(13, Date.valueOf(product.getExpiryDate()));
            else stmt.setNull(13, Types.DATE);
            if (product.getBranchId() != null) stmt.setInt(14, product.getBranchId());
            else stmt.setNull(14, Types.INTEGER);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int productId = rs.getInt(1);
                    
                    // Auto-generate QR code after getting product ID
                    if (qrCode.equals("AUTO")) {
                        com.smartstock.service.QRCodeService qrService = new com.smartstock.service.QRCodeService();
                        String generatedPath = qrService.generateQRCode(
                                productId,
                                product.getName(),
                                product.getBarcode() != null ? product.getBarcode() : "N/A"
                        );
                        if (generatedPath != null) {
                            // Update product with generated QR code path
                            String updateSql = "UPDATE products SET qr_code = ?, qr_code_path = ? WHERE id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setString(1, generatedPath);
                                updateStmt.setString(2, generatedPath);
                                updateStmt.setInt(3, productId);
                                updateStmt.executeUpdate();
                            }
                            product.setQrCode(generatedPath);
                            product.setQrCodePath(generatedPath);
                        }
                    }
                    
                    insertInventory(productId, product.getBranchId(), product.getQuantity(), product.getMinStock());
                    return productId;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public boolean update(Product product) {
        String sql = "UPDATE products SET name=?, barcode=?, qr_code=?, qr_code_path=?, category=?, category_id=?, unit=?, unit_cost=?, wholesale_price=?, " +
                "selling_price=?, reorder_level=?, description=?, expiry_date=? WHERE id=?";
        Double oldPrice = null;
        Product old = findById(product.getId());
        if (old != null) oldPrice = old.getSellingPrice();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getBarcode());
            stmt.setString(3, product.getQrCode());
            stmt.setString(4, product.getQrCodePath());
            stmt.setString(5, product.getCategory());
            if (product.getCategoryId() != null) stmt.setInt(6, product.getCategoryId());
            else stmt.setNull(6, Types.INTEGER);
            stmt.setString(7, product.getUnit());
            stmt.setDouble(8, product.getUnitCost());
            stmt.setDouble(9, product.getWholesalePrice());
            stmt.setDouble(10, product.getSellingPrice());
            stmt.setInt(11, product.getReorderLevel() > 0 ? product.getReorderLevel() : 10);
            stmt.setString(12, product.getDescription());
            if (product.getExpiryDate() != null) stmt.setDate(13, Date.valueOf(product.getExpiryDate()));
            else stmt.setNull(13, Types.DATE);
            stmt.setInt(14, product.getId());
            boolean ok = stmt.executeUpdate() > 0;
            if (ok && oldPrice != null && oldPrice > 0) {
                double changePct = Math.abs(product.getSellingPrice() - oldPrice) / oldPrice;
                if (changePct >= 0.30) {
                    AlertDAO alertDAO = new AlertDAO();
                    if (!alertDAO.existsUnreadOrRecentSimilar("SYSTEM", product.getId(), product.getBranchId())) {
                        com.smartstock.model.Alert alert = new com.smartstock.model.Alert();
                        alert.setType("SYSTEM");
                        alert.setProductId(product.getId());
                        alert.setBranchId(product.getBranchId());
                        alert.setSeverity(changePct >= 0.50 ? "CRITICAL" : "WARNING");
                        alert.setMessage("Abnormal price change: '" + product.getName() + "' changed from " +
                                String.format("%.2f", oldPrice) + " to " + String.format("%.2f", product.getSellingPrice()) +
                                " (" + (int) (changePct * 100) + "%).");
                        alertDAO.insert(alert);
                    }
                }
            }
            return ok;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private void insertInventory(int productId, Integer branchId, int quantity, int minStock) {
        String sql = "INSERT INTO inventory (product_id, branch_id, quantity, min_stock) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, branchId);
            stmt.setInt(3, quantity);
            stmt.setInt(4, minStock > 0 ? minStock : 10);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getInt("id"));
        product.setName(rs.getString("name"));
        product.setBarcode(rs.getString("barcode"));
        product.setQrCode(rs.getString("qr_code"));
        try { product.setQrCodePath(rs.getString("qr_code_path")); } catch (SQLException ignored) {}
        product.setCategory(rs.getString("category"));
        // New fields — safe fallback if column missing in older query
        try { product.setCategoryId(rs.getObject("category_id", Integer.class)); } catch (SQLException ignored) {}
        try { product.setUnit(rs.getString("unit")); } catch (SQLException ignored) {}
        product.setUnitCost(rs.getDouble("unit_cost"));
        try { product.setWholesalePrice(rs.getDouble("wholesale_price")); } catch (SQLException ignored) {}
        product.setSellingPrice(rs.getDouble("selling_price"));
        try { product.setReorderLevel(rs.getInt("reorder_level")); } catch (SQLException ignored) {}
        try { product.setDescription(rs.getString("description")); } catch (SQLException ignored) {}
        Date expiryDate = rs.getDate("expiry_date");
        if (expiryDate != null) product.setExpiryDate(expiryDate.toLocalDate());
        product.setBranchId(rs.getObject("branch_id", Integer.class));
        try { product.setQuantity(rs.getInt("quantity")); } catch (SQLException ignored) {}
        try { product.setMinStock(rs.getInt("min_stock")); } catch (SQLException ignored) {}
        return product;
    }

    public int backfillMissingCategories(Map<String, Integer> categoryNameToId) {
        if (categoryNameToId == null || categoryNameToId.isEmpty()) return 0;
        Integer defaultId = categoryNameToId.get("canned & packaged");
        if (defaultId == null) return 0;

        int updated = 0;
        String selectSql = "SELECT id, name, category FROM products WHERE category_id IS NULL OR category IS NULL OR TRIM(category) = ''";
        String updateSql = "UPDATE products SET category=?, category_id=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             ResultSet rs = selectStmt.executeQuery();
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            while (rs.next()) {
                int productId = rs.getInt("id");
                String productName = rs.getString("name");
                String currentCategory = rs.getString("category");

                String resolvedCategory = resolveCategory(productName, currentCategory, categoryNameToId);
                Integer resolvedId = categoryNameToId.getOrDefault(resolvedCategory.toLowerCase(Locale.ROOT), defaultId);

                updateStmt.setString(1, resolvedCategory);
                updateStmt.setInt(2, resolvedId);
                updateStmt.setInt(3, productId);
                updated += updateStmt.executeUpdate();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return updated;
    }

    private String resolveCategory(String productName, String currentCategory, Map<String, Integer> categoryNameToId) {
        if (currentCategory != null && !currentCategory.isBlank()) {
            String normalized = currentCategory.trim().toLowerCase(Locale.ROOT);
            if (categoryNameToId.containsKey(normalized)) return toDisplayName(normalized);
        }
        String name = productName == null ? "" : productName.toLowerCase(Locale.ROOT);
        Map<String, String> keywords = new HashMap<>();
        keywords.put("dairy & eggs", "milk|cheese|yogurt|butter|egg");
        keywords.put("beverages", "water|juice|drink|cola|pepsi|7up|tea|coffee");
        keywords.put("snacks", "chips|biscuit|chocolate|candy|nuts");
        keywords.put("grains & staples", "rice|flour|pasta|sugar|salt|oil");
        keywords.put("canned & packaged", "canned|beans|tuna|sauce|ketchup|mayo");
        keywords.put("cleaning & household", "detergent|dish|fabric|trash|clean");
        keywords.put("personal care", "shampoo|toothpaste|deodorant|razor|soap");
        keywords.put("frozen foods", "frozen|ice cream");
        keywords.put("bread & bakery", "bread|toast|bun|pita|croissant");
        keywords.put("meat & poultry", "chicken|beef|meat|sausage|poultry");

        for (Map.Entry<String, String> entry : keywords.entrySet()) {
            for (String token : entry.getValue().split("\\|")) {
                if (name.contains(token.trim())) return toDisplayName(entry.getKey());
            }
        }
        return "Canned & Packaged";
    }

    private String toDisplayName(String normalizedCategory) {
        return switch (normalizedCategory) {
            case "dairy & eggs" -> "Dairy & Eggs";
            case "beverages" -> "Beverages";
            case "snacks" -> "Snacks";
            case "grains & staples" -> "Grains & Staples";
            case "canned & packaged" -> "Canned & Packaged";
            case "cleaning & household" -> "Cleaning & Household";
            case "personal care" -> "Personal Care";
            case "frozen foods" -> "Frozen Foods";
            case "bread & bakery" -> "Bread & Bakery";
            case "meat & poultry" -> "Meat & Poultry";
            default -> "Canned & Packaged";
        };
    }

    /**
     * Gets all product IDs
     */
    public List<Integer> getAllProductIds() {
        List<Integer> productIds = new ArrayList<>();
        String sql = "SELECT id FROM products ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                productIds.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return productIds;
    }
}
