package com.smartstock.controller;

import com.smartstock.dao.BranchDAO;
import com.smartstock.dao.StockMovementDAO;
import com.smartstock.model.Branch;
import com.smartstock.model.StockMovement;
import com.smartstock.service.ResourceTrackingService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for Resource Tracking &amp; History Dashboard
 * Shows incoming/outgoing quantities per branch over time
 */
public class ResourceTrackingController {

    @FXML private ComboBox<Branch> branchComboBox;
    @FXML private ComboBox<String> periodComboBox;
    
    @FXML private Label totalInLabel;
    @FXML private Label totalOutLabel;
    @FXML private Label totalTransferLabel;
    @FXML private Label totalAdjustmentLabel;
    @FXML private Label netMovementLabel;
    
    @FXML private TextField searchField;
    @FXML private TableView<MovementItem> movementTable;
    @FXML private TableColumn<MovementItem, String> dateColumn;
    @FXML private TableColumn<MovementItem, String> typeColumn;
    @FXML private TableColumn<MovementItem, String> productColumn;
    @FXML private TableColumn<MovementItem, String> branchColumn;
    @FXML private TableColumn<MovementItem, Integer> quantityColumn;
    @FXML private TableColumn<MovementItem, String> userColumn;
    @FXML private TableColumn<MovementItem, String> notesColumn;
    
    @FXML private TableView<DailyTrendItem> dailyTrendTable;
    @FXML private TableColumn<DailyTrendItem, String> trendDateColumn;
    @FXML private TableColumn<DailyTrendItem, Integer> trendInColumn;
    @FXML private TableColumn<DailyTrendItem, Integer> trendOutColumn;
    @FXML private TableColumn<DailyTrendItem, Integer> trendNetColumn;
    
    private final ResourceTrackingService resourceTrackingService;
    private final StockMovementDAO stockMovementDAO;
    private final BranchDAO branchDAO;
    
    private ObservableList<MovementItem> movementItems;
    private ObservableList<DailyTrendItem> trendItems;
    
    public ResourceTrackingController() {
        this.resourceTrackingService = new ResourceTrackingService();
        this.stockMovementDAO = new StockMovementDAO();
        this.branchDAO = new BranchDAO();
    }
    
    @FXML
    public void initialize() {
        // Initialize table columns
        initializeColumns();
        
        // Load branches
        loadBranches();
        
        // Load time periods
        loadPeriods();
        
        // Initialize data lists
        movementItems = FXCollections.observableArrayList();
        movementTable.setItems(movementItems);
        
        trendItems = FXCollections.observableArrayList();
        dailyTrendTable.setItems(trendItems);
        
        // Set default selection
        periodComboBox.getSelectionModel().selectFirst();
        
        // Load initial data
        handleRefresh();
    }
    
    private void initializeColumns() {
        // Movement table columns
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        typeColumn.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        productColumn.setCellValueFactory(cellData -> cellData.getValue().productProperty());
        branchColumn.setCellValueFactory(cellData -> cellData.getValue().branchProperty());
        quantityColumn.setCellValueFactory(cellData -> cellData.getValue().quantityProperty().asObject());
        userColumn.setCellValueFactory(cellData -> cellData.getValue().userProperty());
        notesColumn.setCellValueFactory(cellData -> cellData.getValue().notesProperty());
        
        // Daily trend table columns
        trendDateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        trendInColumn.setCellValueFactory(cellData -> cellData.getValue().inProperty().asObject());
        trendOutColumn.setCellValueFactory(cellData -> cellData.getValue().outProperty().asObject());
        trendNetColumn.setCellValueFactory(cellData -> cellData.getValue().netProperty().asObject());
    }
    
    private void loadBranches() {
        List<Branch> branches = branchDAO.findAll();
        branchComboBox.setItems(FXCollections.observableArrayList(branches));
        
        if (!branches.isEmpty()) {
            branchComboBox.getSelectionModel().selectFirst();
        }
        
        branchComboBox.setOnAction(e -> handleRefresh());
    }
    
    private void loadPeriods() {
        ObservableList<String> periods = FXCollections.observableArrayList(
                "Last 7 Days",
                "Last 30 Days",
                "Last 90 Days",
                "Last 365 Days"
        );
        periodComboBox.setItems(periods);
        
        periodComboBox.setOnAction(e -> handleRefresh());
    }
    
    @FXML
    private void handleRefresh() {
        Branch selectedBranch = branchComboBox.getValue();
        if (selectedBranch == null) {
            return;
        }
        
        int branchId = selectedBranch.getId();
        int days = getDaysFromPeriod();
        
        // Load movement summary
        loadMovementSummary(branchId, days);
        
        // Load movement history
        loadMovementHistory(branchId);
        
        // Load daily trends
        loadDailyTrends(branchId, days);
    }
    
    private int getDaysFromPeriod() {
        String selected = periodComboBox.getValue();
        if (selected == null) return 30;
        
        return switch (selected) {
            case "Last 7 Days" -> 7;
            case "Last 30 Days" -> 30;
            case "Last 90 Days" -> 90;
            case "Last 365 Days" -> 365;
            default -> 30;
        };
    }
    
    private void loadMovementSummary(int branchId, int days) {
        Map<String, Integer> summary = resourceTrackingService.getBranchMovementSummary(branchId, days);
        
        int totalIn = summary.getOrDefault("in", 0);
        int totalOut = summary.getOrDefault("out", 0);
        int totalTransfer = summary.getOrDefault("transfer", 0);
        int totalAdjustment = summary.getOrDefault("adjustment", 0);
        int netMovement = totalIn - totalOut;
        
        totalInLabel.setText(String.valueOf(totalIn));
        totalOutLabel.setText(String.valueOf(totalOut));
        totalTransferLabel.setText(String.valueOf(totalTransfer));
        totalAdjustmentLabel.setText(String.valueOf(totalAdjustment));
        netMovementLabel.setText(String.valueOf(netMovement));
        
        // Color code net movement
        if (netMovement > 0) {
            netMovementLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else if (netMovement < 0) {
            netMovementLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else {
            netMovementLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        }
    }
    
    private void loadMovementHistory(int branchId) {
        List<StockMovement> movements = stockMovementDAO.findRecentByBranch(branchId);
        movementItems.clear();
        
        for (StockMovement movement : movements) {
            MovementItem item = new MovementItem(
                    movement.getCreatedAtFormatted(),
                    movement.getMovementType(),
                    movement.getProductName() != null ? movement.getProductName() : "Product " + movement.getProductId(),
                    movement.getBranchName() != null ? movement.getBranchName() : "Branch " + movement.getBranchId(),
                    movement.getQuantity(),
                    getUserName(movement.getCashierId()),
                    movement.getNotes() != null ? movement.getNotes() : ""
            );
            movementItems.add(item);
        }
    }
    
    private void loadDailyTrends(int branchId, int days) {
        Map<String, Map<String, Integer>> dailySummary = resourceTrackingService.getDailyMovementSummary(branchId, days);
        trendItems.clear();
        
        for (Map.Entry<String, Map<String, Integer>> entry : dailySummary.entrySet()) {
            String date = entry.getKey();
            Map<String, Integer> dayData = entry.getValue();
            
            int in = dayData.getOrDefault("IN", 0);
            int out = dayData.getOrDefault("OUT", 0) + dayData.getOrDefault("SALE", 0);
            int net = in - out;
            
            DailyTrendItem item = new DailyTrendItem(date, in, out, net);
            trendItems.add(item);
        }
    }
    
    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().toLowerCase();
        
        if (searchTerm == null || searchTerm.isEmpty()) {
            loadMovementHistory(branchComboBox.getValue().getId());
            return;
        }
        
        ObservableList<MovementItem> filteredItems = FXCollections.observableArrayList();
        
        for (MovementItem item : movementItems) {
            if (item.productProperty().get().toLowerCase().contains(searchTerm) ||
                item.notesProperty().get().toLowerCase().contains(searchTerm)) {
                filteredItems.add(item);
            }
        }
        
        movementTable.setItems(filteredItems);
    }
    
    @FXML
    private void handleExport() {
        showAlert("Export", "Export functionality would be implemented here.\nThis would export the movement history to CSV or Excel.", Alert.AlertType.INFORMATION);
    }
    
    private String getUserName(Integer userId) {
        if (userId == null) return "-";
        // This would use UserDAO to get the user name
        // For now, return a placeholder
        return "User " + userId;
    }
    
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner class for movement table items
    public static class MovementItem {
        private final SimpleStringProperty date;
        private final SimpleStringProperty type;
        private final SimpleStringProperty product;
        private final SimpleStringProperty branch;
        private final SimpleIntegerProperty quantity;
        private final SimpleStringProperty user;
        private final SimpleStringProperty notes;
        
        public MovementItem(String date, String type, String product, String branch, 
                          int quantity, String user, String notes) {
            this.date = new SimpleStringProperty(date);
            this.type = new SimpleStringProperty(type);
            this.product = new SimpleStringProperty(product);
            this.branch = new SimpleStringProperty(branch);
            this.quantity = new SimpleIntegerProperty(quantity);
            this.user = new SimpleStringProperty(user);
            this.notes = new SimpleStringProperty(notes);
        }
        
        public SimpleStringProperty dateProperty() { return date; }
        public SimpleStringProperty typeProperty() { return type; }
        public SimpleStringProperty productProperty() { return product; }
        public SimpleStringProperty branchProperty() { return branch; }
        public SimpleIntegerProperty quantityProperty() { return quantity; }
        public SimpleStringProperty userProperty() { return user; }
        public SimpleStringProperty notesProperty() { return notes; }
    }
    
    // Inner class for daily trend table items
    public static class DailyTrendItem {
        private final SimpleStringProperty date;
        private final SimpleIntegerProperty in;
        private final SimpleIntegerProperty out;
        private final SimpleIntegerProperty net;
        
        public DailyTrendItem(String date, int in, int out, int net) {
            this.date = new SimpleStringProperty(date);
            this.in = new SimpleIntegerProperty(in);
            this.out = new SimpleIntegerProperty(out);
            this.net = new SimpleIntegerProperty(net);
        }
        
        public SimpleStringProperty dateProperty() { return date; }
        public SimpleIntegerProperty inProperty() { return in; }
        public SimpleIntegerProperty outProperty() { return out; }
        public SimpleIntegerProperty netProperty() { return net; }
    }
}
