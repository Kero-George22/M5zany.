package com.smartstock.controller;

import com.smartstock.dao.CycleCountLogDAO;
import com.smartstock.dao.InventoryDAO;
import com.smartstock.dao.ProductDAO;
import com.smartstock.model.CycleCountLog;
import com.smartstock.model.Product;
import com.smartstock.service.CycleCountingService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for Cycle Counting Interface
 * Handles daily cycle count suggestions and logging
 */
public class CycleCountingController {

    @FXML private Label branchLabel;
    @FXML private Label dateLabel;
    @FXML private Label totalCountsLabel;
    @FXML private Label discrepanciesLabel;
    @FXML private Label accuracyRateLabel;
    @FXML private Label lastCountLabel;
    
    @FXML private TableView<CountListItem> countListTable;
    @FXML private TableColumn<CountListItem, Integer> productIdColumn;
    @FXML private TableColumn<CountListItem, String> productNameColumn;
    @FXML private TableColumn<CountListItem, Integer> expectedQtyColumn;
    @FXML private TableColumn<CountListItem, Integer> countedQtyColumn;
    @FXML private TableColumn<CountListItem, Integer> discrepancyColumn;
    @FXML private TableColumn<CountListItem, String> notesColumn;
    @FXML private TableColumn<CountListItem, String> actionColumn;
    
    @FXML private ComboBox<Product> productComboBox;
    @FXML private TextField expectedQtyField;
    @FXML private TextField countedQtyField;
    @FXML private TextField notesField;
    
    @FXML private TableView<HistoryItem> historyTable;
    @FXML private TableColumn<HistoryItem, String> historyDateColumn;
    @FXML private TableColumn<HistoryItem, String> historyProductColumn;
    @FXML private TableColumn<HistoryItem, Integer> historyExpectedColumn;
    @FXML private TableColumn<HistoryItem, Integer> historyCountedColumn;
    @FXML private TableColumn<HistoryItem, Integer> historyDiscrepancyColumn;
    @FXML private TableColumn<HistoryItem, String> historyCountedByColumn;
    @FXML private TableColumn<HistoryItem, String> historyNotesColumn;
    
    private final CycleCountingService cycleCountingService;
    private final CycleCountLogDAO cycleCountLogDAO;
    private final InventoryDAO inventoryDAO;
    private final ProductDAO productDAO;
    
    private int currentBranchId;
    private ObservableList<CountListItem> countListItems;
    private ObservableList<HistoryItem> historyItems;
    
    public CycleCountingController() {
        this.cycleCountingService = new CycleCountingService();
        this.cycleCountLogDAO = new CycleCountLogDAO();
        this.inventoryDAO = new InventoryDAO();
        this.productDAO = new ProductDAO();
    }
    
    @FXML
    public void initialize() {
        // Initialize table columns
        initializeColumns();
        
        // Set default branch (would normally come from session)
        currentBranchId = 1;
        
        // Load products into combo box
        loadProducts();
        
        // Load today's date
        dateLabel.setText("Date: " + LocalDate.now().toString());
        branchLabel.setText("Branch: " + getBranchName(currentBranchId));
        
        // Load statistics
        loadStatistics();
        
        // Load history
        loadHistory();
        
        // Generate initial daily list
        handleGenerateDailyList();
    }
    
    private void initializeColumns() {
        // Count list table columns
        productIdColumn.setCellValueFactory(cellData -> cellData.getValue().productIdProperty().asObject());
        productNameColumn.setCellValueFactory(cellData -> cellData.getValue().productNameProperty());
        expectedQtyColumn.setCellValueFactory(cellData -> cellData.getValue().expectedQtyProperty().asObject());
        countedQtyColumn.setCellValueFactory(cellData -> cellData.getValue().countedQtyProperty().asObject());
        discrepancyColumn.setCellValueFactory(cellData -> cellData.getValue().discrepancyProperty().asObject());
        notesColumn.setCellValueFactory(cellData -> cellData.getValue().notesProperty());
        actionColumn.setCellValueFactory(cellData -> cellData.getValue().actionProperty());
        
        // History table columns
        historyDateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        historyProductColumn.setCellValueFactory(cellData -> cellData.getValue().productProperty());
        historyExpectedColumn.setCellValueFactory(cellData -> cellData.getValue().expectedProperty().asObject());
        historyCountedColumn.setCellValueFactory(cellData -> cellData.getValue().countedProperty().asObject());
        historyDiscrepancyColumn.setCellValueFactory(cellData -> cellData.getValue().discrepancyProperty().asObject());
        historyCountedByColumn.setCellValueFactory(cellData -> cellData.getValue().countedByProperty());
        historyNotesColumn.setCellValueFactory(cellData -> cellData.getValue().notesProperty());
        
        countListItems = FXCollections.observableArrayList();
        countListTable.setItems(countListItems);
        
        historyItems = FXCollections.observableArrayList();
        historyTable.setItems(historyItems);
    }
    
    private void loadProducts() {
        List<Product> products = productDAO.findByBranchId(currentBranchId);
        productComboBox.setItems(FXCollections.observableArrayList(products));
        
        productComboBox.setOnAction(e -> {
            Product selected = productComboBox.getValue();
            if (selected != null) {
                int expectedQty = inventoryDAO.getQuantity(selected.getId(), currentBranchId);
                expectedQtyField.setText(String.valueOf(expectedQty));
            }
        });
    }
    
    private void loadStatistics() {
        var stats = cycleCountingService.getCycleCountStatistics(currentBranchId);
        totalCountsLabel.setText(String.valueOf(stats.getOrDefault("totalCounts", 0)));
        discrepanciesLabel.setText(String.valueOf(stats.getOrDefault("discrepancyCount", 0)));
        accuracyRateLabel.setText(String.format("%.1f%%", (Double) stats.getOrDefault("accuracyRate", 0.0)));
        
        LocalDate lastDate = (LocalDate) stats.get("lastCountDate");
        lastCountLabel.setText(lastDate != null ? lastDate.toString() : "Never");
    }
    
    private void loadHistory() {
        List<CycleCountLog> logs = cycleCountLogDAO.findByBranch(currentBranchId);
        historyItems.clear();
        
        for (CycleCountLog log : logs) {
            HistoryItem item = new HistoryItem(
                    log.getCountDate() != null ? log.getCountDate().toString() : "-",
                    log.getProductName() != null ? log.getProductName() : "Product " + log.getProductId(),
                    log.getExpectedQty() != null ? log.getExpectedQty() : 0,
                    log.getCountedQty() != null ? log.getCountedQty() : 0,
                    log.getDiscrepancy() != null ? log.getDiscrepancy() : 0,
                    log.getCountedByName() != null ? log.getCountedByName() : "-",
                    log.getNotes() != null ? log.getNotes() : ""
            );
            historyItems.add(item);
        }
    }
    
    @FXML
    private void handleGenerateDailyList() {
        List<Integer> productIds = cycleCountingService.generateDailyCountList(
                currentBranchId, LocalDate.now(), 20
        );
        
        countListItems.clear();
        
        for (Integer productId : productIds) {
            Product product = productDAO.findById(productId);
            int expectedQty = inventoryDAO.getQuantity(productId, currentBranchId);
            
            CountListItem item = new CountListItem(
                    productId,
                    product != null ? product.getName() : "Unknown",
                    expectedQty,
                    null,
                    null,
                    "",
                    "Check-off"
            );
            countListItems.add(item);
        }
        
        showAlert("Success", "Generated " + productIds.size() + " products for daily cycle count", Alert.AlertType.INFORMATION);
    }
    
    @FXML
    private void handleSuggest() {
        // Generate new suggestions
        handleGenerateDailyList();
    }
    
    @FXML
    private void handleCheckOff() {
        CountListItem selectedItem = countListTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert("Error", "Please select a product to check off", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            int productId = selectedItem.productIdProperty().get();
            int expectedQty = selectedItem.expectedQtyProperty().get();
            
            // Log the count with expected qty as counted qty (no discrepancy)
            int countId = cycleCountingService.logCycleCount(
                    currentBranchId,
                    productId,
                    LocalDate.now(),
                    expectedQty,
                    expectedQty, // Counted equals expected (no discrepancy)
                    getCurrentUserId(),
                    "Auto check-off - no discrepancy"
            );
            
            if (countId > 0) {
                // Update the item to show it's checked off
                selectedItem.setCountedQty(expectedQty);
                selectedItem.setDiscrepancy(0);
                selectedItem.setNotes("Checked off");
                selectedItem.setAction("Completed");
                
                // Refresh the table
                countListTable.refresh();
                
                showAlert("Success", "Product checked off successfully", Alert.AlertType.INFORMATION);
                
                // Refresh statistics
                loadStatistics();
                loadHistory();
            } else {
                showAlert("Error", "Failed to check off product", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            showAlert("Error", "Error checking off product: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleRefresh() {
        loadStatistics();
        loadHistory();
    }
    
    @FXML
    private void handleSaveCount() {
        Product selectedProduct = productComboBox.getValue();
        if (selectedProduct == null) {
            showAlert("Error", "Please select a product", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            int expectedQty = Integer.parseInt(expectedQtyField.getText());
            int countedQty = Integer.parseInt(countedQtyField.getText());
            String notes = notesField.getText();
            
            // Log the count
            int countId = cycleCountingService.logCycleCount(
                    currentBranchId,
                    selectedProduct.getId(),
                    LocalDate.now(),
                    expectedQty,
                    countedQty,
                    getCurrentUserId(), // Would come from session
                    notes
            );
            
            if (countId > 0) {
                showAlert("Success", "Count saved successfully", Alert.AlertType.INFORMATION);
                
                // Clear fields
                countedQtyField.clear();
                notesField.clear();
                
                // Refresh
                loadStatistics();
                loadHistory();
            } else {
                showAlert("Error", "Failed to save count", Alert.AlertType.ERROR);
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter valid quantities", Alert.AlertType.ERROR);
        }
    }
    
    private String getBranchName(int branchId) {
        // This would use BranchDAO to get the branch name
        // For now, return a default
        return "Branch " + branchId;
    }
    
    private int getCurrentUserId() {
        // This would come from the current user session
        // For now, return a default
        return 1;
    }
    
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner class for count list table items
    public static class CountListItem {
        private final SimpleIntegerProperty productId;
        private final SimpleStringProperty productName;
        private final SimpleIntegerProperty expectedQty;
        private final SimpleIntegerProperty countedQty;
        private final SimpleIntegerProperty discrepancy;
        private final SimpleStringProperty notes;
        private final SimpleStringProperty action;
        
        public CountListItem(int productId, String productName, int expectedQty, 
                             Integer countedQty, Integer discrepancy, String notes, String action) {
            this.productId = new SimpleIntegerProperty(productId);
            this.productName = new SimpleStringProperty(productName);
            this.expectedQty = new SimpleIntegerProperty(expectedQty);
            this.countedQty = new SimpleIntegerProperty(countedQty != null ? countedQty : 0);
            this.discrepancy = new SimpleIntegerProperty(discrepancy != null ? discrepancy : 0);
            this.notes = new SimpleStringProperty(notes);
            this.action = new SimpleStringProperty(action);
        }
        
        public SimpleIntegerProperty productIdProperty() { return productId; }
        public SimpleStringProperty productNameProperty() { return productName; }
        public SimpleIntegerProperty expectedQtyProperty() { return expectedQty; }
        public SimpleIntegerProperty countedQtyProperty() { return countedQty; }
        public SimpleIntegerProperty discrepancyProperty() { return discrepancy; }
        public SimpleStringProperty notesProperty() { return notes; }
        public SimpleStringProperty actionProperty() { return action; }
        
        // Setter methods for updating values
        public void setCountedQty(int qty) { this.countedQty.set(qty); }
        public void setDiscrepancy(int disc) { this.discrepancy.set(disc); }
        public void setNotes(String note) { this.notes.set(note); }
        public void setAction(String act) { this.action.set(act); }
    }
    
    // Inner class for history table items
    public static class HistoryItem {
        private final SimpleStringProperty date;
        private final SimpleStringProperty product;
        private final SimpleIntegerProperty expected;
        private final SimpleIntegerProperty counted;
        private final SimpleIntegerProperty discrepancy;
        private final SimpleStringProperty countedBy;
        private final SimpleStringProperty notes;
        
        public HistoryItem(String date, String product, int expected, int counted, 
                          int discrepancy, String countedBy, String notes) {
            this.date = new SimpleStringProperty(date);
            this.product = new SimpleStringProperty(product);
            this.expected = new SimpleIntegerProperty(expected);
            this.counted = new SimpleIntegerProperty(counted);
            this.discrepancy = new SimpleIntegerProperty(discrepancy);
            this.countedBy = new SimpleStringProperty(countedBy);
            this.notes = new SimpleStringProperty(notes);
        }
        
        public SimpleStringProperty dateProperty() { return date; }
        public SimpleStringProperty productProperty() { return product; }
        public SimpleIntegerProperty expectedProperty() { return expected; }
        public SimpleIntegerProperty countedProperty() { return counted; }
        public SimpleIntegerProperty discrepancyProperty() { return discrepancy; }
        public SimpleStringProperty countedByProperty() { return countedBy; }
        public SimpleStringProperty notesProperty() { return notes; }
    }
}
