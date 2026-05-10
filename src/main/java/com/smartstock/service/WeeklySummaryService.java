package com.smartstock.service;

import com.smartstock.dao.BranchDAO;
import com.smartstock.dao.ProductDAO;
import com.smartstock.dao.AlertDAO;
import com.smartstock.model.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.sql.*;
import java.util.stream.Collectors;

public class WeeklySummaryService {
    private final BranchDAO branchDAO;
    private final ProductDAO productDAO;
    private final AlertDAO alertDAO;

    public WeeklySummaryService() {
        this.branchDAO = new BranchDAO();
        this.productDAO = new ProductDAO();
        this.alertDAO = new AlertDAO();
    }

    public String generateSummary() {
        List<Branch> branches = branchDAO.findAll();
        StringBuilder report = new StringBuilder();

        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + " (Weekly Summary)";

        report.append("=== SmartStock ERP - Weekly AI Summary ===\n");
        report.append("Period: ").append(period).append("\n\n");

        for (Branch branch : branches) {
            report.append("--- Branch: ").append(branch.getName()).append(" ---\n");

            List<Product> lowStock = productDAO.findLowStock(branch.getId());
            List<Product> fastMoving = productDAO.findFastMoving(branch.getId(), 5);

            report.append("Products: ").append(branch.getProductCount()).append("\n");
            report.append("Total Inventory: ").append(branch.getTotalQuantity()).append(" units\n");
            report.append("Low Stock Items: ").append(branch.getLowStockCount()).append("\n");

            if (!lowStock.isEmpty()) {
                report.append("\nLow-Stock Products:\n");
                for (Product p : lowStock) {
                    report.append("  - ").append(p.getName())
                            .append(" (Qty: ").append(p.getQuantity())
                            .append(", Min: ").append(p.getMinStock()).append(")\n");
                }
            }

            if (!fastMoving.isEmpty()) {
                report.append("\nFast-Moving Products (Top Sellers):\n");
                for (Product p : fastMoving) {
                    report.append("  - ").append(p.getName())
                            .append(" (Price: ").append(String.format("%.2f", p.getSellingPrice())).append(")\n");
                }
            }
            report.append("\n");

            // Build CSV-like strings for new weekly_summaries columns
            String lowStockNames = lowStock.stream().map(Product::getName).collect(Collectors.joining(", "));
            String fastMovingNames = fastMoving.stream().map(Product::getName).collect(Collectors.joining(", "));

            saveSummary(report.toString(), period, branch.getId(), lowStockNames, fastMovingNames);
        }

        report.append("=== End of Summary ===\n");
        sendAlertToAdmin(report.toString());
        return report.toString();
    }

    public String generateAISummary() {
        AIService aiService = new AIService();
        String aiSummary = aiService.generateWeeklySummary();
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " (AI Summary)";
        saveSummary(aiSummary, period, null, null, null);
        sendAlertToAdmin(aiSummary);
        return aiSummary;
    }

    private String generateRecommendation(List<Branch> branches) {
        int totalLowStock = branches.stream().mapToInt(Branch::getLowStockCount).sum();
        if (totalLowStock == 0) return "All branches are well-stocked. No action needed.";
        if (totalLowStock < 5) return "Minor restocking needed for " + totalLowStock + " items across branches.";
        return "Significant restocking required for " + totalLowStock + " items. Consider bulk purchase.";
    }

    /**
     * Saves a weekly summary including the new low_stock_products and fast_moving_products columns.
     */
    private void saveSummary(String summary, String period, Integer branchId,
                              String lowStockProducts, String fastMovingProducts) {
        String sql = "INSERT INTO weekly_summaries (branch_id, summary_text, report_period, low_stock_products, fast_moving_products) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = com.smartstock.dao.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (branchId != null) stmt.setInt(1, branchId);
            else stmt.setNull(1, Types.INTEGER);
            stmt.setString(2, summary);
            stmt.setString(3, period);
            stmt.setString(4, lowStockProducts);
            stmt.setString(5, fastMovingProducts);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the timestamp of the most recent weekly summary, or null if none exists.
     * Used by WeeklyReportTask to determine if a new run is needed.
     */
    public LocalDateTime getLastSummaryTimestamp() {
        String sql = "SELECT MAX(generated_at) FROM weekly_summaries";
        try (Connection conn = com.smartstock.dao.DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getTimestamp(1) != null) {
                return rs.getTimestamp(1).toLocalDateTime();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendAlertToAdmin(String summary) {
        Alert alert = new Alert();
        alert.setType("SYSTEM");
        alert.setMessage("Weekly Summary generated:\n" + summary.substring(0, Math.min(500, summary.length())));
        alert.setSeverity("INFO");
        alertDAO.insert(alert);
    }
}
