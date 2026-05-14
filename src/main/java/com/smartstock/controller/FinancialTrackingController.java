package com.smartstock.controller;

import com.smartstock.dao.FinancialSummaryDAO;
import com.smartstock.model.Branch;
import com.smartstock.model.FinancialSummary;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.application.Platform;
import java.time.LocalDate;

public class FinancialTrackingController {

    @FXML private Label branchNameLabel;
    @FXML private Label branchIdLabel;
    @FXML private Label revenueLabel;
    @FXML private Label expensesLabel;
    @FXML private Label profitLabel;
    @FXML private Label marginLabel;
    @FXML private Label transactionCountLabel;
    @FXML private Label avgTicketLabel;

    private Branch currentBranch;
    private final FinancialSummaryDAO financialDAO = new FinancialSummaryDAO();

    /**
     * Receives branch data from the navigation source.
     * Triggers real-time data fetch.
     */
    public void setBranchData(Branch branch) {
        this.currentBranch = branch;
        updateUI();
    }

    private void updateUI() {
        if (currentBranch == null) return;

        branchNameLabel.setText(currentBranch.getName());
        branchIdLabel.setText("Branch ID: #" + currentBranch.getId());

        // Fetch data in background to keep UI responsive
        new Thread(() -> {
            // First, recalculate today's stats from transactions
            financialDAO.recalculateToday(currentBranch.getId());
            
            // Then fetch the latest summary
            FinancialSummary summary = financialDAO.findByBranchAndDate(currentBranch.getId(), LocalDate.now());
            
            Platform.runLater(() -> {
                if (summary != null) {
                    revenueLabel.setText(String.format("EGP %.2f", summary.getTotalRevenue()));
                    expensesLabel.setText(String.format("EGP %.2f", summary.getTotalCost()));
                    profitLabel.setText(String.format("EGP %.2f", summary.getTotalProfit()));
                    marginLabel.setText(String.format("Margin: %.1f%%", summary.getProfitMargin()));
                    transactionCountLabel.setText(String.valueOf(summary.getTransactionCount()));
                    
                    double avg = summary.getTransactionCount() > 0 ? summary.getTotalRevenue() / summary.getTransactionCount() : 0;
                    avgTicketLabel.setText(String.format("EGP %.2f", avg));
                } else {
                    resetToZero();
                }
            });
        }).start();
    }

    private void resetToZero() {
        revenueLabel.setText("EGP 0.00");
        expensesLabel.setText("EGP 0.00");
        profitLabel.setText("EGP 0.00");
        marginLabel.setText("Margin: 0%");
        transactionCountLabel.setText("0");
        avgTicketLabel.setText("EGP 0.00");
    }

    @FXML
    private void refreshData() {
        updateUI();
    }

    @FXML
    private void handleClose() {
        // This assumes we are inside the AdminDashboard content host
        // We can navigate back to the Branches overview
        try {
            // We need a way to get back. Since we don't have a direct reference to the dashboard,
            // we can try to find it via the scene root or just use a helper.
            // For now, let's just trigger a refresh if needed or rely on the dashboard sidebar.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
