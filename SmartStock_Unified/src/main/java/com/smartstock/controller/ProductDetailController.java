package com.smartstock.controller;

import com.smartstock.dao.InventoryDAO;
import com.smartstock.model.Branch;
import com.smartstock.model.Product;
import com.smartstock.service.AuthService;
import com.smartstock.service.BranchService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailController extends VBox {

    private final Product product;
    private final AuthService authService;
    private final Stage stage;
    private final BranchService branchService = new BranchService();
    private final InventoryDAO inventoryDAO = new InventoryDAO();

    // DTO for table
    public static class BranchStock {
        private final Branch branch;
        private final int quantity;
        private final double sellingPrice;

        public BranchStock(Branch branch, int quantity, double sellingPrice) {
            this.branch = branch;
            this.quantity = quantity;
            this.sellingPrice = sellingPrice;
        }

        public String getBranchName() { return branch.getName(); }
        public String getLocation() { return branch.getLocation(); }
        public int getQuantity() { return quantity; }
        public double getSellingPrice() { return sellingPrice; }
        public String getStatus() { return quantity > 0 ? "IN STOCK" : "OUT OF STOCK"; }
    }

    public ProductDetailController(Product product, AuthService authService, Stage stage) {
        this.product = product;
        this.authService = authService;
        this.stage = stage;

        com.smartstock.util.ThemeManager.applyTheme(this);
        setSpacing(0);
        VBox.setVgrow(this, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(24);
        content.setPadding(new Insets(28));
        content.setStyle("-fx-background-color: -bg-color;");

        Button backBtn = new Button("← BACK TO INVENTORY");
        backBtn.setStyle("-fx-background-color: -card-bg; -fx-text-fill: -text-secondary; -fx-font-weight: bold; -fx-padding: 8 16; -fx-background-radius: 8; -fx-border-color: -card-border; -fx-border-radius: 8; -fx-cursor: hand;");
        backBtn.setOnAction(e -> goBackToProducts());
        content.getChildren().add(backBtn);

        buildInfoCards(content);
        buildBranchesSection(content);

        scroll.setContent(content);
        getChildren().add(scroll);
    }

    private void buildInfoCards(VBox parent) {
        // Title row
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBg = new StackPane();
        iconBg.setStyle("-fx-background-color: rgba(99,102,241,0.15); -fx-background-radius: 10;");
        iconBg.setPrefSize(42, 42);
        FontIcon icon = new FontIcon("mdi2p-package-variant");
        icon.setIconSize(22);
        icon.setIconColor(javafx.scene.paint.Color.web("#6366F1"));
        iconBg.getChildren().add(icon);

        VBox titleText = new VBox(2);
        Label name = new Label(product.getName());
        name.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
        Label barcode = new Label("Barcode: " + (product.getBarcode() != null ? product.getBarcode() : "N/A"));
        barcode.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");
        titleText.getChildren().addAll(name, barcode);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // Category badge
        Label catBadge = new Label(product.getCategory() != null ? product.getCategory().toUpperCase() : "GENERAL");
        catBadge.setStyle("-fx-background-color: rgba(139,92,246,0.15); -fx-text-fill: #8B5CF6; -fx-border-color: #8B5CF6; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 4 14; -fx-font-weight: bold; -fx-font-size: 11px;");

        titleRow.getChildren().addAll(iconBg, titleText, sp, catBadge);

        // Info chips row
        HBox chipsRow = new HBox(12);
        chipsRow.setPadding(new Insets(6, 0, 0, 0));
        
        // Calculate total quantity across all branches
        int totalGlobalQuantity = 0;
        List<Branch> branches = branchService.getAllBranches();
        for (Branch b : branches) {
            totalGlobalQuantity += inventoryDAO.getQuantity(product.getId(), b.getId());
        }

        chipsRow.getChildren().addAll(
                infoChip("mdi2c-cash", String.format("Cost: $%.2f", product.getUnitCost()), "#F59E0B"),
                infoChip("mdi2t-tag-outline", String.format("Price: $%.2f", product.getSellingPrice()), "#10B981"),
                infoChip("mdi2w-warehouse", totalGlobalQuantity + " Total Global Qty", "#3B82F6"),
                infoChip("mdi2a-alert-outline", "Min Stock: " + product.getMinStock(), "#EF4444")
        );

        VBox header = new VBox(14, titleRow, chipsRow);
        header.setStyle("-fx-background-color: -card-bg; -fx-background-radius: 14; -fx-padding: 22; -fx-border-color: -card-border; -fx-border-radius: 14; -fx-border-width: 1;");
        parent.getChildren().add(header);
    }

    private HBox infoChip(String iconCode, String text, String color) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-background-radius: 8; -fx-padding: 7 14; -fx-border-color: -card-border; -fx-border-radius: 8; -fx-border-width: 1;");
        FontIcon ic = new FontIcon(iconCode);
        ic.setIconSize(14);
        ic.setIconColor(javafx.scene.paint.Color.web(color));
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 12px; -fx-font-weight: bold;");
        chip.getChildren().addAll(ic, lbl);
        return chip;
    }

    private void buildBranchesSection(VBox parent) {
        // Fetch branch stock data
        List<Branch> branches = branchService.getAllBranches();
        List<BranchStock> stockList = new ArrayList<>();
        for (Branch b : branches) {
            int qty = inventoryDAO.getQuantity(product.getId(), b.getId());
            stockList.add(new BranchStock(b, qty, product.getSellingPrice()));
        }
        
        ObservableList<BranchStock> data = FXCollections.observableArrayList(stockList);
        FilteredList<BranchStock> filtered = new FilteredList<>(data, p -> true);

        // Section header
        HBox sectionHeader = new HBox(10);
        sectionHeader.setAlignment(Pos.CENTER_LEFT);

        FontIcon bIcon = new FontIcon("mdi2s-storefront-outline");
        bIcon.setIconSize(16);
        bIcon.setIconColor(javafx.scene.paint.Color.web("#3B82F6"));
        Label sectionTitle = new Label("STOCK ACROSS BRANCHES");
        sectionTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -text-primary; -fx-letter-spacing: 1px;");
        
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // Filter bar
        HBox filterBar = new HBox(8);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search branch...");
        searchField.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-border-color: -card-border; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: -text-primary; -fx-padding: 4 8;");
        searchField.setPrefWidth(160);

        CheckBox inStockOnly = new CheckBox("In Stock Only");
        inStockOnly.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 11px;");

        Runnable applyFilter = () -> {
            String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            boolean stockOnly = inStockOnly.isSelected();
            filtered.setPredicate(bs -> {
                boolean matchName = bs.getBranchName().toLowerCase().contains(q);
                boolean matchStock = !stockOnly || bs.getQuantity() > 0;
                return matchName && matchStock;
            });
        };
        searchField.textProperty().addListener((obs, old, val) -> applyFilter.run());
        inStockOnly.selectedProperty().addListener((obs, old, val) -> applyFilter.run());

        filterBar.getChildren().addAll(inStockOnly, searchField);
        sectionHeader.getChildren().addAll(bIcon, sectionTitle, sp, filterBar);

        // Table
        TableView<BranchStock> table = new TableView<>(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(300);
        table.getStyleClass().add("custom-table");
        table.setStyle("-fx-background-color: transparent;");

        TableColumn<BranchStock, String> nameCol = new TableColumn<>("BRANCH");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("branchName"));
        nameCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: -text-primary; -fx-padding: 0 0 0 10;");
                }
            }
        });

        TableColumn<BranchStock, String> locCol = new TableColumn<>("LOCATION");
        locCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        locCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item); setStyle("-fx-text-fill: -text-secondary;"); }
            }
        });

        TableColumn<BranchStock, Double> priceCol = new TableColumn<>("SELLING PRICE");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        priceCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(String.format("$%.2f", item));
                    setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<BranchStock, Integer> qtyCol = new TableColumn<>("QUANTITY");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(item.toString());
                    if (item <= product.getMinStock() && item > 0) {
                        setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
                    } else if (item == 0) {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #3B82F6; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<BranchStock, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label badge = new Label(item);
                    if (item.equals("IN STOCK")) {
                        badge.setStyle("-fx-background-color: rgba(16,185,129,0.1); -fx-text-fill: #10B981; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
                    } else {
                        badge.setStyle("-fx-background-color: rgba(239,68,68,0.1); -fx-text-fill: #EF4444; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        table.getColumns().addAll(nameCol, locCol, priceCol, qtyCol, statusCol);

        VBox container = new VBox(12, sectionHeader, table);
        container.setStyle("-fx-background-color: -card-bg; -fx-background-radius: 14; -fx-padding: 22; -fx-border-color: -card-border; -fx-border-radius: 14; -fx-border-width: 1;");
        parent.getChildren().add(container);
    }

    private void goBackToProducts() {
        if (getScene() == null || getScene().getRoot() == null) return;
        if (getScene().getRoot() instanceof AdminDashboardController adminDashboard) {
            adminDashboard.openPage(new ProductManagementController(authService, stage), "Products");
            return;
        }
        if (getScene().getRoot() instanceof ManagerDashboardController managerDashboard) {
            managerDashboard.openPage(new ProductManagementController(authService, stage), "Products");
        }
    }
}
