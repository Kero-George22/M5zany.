package com.smartstock.controller;

import com.smartstock.dao.ProductDAO;
import com.smartstock.dao.UserDAO;
import com.smartstock.model.Branch;
import com.smartstock.model.Product;
import com.smartstock.model.User;
import com.smartstock.service.AuthService;
import com.smartstock.util.PDFExporter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.util.List;
import java.util.Map;
import com.smartstock.dao.TransactionDAO;
import java.time.LocalDate;

public class BranchDetailController extends VBox {

    private final Branch branch;
    private final AuthService authService;
    private final Stage stage;
    private final ProductDAO productDAO = new ProductDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();

    public BranchDetailController(Branch branch, AuthService authService, Stage stage) {
        this.branch = branch;
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

        Button printBtn = new Button("🖨 PRINT REPORT");
        printBtn.setStyle("-fx-background-color: rgba(99,102,241,0.15); -fx-border-color: #6366F1; -fx-text-fill: #818CF8; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 8 16;");
        printBtn.setOnAction(e -> printBranchReport());
        content.getChildren().add(printBtn);

        buildInfoCards(content);
        buildFinancialSection(content);
        buildProductsSection(content);
        buildUsersSection(content);

        scroll.setContent(content);
        getChildren().add(scroll);
    }

    // ── Branch Info Cards ─────────────────────────────────────────────────
    private void buildInfoCards(VBox parent) {
        // Title row
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBg = new StackPane();
        iconBg.setStyle("-fx-background-color: rgba(99,102,241,0.15); -fx-background-radius: 10;");
        iconBg.setPrefSize(42, 42);
        FontIcon icon = new FontIcon("mdi2d-domain");
        icon.setIconSize(22);
        icon.setIconColor(javafx.scene.paint.Color.web("#6366F1"));
        iconBg.getChildren().add(icon);

        VBox titleText = new VBox(2);
        Label name = new Label(branch.getName());
        name.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
        Label loc = new Label("📍 " + branch.getLocation());
        loc.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");
        titleText.getChildren().addAll(name, loc);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // Status badge
        Label statusBadge = new Label(branch.isActive() ? "● ACTIVE" : "● INACTIVE");
        statusBadge.setStyle(branch.isActive()
                ? "-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10B981; -fx-border-color: #10B981; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 4 14; -fx-font-weight: bold; -fx-font-size: 11px;"
                : "-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #EF4444; -fx-border-color: #EF4444; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 4 14; -fx-font-weight: bold; -fx-font-size: 11px;");

        titleRow.getChildren().addAll(iconBg, titleText, sp, statusBadge);

        // Info chips row
        HBox chipsRow = new HBox(12);
        chipsRow.setPadding(new Insets(6, 0, 0, 0));
        chipsRow.getChildren().addAll(
                infoChip("mdi2p-phone-outline", branch.getPhone() != null ? branch.getPhone() : "N/A", "#3B82F6"),
                infoChip("mdi2e-email-outline", branch.getEmail() != null ? branch.getEmail() : "N/A", "#10B981"),
                infoChip("mdi2c-cube-outline", branch.getProductCount() + " Products", "#F59E0B"),
                infoChip("mdi2w-warehouse", branch.getTotalQuantity() + " Total Qty", "#6366F1"),
                infoChip("mdi2a-alert-outline", branch.getLowStockCount() + " Low Stock", "#EF4444")
        );

        VBox header = new VBox(14, titleRow, chipsRow);
        header.setStyle("-fx-background-color: -card-bg; -fx-background-radius: 14; -fx-padding: 22; -fx-border-color: -card-border; -fx-border-radius: 14; -fx-border-width: 1;");
        parent.getChildren().add(header);
    }

    private void buildFinancialSection(VBox parent) {
        Map<String, Double> stats = transactionDAO.getFinancialSummary(branch.getId(), LocalDate.now());
        
        HBox sectionHeader = new HBox(10);
        sectionHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon fIcon = new FontIcon("mdi2c-chart-areaspline");
        fIcon.setIconSize(18);
        fIcon.setIconColor(Color.web("#6366F1"));
        Label title = new Label("DAILY FINANCIAL MONITORING");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -text-primary; -fx-letter-spacing: 1px;");
        sectionHeader.getChildren().addAll(fIcon, title);

        HBox statsRow = new HBox(16);
        statsRow.getChildren().addAll(
            financialCard("DAILY REVENUE", stats.getOrDefault("revenue", 0.0), "mdi2c-cash-multiple", "#10B981"),
            financialCard("DAILY EXPENSES", stats.getOrDefault("expenses", 0.0), "mdi2c-cart-arrow-down", "#EF4444"),
            financialCard("DAILY PROFIT", stats.getOrDefault("profit", 0.0), "mdi2c-trending-up", "#6366F1")
        );

        VBox container = new VBox(15, sectionHeader, statsRow);
        container.setStyle("-fx-background-color: -card-bg; -fx-background-radius: 14; -fx-padding: 22; -fx-border-color: -card-border; -fx-border-radius: 14; -fx-border-width: 1;");
        parent.getChildren().add(container);
    }

    private VBox financialCard(String title, double value, String iconCode, String color) {
        VBox card = new VBox(8);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle("-fx-background-color: -bg-color; -fx-background-radius: 10; -fx-padding: 15; -fx-border-color: -card-border; -fx-border-radius: 10;");
        
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        HBox valRow = new HBox(10);
        valRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(24);
        icon.setIconColor(Color.web(color));
        Label valLbl = new Label(String.format("EGP %.2f", value));
        valLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        valRow.getChildren().addAll(icon, valLbl);
        
        card.getChildren().addAll(titleLbl, valRow);
        return card;
    }

    private HBox infoChip(String iconCode, String text, String color) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-background-radius: 8; -fx-padding: 7 14; -fx-border-color: -card-border; -fx-border-radius: 8; -fx-border-width: 1;");
        FontIcon ic = new FontIcon(iconCode);
        ic.setIconSize(14);
        ic.setIconColor(javafx.scene.paint.Color.web(color));
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 12px;");
        chip.getChildren().addAll(ic, lbl);
        return chip;
    }

    // ── Products Section ──────────────────────────────────────────────────
    private void buildProductsSection(VBox parent) {
        List<Product> products = productDAO.findByBranchId(branch.getId());
        ObservableList<Product> data = FXCollections.observableArrayList(products);
        FilteredList<Product> filtered = new FilteredList<>(data, p -> true);

        // Section header
        HBox sectionHeader = new HBox(10);
        sectionHeader.setAlignment(Pos.CENTER_LEFT);

        FontIcon pIcon = new FontIcon("mdi2p-package-variant-closed");
        pIcon.setIconSize(16);
        pIcon.setIconColor(javafx.scene.paint.Color.web("#10B981"));
        Label sectionTitle = new Label("PRODUCTS IN THIS BRANCH");
        sectionTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -text-primary; -fx-letter-spacing: 1px;");
        Label countBadge = new Label(products.size() + " items");
        countBadge.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10B981; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // Filter bar
        HBox filterBar = new HBox(8);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search products...");
        searchField.setStyle("-fx-background-color: -card-bg; -fx-border-color: -card-border; -fx-border-radius: 6; -fx-text-fill: -text-primary; -fx-padding: 6 12; -fx-pref-width: 200px;");

        ComboBox<String> categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("Category");
        categoryFilter.setStyle("-fx-background-color: -card-bg; -fx-border-color: -card-border; -fx-border-radius: 6;");
        // Populate categories from product data
        java.util.Set<String> categories = new java.util.LinkedHashSet<>();
        categories.add("All");
        products.stream().filter(p -> p.getCategory() != null).map(Product::getCategory).distinct().sorted().forEach(categories::add);
        categoryFilter.setItems(FXCollections.observableArrayList(categories));
        categoryFilter.setValue("All");

        ToggleButton lowStockFilter = new ToggleButton("⚠ Low Stock Only");
        lowStockFilter.setStyle("-fx-background-color: -card-bg; -fx-border-color: -card-border; -fx-border-radius: 6; -fx-text-fill: -text-secondary; -fx-padding: 6 12;");

        ToggleButton expiryFilter = new ToggleButton("⏳ Expiring/Expired");
        expiryFilter.setStyle("-fx-background-color: -card-bg; -fx-border-color: -card-border; -fx-border-radius: 6; -fx-text-fill: -text-secondary; -fx-padding: 6 12;");

        // Wire filters
        Runnable applyFilter = () -> {
            String search = searchField.getText().toLowerCase();
            String cat = categoryFilter.getValue();
            boolean lowOnly = lowStockFilter.isSelected();
            boolean expiringOnly = expiryFilter.isSelected();
            
            filtered.setPredicate(p -> {
                boolean nameMatch = p.getName() != null && p.getName().toLowerCase().contains(search)
                        || p.getBarcode() != null && p.getBarcode().toLowerCase().contains(search);
                boolean catMatch = "All".equals(cat) || cat == null || cat.equals(p.getCategory());
                boolean stockMatch = !lowOnly || p.getQuantity() < p.getMinStock();
                
                boolean isExpiring = false;
                if (p.getExpiryDate() != null) {
                    isExpiring = p.getExpiryDate().isBefore(java.time.LocalDate.now().plusDays(7));
                }
                boolean expiryMatch = !expiringOnly || isExpiring;
                
                return nameMatch && catMatch && stockMatch && expiryMatch;
            });
        };

        searchField.textProperty().addListener((obs, o, n) -> applyFilter.run());
        categoryFilter.valueProperty().addListener((obs, o, n) -> applyFilter.run());
        lowStockFilter.selectedProperty().addListener((obs, o, n) -> {
            lowStockFilter.setStyle(n
                    ? "-fx-background-color: rgba(239,68,68,0.15); -fx-border-color: #EF4444; -fx-border-radius: 6; -fx-text-fill: #EF4444; -fx-padding: 6 12; -fx-font-weight: bold;"
                    : "-fx-background-color: -card-bg; -fx-border-color: -card-border; -fx-border-radius: 6; -fx-text-fill: -text-secondary; -fx-padding: 6 12;");
            applyFilter.run();
        });

        expiryFilter.selectedProperty().addListener((obs, o, n) -> {
            expiryFilter.setStyle(n
                    ? "-fx-background-color: rgba(245,158,11,0.15); -fx-border-color: #F59E0B; -fx-border-radius: 6; -fx-text-fill: #F59E0B; -fx-padding: 6 12; -fx-font-weight: bold;"
                    : "-fx-background-color: -card-bg; -fx-border-color: -card-border; -fx-border-radius: 6; -fx-text-fill: -text-secondary; -fx-padding: 6 12;");
            applyFilter.run();
        });

        filterBar.getChildren().addAll(searchField, categoryFilter, lowStockFilter, expiryFilter);

        sectionHeader.getChildren().addAll(pIcon, sectionTitle, countBadge, sp, filterBar);

        // Products Table
        TableView<Product> table = new TableView<>(filtered);
        table.setPrefHeight(260);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No products found"));

        TableColumn<Product, Integer> idCol = new TableColumn<>("ID");
        idCol.setMaxWidth(55);
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "#" + item);
                setStyle(empty ? "" : "-fx-text-fill: -text-secondary;");
            }
        });

        TableColumn<Product, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle(empty ? "" : "-fx-text-fill: -text-primary; -fx-font-weight: bold;");
            }
        });

        TableColumn<Product, String> catCol = new TableColumn<>("CATEGORY");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item.toUpperCase());
                badge.setStyle("-fx-background-color: rgba(99,102,241,0.15); -fx-text-fill: #818CF8; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
                setGraphic(badge); setText(null);
            }
        });

        TableColumn<Product, Double> priceCol = new TableColumn<>("PRICE");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        priceCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$%.2f", item));
                setStyle(empty ? "" : "-fx-text-fill: #10B981; -fx-font-weight: bold;");
            }
        });

        TableColumn<Product, Integer> qtyCol = new TableColumn<>("QTY");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                Product p = getTableView().getItems().get(getIndex());
                boolean low = p.getQuantity() < p.getMinStock();
                setText(String.valueOf(item));
                setStyle(low ? "-fx-text-fill: #EF4444; -fx-font-weight: bold;" : "-fx-text-fill: -text-primary; -fx-font-weight: bold;");
            }
        });

        TableColumn<Product, Integer> minCol = new TableColumn<>("MIN STOCK");
        minCol.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        minCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item));
                setStyle(empty ? "" : "-fx-text-fill: -text-secondary;");
            }
        });

        TableColumn<Product, Integer> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        statusCol.setCellFactory(c -> new TableCell<Product, Integer>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || getIndex() >= getTableView().getItems().size()) { setGraphic(null); return; }
                Product p = getTableView().getItems().get(getIndex());
                boolean low = p.getQuantity() < p.getMinStock();
                Label badge = new Label(low ? "⚠ LOW" : "✓ OK");
                badge.setStyle(low
                        ? "-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #EF4444; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;"
                        : "-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10B981; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
                setGraphic(badge);
            }
        });

        table.getColumns().addAll(idCol, nameCol, catCol, priceCol, qtyCol, minCol, statusCol);

        VBox card = new VBox(14, sectionHeader, table);
        card.setStyle("-fx-background-color: -card-bg; -fx-background-radius: 14; -fx-padding: 22; -fx-border-color: -card-border; -fx-border-radius: 14; -fx-border-width: 1;");
        parent.getChildren().add(card);
    }

    // ── Users Section ─────────────────────────────────────────────────────
    private void buildUsersSection(VBox parent) {
        List<User> users = userDAO.findByBranchId(branch.getId());

        HBox sectionHeader = new HBox(10);
        sectionHeader.setAlignment(Pos.CENTER_LEFT);

        FontIcon uIcon = new FontIcon("mdi2a-account-group");
        uIcon.setIconSize(16);
        uIcon.setIconColor(javafx.scene.paint.Color.web("#6366F1"));
        Label sectionTitle = new Label("BRANCH USERS");
        sectionTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -text-primary; -fx-letter-spacing: 1px;");
        Label countBadge = new Label(users.size() + " users");
        countBadge.setStyle("-fx-background-color: rgba(99,102,241,0.15); -fx-text-fill: #6366F1; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        sectionHeader.getChildren().addAll(uIcon, sectionTitle, countBadge);

        // User cards (wrapping flow)
        FlowPane usersFlow = new FlowPane(12, 12);

        if (users.isEmpty()) {
            Label empty = new Label("No users assigned to this branch.");
            empty.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 13px; -fx-font-style: italic;");
            usersFlow.getChildren().add(empty);
        } else {
            for (User u : users) {
                usersFlow.getChildren().add(buildUserCard(u));
            }
        }

        VBox card = new VBox(14, sectionHeader, usersFlow);
        card.setStyle("-fx-background-color: -card-bg; -fx-background-radius: 14; -fx-padding: 22; -fx-border-color: -card-border; -fx-border-radius: 14; -fx-border-width: 1;");
        parent.getChildren().add(card);
    }

    private VBox buildUserCard(User user) {
        VBox card = new VBox(8);
        card.setPrefWidth(190);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: -bg-color; -fx-background-radius: 10; -fx-border-color: -card-border; -fx-border-radius: 10; -fx-border-width: 1;");

        // Avatar circle with initials
        StackPane avatar = new StackPane();
        avatar.setPrefSize(44, 44);
        String roleColor = roleColor(user.getRole());
        avatar.setStyle("-fx-background-color: " + roleColor + "33; -fx-background-radius: 22; -fx-border-color: " + roleColor + "; -fx-border-radius: 22; -fx-border-width: 1.5;");
        String initials = getInitials(user.getFullName());
        Label avatarLbl = new Label(initials);
        avatarLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + roleColor + ";");
        avatar.getChildren().add(avatarLbl);

        Label nameLbl = new Label(user.getFullName() != null ? user.getFullName() : user.getUsername());
        nameLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -text-primary; -fx-alignment: center;");
        nameLbl.setWrapText(true);
        nameLbl.setMaxWidth(160);

        Label userLbl = new Label("@" + user.getUsername());
        userLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-secondary;");

        Label roleBadge = new Label(user.getRole() != null ? user.getRole() : "USER");
        roleBadge.setStyle("-fx-background-color: " + roleColor + "22; -fx-text-fill: " + roleColor + "; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;");

        Label statusLbl = new Label(user.isActive() ? "● Active" : "● Inactive");
        statusLbl.setStyle(user.isActive()
                ? "-fx-text-fill: #10B981; -fx-font-size: 11px;"
                : "-fx-text-fill: #6B7280; -fx-font-size: 11px;");

        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            Label emailLbl = new Label(user.getEmail());
            emailLbl.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 10px;");
            emailLbl.setWrapText(true);
            emailLbl.setMaxWidth(160);
            card.getChildren().addAll(avatar, nameLbl, userLbl, roleBadge, statusLbl, emailLbl);
        } else {
            card.getChildren().addAll(avatar, nameLbl, userLbl, roleBadge, statusLbl);
        }

        return card;
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
    }

    private String roleColor(String role) {
        if (role == null) return "#6366F1";
        return switch (role.toUpperCase()) {
            case "ADMIN"   -> "#EF4444";
            case "MANAGER" -> "#F59E0B";
            case "CASHIER" -> "#10B981";
            default        -> "#6366F1";
        };
    }

    private void printBranchReport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Full Branch Report (PDF)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("branch_" + branch.getId() + "_full_report.pdf");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        try {
            List<Product> products = productDAO.findByBranchId(branch.getId());
            List<User> users = userDAO.findByBranchId(branch.getId());
            PDFExporter.exportFullBranchReport(branch, products, users, file.getAbsolutePath());
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Branch report exported successfully.");
            ok.setHeaderText(null);
            ok.showAndWait();
        } catch (Exception ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Failed to export branch report: " + ex.getMessage());
            err.setHeaderText(null);
            err.showAndWait();
        }
    }
}
