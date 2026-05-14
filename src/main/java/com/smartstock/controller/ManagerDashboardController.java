package com.smartstock.controller;

import com.smartstock.dao.AlertDAO;
import com.smartstock.dao.StockMovementDAO;
import com.smartstock.model.Branch;
import com.smartstock.model.Product;
import com.smartstock.model.StockMovement;
import com.smartstock.model.User;
import com.smartstock.service.*;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class ManagerDashboardController extends VBox {

    // ── Sidebar state ──────────────────────────────────────────────────────
    private VBox sidebar;
    private boolean sidebarExpanded = true;
    private static final double EXPANDED_W  = 220;
    private static final double COLLAPSED_W = 52;

    private Label sidebarLogoText;
    private Label sidebarSubText;
    private final List<Label>    navSectionLabels = new ArrayList<>();
    private final List<NavEntry> navEntries       = new ArrayList<>();

    record NavEntry(Button btn, String icon, String label) {}

    // ── Content host ───────────────────────────────────────────────────────
    private VBox contentHost;

    // ── Dashboard widgets ──────────────────────────────────────────────────
    private Label welcomeLabel;
    private Label totalProductsLabel, lowStockLabel, totalQtyLabel, unreadAlertsLabel;
    private Button alertBadgeBtn;
    private TableView<Product>      lowStockTable;
    private TableView<StockMovement> movementsTable;

    private final AuthService authService;
    private final Stage stage;
    private final ReportService reportService;
    private final WeeklySummaryService weeklySummaryService;
    private final StockMovementDAO movementDAO;
    private final AlertDAO alertDAO;

    public ManagerDashboardController(AuthService authService, Stage stage) {
        this.authService          = authService;
        this.stage                = stage;
        this.reportService        = new ReportService();
        this.weeklySummaryService = new WeeklySummaryService();
        this.movementDAO          = new StockMovementDAO();
        this.alertDAO             = new AlertDAO();

        com.smartstock.util.ThemeManager.applyTheme(this);
        setSpacing(0);
        VBox.setVgrow(this, Priority.ALWAYS);

        HBox layout = new HBox(0);
        VBox.setVgrow(layout, Priority.ALWAYS);
        layout.getChildren().addAll(buildSidebar(), buildMainArea());
        getChildren().add(layout);

        loadDashboardData();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SIDEBAR
    // ═══════════════════════════════════════════════════════════════════════
    private VBox buildSidebar() {
        sidebar = new VBox(0);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(EXPANDED_W);
        sidebar.setMinWidth(EXPANDED_W);
        sidebar.setMaxWidth(EXPANDED_W);

        VBox logoBox = new VBox(3);
        logoBox.getStyleClass().add("sidebar-header");

        HBox logoRow = new HBox(8);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        Label logoIcon = new Label("📦");
        logoIcon.setStyle("-fx-font-size: 18px;");
        sidebarLogoText = new Label("M5zany");
        sidebarLogoText.getStyleClass().add("sidebar-logo");
        logoRow.getChildren().addAll(logoIcon, sidebarLogoText);

        sidebarSubText = new Label("Branch Manager");
        sidebarSubText.getStyleClass().add("sidebar-sub");
        logoBox.getChildren().addAll(logoRow, sidebarSubText);
        sidebar.getChildren().add(logoBox);

        sidebar.getChildren().add(makeSectionLabel("NAVIGATION"));
        Button dashBtn = addNav(sidebar, "🏠", "Dashboard", true);
        dashBtn.setOnAction(e -> {
            showDashboardHome();
            setActiveNav("Dashboard");
            loadDashboardData();
        });

        Button productsBtn = addNav(sidebar, "📦", "Products", false);
        productsBtn.setOnAction(e -> openPage(new ProductManagementController(authService, stage), "Products"));

        Button usersBtn = addNav(sidebar, "👥", "Users", false);
        usersBtn.setOnAction(e -> openPage(new UserManagementController(authService, stage), "Users"));

        Button myBranchBtn = addNav(sidebar, "🏢", "My Branch", false);
        myBranchBtn.setOnAction(e -> openMyBranchPage());

        Button transferBtn = addNav(sidebar, "🔄", "Stock Transfer", false);
        transferBtn.setOnAction(e -> openPage(new StockTransferController(authService, stage), "Stock Transfer"));

        Button invoicesBtn = addNav(sidebar, "🧾", "Invoices", false);
        invoicesBtn.setOnAction(e -> openPage(new ManagerInvoicesController(authService, stage), "Invoices"));

        Button alertsBtn = addNav(sidebar, "🔔", "Alerts", false);
        alertsBtn.setOnAction(e -> openPage(new AlertManagementController(authService, stage), "Alerts"));

        sidebar.getChildren().add(makeSectionLabel("PHASE 2 FEATURES"));
        
        Button resourceBtn = addNav(sidebar, "📈", "Resource Tracking", false);
        resourceBtn.setOnAction(e -> {
            try {
                openPage(com.smartstock.util.DashboardHelper.loadView("/views/ResourceTrackingView.fxml"), "Resource Tracking");
            } catch (Exception ex) {
                ex.printStackTrace();
                showInfo("Navigation Error", "Could not load Resource Tracking view.");
            }
        });

        sidebar.getChildren().add(makeSectionLabel("ANALYTICS"));

        Button pricingBtn = addNav(sidebar, "🤖", "AI Pricing", false);
        pricingBtn.setOnAction(e -> openPage(new DynamicPricingController(authService, stage), "AI Pricing"));

        Button analysisBtn = addNav(sidebar, "📊", "Product Analysis", false);
        analysisBtn.setOnAction(e -> openPage(new ProductAnalysisController(authService, stage), "Product Analysis"));

        Button chartBtn = addNav(sidebar, "📈", "Live Chart", false);
        chartBtn.setOnAction(e -> openPage(new InventoryChartController(authService, stage), "Live Chart"));

        sidebar.getChildren().add(makeSectionLabel("ACTIONS"));

        Button sendAlertBtn = addNav(sidebar, "📢", "Send Alert", false);
        sendAlertBtn.setOnAction(e -> showSendAlertDialog());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);
        sidebar.getChildren().add(makeDivider());

        Button logoutBtn = new Button("⏻  Logout");
        logoutBtn.getStyleClass().add("nav-btn-danger");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> logout());
        sidebar.getChildren().add(logoutBtn);

        return sidebar;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN AREA
    // ═══════════════════════════════════════════════════════════════════════
    private VBox buildMainArea() {
        VBox main = new VBox(0);
        HBox.setHgrow(main, Priority.ALWAYS);
        main.getStyleClass().add("root");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");

        Button toggleBtn = new Button("☰");
        toggleBtn.getStyleClass().add("toggle-sidebar-btn");
        toggleBtn.setOnAction(e -> toggleSidebar());

        welcomeLabel = new Label("Welcome");
        welcomeLabel.getStyleClass().add("header-title");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        alertBadgeBtn = new Button("● Alerts: 0");
        alertBadgeBtn.getStyleClass().add("alert-badge");
        alertBadgeBtn.setOnAction(e -> openPage(new AlertManagementController(authService, stage), "Alerts"));

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().addAll("button", "btn-secondary");
        refreshBtn.setOnAction(e -> { showDashboardHome(); loadDashboardData(); });

        Button themeBtn = new Button("🌓 Theme");
        themeBtn.getStyleClass().add("theme-toggle-btn");
        themeBtn.setOnAction(e -> com.smartstock.util.ThemeManager.toggleTheme(this.getScene()));

        header.getChildren().addAll(toggleBtn, welcomeLabel, hSpacer, alertBadgeBtn, themeBtn, refreshBtn);
        main.getChildren().add(header);

        contentHost = buildHomeContent();
        VBox.setVgrow(contentHost, Priority.ALWAYS);
        main.getChildren().add(contentHost);

        return main;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HOME CONTENT
    // ═══════════════════════════════════════════════════════════════════════
    private VBox buildHomeContent() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        buildStatsCards(content);
        buildLowStockTable(content);
        buildMovementsTable(content);
        scroll.setContent(content);

        VBox host = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox.setVgrow(host, Priority.ALWAYS);
        return host;
    }

    private void buildStatsCards(VBox parent) {
        Label lbl = new Label("OVERVIEW");
        lbl.getStyleClass().add("section-title");

        HBox statsBox = new HBox(14);
        totalProductsLabel = new Label("0");
        lowStockLabel      = new Label("0");
        totalQtyLabel      = new Label("0");
        unreadAlertsLabel  = new Label("0");

        statsBox.getChildren().addAll(
            statCard("📦  Products",    totalProductsLabel, "#6366F1", "#EEF2FF", "#C7D2FE"),
            statCard("⚠  Low Stock",   lowStockLabel,      "#F59E0B", "#FFFBEB", "#FDE68A"),
            statCard("🏷  Total Units", totalQtyLabel,      "#0D9488", "#F0FDF9", "#A7F3D0"),
            statCard("🔔  My Alerts",   unreadAlertsLabel,  "#EF4444", "#FEF2F2", "#FECACA")
        );
        parent.getChildren().addAll(lbl, statsBox);
    }

    private VBox statCard(String title, Label valueLabel, String accent, String bg, String border) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-radius: 10; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        valueLabel.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 32px; -fx-font-weight: bold;");
        card.getChildren().addAll(titleLbl, valueLabel);
        return card;
    }

    private void buildLowStockTable(VBox parent) {
        Label lbl = new Label("⚠  Low Stock Products");
        lbl.getStyleClass().add("section-title");

        lowStockTable = new TableView<>();
        lowStockTable.setPrefHeight(190);
        lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        lowStockTable.setPlaceholder(new Label("✅ All products are well stocked"));

        lowStockTable.getColumns().addAll(
            col("Product",      "name"),
            col("Category",     "category"),
            col("Qty",          "quantity"),
            col("Min Required", "minStock")
        );

        lowStockTable.setRowFactory(tv -> new TableRow<Product>() {
            @Override protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null && item.getQuantity() == 0)
                    setStyle("-fx-background-color: #3C1F24;");
                else if (!empty && item != null && item.getQuantity() < item.getMinStock())
                    setStyle("-fx-background-color: #3A3118;");
                else setStyle("");
            }
        });

        VBox card = new VBox(10, lbl, lowStockTable);
        card.getStyleClass().add("card");
        parent.getChildren().add(card);
    }

    private void buildMovementsTable(VBox parent) {
        Label lbl = new Label("📋  Recent Stock Movements");
        lbl.getStyleClass().add("section-title");

        movementsTable = new TableView<>();
        movementsTable.setPrefHeight(200);
        movementsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        movementsTable.setPlaceholder(new Label("No recent movements"));
        movementsTable.getColumns().addAll(
            col("Product", "productName"),
            col("Type",    "movementType"),
            col("Qty",     "quantity"),
            col("Notes",   "notes"),
            col("Date",    "createdAtFormatted")
        );

        VBox card = new VBox(10, lbl, movementsTable);
        card.getStyleClass().add("card");
        parent.getChildren().add(card);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════
    public void openPage(VBox pageCtrl, String title) {
        VBox wrapper = new VBox(0);
        wrapper.getStyleClass().add("root");
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        VBox.setVgrow(pageCtrl, Priority.ALWAYS);
        wrapper.getChildren().add(pageCtrl);
        swapContent(wrapper);
        welcomeLabel.setText(title);
        setActiveNav(title);
    }

    private void showDashboardHome() {
        VBox home = buildHomeContent();
        VBox.setVgrow(home, Priority.ALWAYS);
        swapContent(home);
        setActiveNav("Dashboard");
    }

    private void swapContent(VBox newContent) {
        VBox main = (VBox) contentHost.getParent();
        main.getChildren().remove(contentHost);
        contentHost = newContent;
        VBox.setVgrow(contentHost, Priority.ALWAYS);
        main.getChildren().add(contentHost);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SIDEBAR TOGGLE
    // ═══════════════════════════════════════════════════════════════════════
    private void toggleSidebar() {
        sidebarExpanded = !sidebarExpanded;
        double target = sidebarExpanded ? EXPANDED_W : COLLAPSED_W;

        Timeline tl = new Timeline(new KeyFrame(Duration.millis(220),
            new KeyValue(sidebar.prefWidthProperty(), target, Interpolator.EASE_BOTH),
            new KeyValue(sidebar.minWidthProperty(),  target, Interpolator.EASE_BOTH),
            new KeyValue(sidebar.maxWidthProperty(),  target, Interpolator.EASE_BOTH)
        ));
        tl.play();

        sidebarLogoText.setVisible(sidebarExpanded);
        sidebarLogoText.setManaged(sidebarExpanded);
        sidebarSubText.setVisible(sidebarExpanded);
        sidebarSubText.setManaged(sidebarExpanded);

        navSectionLabels.forEach(l -> { l.setVisible(sidebarExpanded); l.setManaged(sidebarExpanded); });

        for (NavEntry e : navEntries) {
            e.btn().setText(sidebarExpanded ? e.icon() + "  " + e.label() : e.icon());
            e.btn().setTooltip(sidebarExpanded ? null : new Tooltip(e.label()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DATA
    // ═══════════════════════════════════════════════════════════════════════
    public void loadDashboardData() {
        User user = authService.getCurrentUser();
        if (user == null) return;
        welcomeLabel.setText("Welcome, " + user.getFullName() + "  ·  Branch Manager");
        Integer branchId = user.getBranchId();
        if (branchId == null) return;
        try {
            reportService.generateOperationalAlerts();
            List<Product> lowStock = reportService.getLowStockProducts(branchId);
            if (lowStockTable != null) lowStockTable.setItems(FXCollections.observableArrayList(lowStock));
            if (lowStockLabel != null) lowStockLabel.setText(String.valueOf(lowStock.size()));

            if (movementsTable != null)
                movementsTable.setItems(FXCollections.observableArrayList(movementDAO.findRecentByBranch(branchId)));

            BranchService bs = new BranchService();
            com.smartstock.model.Branch branch = bs.getBranchById(branchId);
            if (branch != null) {
                if (totalProductsLabel != null) totalProductsLabel.setText(String.valueOf(branch.getProductCount()));
                if (totalQtyLabel      != null) totalQtyLabel.setText(String.valueOf(branch.getTotalQuantity()));
            }

            long unread = alertDAO.findForBranch(branchId).stream().filter(a -> !a.isRead()).count();
            if (unreadAlertsLabel != null) unreadAlertsLabel.setText(String.valueOf(unread));
            alertBadgeBtn.setText("● Alerts: " + unread);
            alertBadgeBtn.getStyleClass().setAll(unread > 0 ? "alert-badge" : "alert-badge-ok");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void generateSummary(Button btn) {
        btn.setDisable(true);
        btn.setText(sidebarExpanded ? "⏳  Generating..." : "⏳");
        new Thread(() -> {
            String result = weeklySummaryService.generateAISummary();
            Platform.runLater(() -> {
                btn.setDisable(false);
                btn.setText(sidebarExpanded ? "🤖  AI Summary" : "🤖");
                Alert dlg = new Alert(Alert.AlertType.INFORMATION);
                dlg.setTitle("Weekly AI Summary"); dlg.setHeaderText(null);
                TextArea ta = new TextArea(result); ta.setEditable(false); ta.setWrapText(true); ta.setPrefSize(520, 320);
                dlg.getDialogPane().setContent(ta); dlg.showAndWait();
            });
        }).start();
    }

    private void showSendAlertDialog() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Send Alert to Admin"); dialog.setHeaderText("Report an issue");
        TextField subjectField = new TextField(); subjectField.setPromptText("Subject");
        TextArea detailsArea = new TextArea(); detailsArea.setPromptText("Describe the issue..."); detailsArea.setPrefHeight(100);
        ComboBox<String> sevCombo = new ComboBox<>();
        sevCombo.getItems().addAll("INFO", "WARNING", "CRITICAL"); sevCombo.setValue("WARNING");
        VBox content = new VBox(8, new Label("Subject:"), subjectField, new Label("Severity:"), sevCombo, new Label("Details:"), detailsArea);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(b -> b == ButtonType.OK ? new String[]{subjectField.getText(), detailsArea.getText(), sevCombo.getValue()} : null);
        dialog.showAndWait().ifPresent(result -> {
            if (result[0].trim().isEmpty()) { showInfo("Validation", "Subject cannot be empty."); return; }
            try {
                User user = authService.getCurrentUser();
                String msg = "From: " + user.getFullName() + " | Branch: " + user.getBranchId()
                        + "\nSubject: " + result[0].trim() + "\nDetails: " + result[1].trim();
                boolean saved = alertDAO.insertCustomAlert(user.getId(), null, msg, result[2]);
                showInfo(saved ? "Sent" : "Failed", saved ? "Alert sent to admin." : "Could not save alert.");
                loadDashboardData();
            } catch (Exception ex) { ex.printStackTrace(); showInfo("Error", ex.getMessage()); }
        });
    }

    private void logout() {
        authService.logout();
        try {
            LoginController ctrl = new LoginController(authService, stage);
            Scene scene = new Scene(ctrl, 480, 620);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            stage.setScene(scene); stage.setTitle("SmartStock ERP — Login"); stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    private Button addNav(VBox parent, String icon, String label, boolean active) {
        Button btn = new Button(icon + "  " + label);
        btn.getStyleClass().add(active ? "nav-btn-active" : "nav-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        navEntries.add(new NavEntry(btn, icon, label));
        parent.getChildren().add(btn);
        return btn;
    }

    private void setActiveNav(String targetLabel) {
        String match = targetLabel;
        if ("Low Stock".equalsIgnoreCase(match)) match = "Products";
        if (match != null && match.startsWith("Product:")) match = "Products";
        if (match != null && match.startsWith("Branch:")) match = "My Branch";

        for (NavEntry e : navEntries) {
            e.btn().getStyleClass().removeAll("nav-btn-active", "nav-btn");
            if (e.label().equalsIgnoreCase(match)) {
                e.btn().getStyleClass().add("nav-btn-active");
            } else {
                e.btn().getStyleClass().add("nav-btn");
            }
        }
    }

    private Label makeSectionLabel(String text) {
        Label lbl = new Label(text); lbl.getStyleClass().add("nav-section"); navSectionLabels.add(lbl); return lbl;
    }

    private Region makeDivider() {
        Region r = new Region(); r.setPrefHeight(1); r.setStyle("-fx-background-color: #E2E8F0;"); return r;
    }

    @SuppressWarnings("unchecked")
    private <S, T> TableColumn<S, T> col(String name, String prop) {
        TableColumn<S, T> c = new TableColumn<>(name);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        return c;
    }

    private void showInfo(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    private void openMyBranchPage() {
        User user = authService.getCurrentUser();
        if (user == null || user.getBranchId() == null) {
            showInfo("Branch", "No branch assigned to this manager.");
            return;
        }
        BranchService branchService = new BranchService();
        Branch branch = branchService.getBranchById(user.getBranchId());
        if (branch == null) {
            showInfo("Branch", "Branch details could not be loaded.");
            return;
        }
        openPage(new BranchDetailController(branch, authService, stage), "Branch: " + branch.getName());
    }
}
