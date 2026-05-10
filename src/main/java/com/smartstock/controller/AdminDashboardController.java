package com.smartstock.controller;

import com.smartstock.dao.AlertDAO;
import com.smartstock.model.Branch;
import com.smartstock.model.Product;
import com.smartstock.model.User;
import com.smartstock.service.*;
import com.smartstock.util.CSVExporter;
import com.smartstock.util.NavigationHelper;
import com.smartstock.util.PDFExporter;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController extends VBox {

    // ── Sidebar state ──────────────────────────────────────────────────────
    private VBox sidebar;
    private boolean sidebarExpanded = true;
    private static final double EXPANDED_W  = 220;
    private static final double COLLAPSED_W = 52;

    // Labels hidden when sidebar is collapsed
    private Label sidebarLogoText;
    private Label sidebarSubText;
    private final List<Label> navSectionLabels = new ArrayList<>();
    private final List<NavEntry> navEntries    = new ArrayList<>();

    record NavEntry(MFXButton btn, String icon, String label) {}

    // ── Content area ───────────────────────────────────────────────────────
    private VBox contentHost;  // swapped when navigating

    // ── Dashboard widgets ──────────────────────────────────────────────────
    private Label welcomeLabel;
    private Label branchCountLabel, totalProductsLabel, lowStockAlertsLabel, unreadAlertsLabel;
    private MFXButton alertBadgeBtn;
    private FlowPane branchCardsBox;
    private FilteredList<Branch> filteredBranches;
    private ObservableList<Branch> allBranchData = FXCollections.observableArrayList();
    private TextArea summaryArea;

    private final AuthService authService;
    private final Stage stage;
    private final ReportService reportService;
    private final BranchService branchService;
    private final WeeklySummaryService weeklySummaryService;
    private final AlertDAO alertDAO;

    public AdminDashboardController(AuthService authService, Stage stage) {
        this.authService         = authService;
        this.stage               = stage;
        this.reportService       = new ReportService();
        this.branchService       = new BranchService();
        this.weeklySummaryService = new WeeklySummaryService();
        this.alertDAO            = new AlertDAO();

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

        // Logo header
        VBox logoBox = new VBox(6);
        logoBox.getStyleClass().add("sidebar-header");

        HBox logoRow = new HBox(12);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        
        StackPane logoIconBg = new StackPane();
        logoIconBg.setStyle("-fx-background-color: #6366F1; -fx-background-radius: 8;");
        logoIconBg.setPrefSize(32, 32);
        FontIcon cubeIcon = new FontIcon("mdi2c-cube-outline");
        cubeIcon.setIconSize(20);
        cubeIcon.setIconColor(javafx.scene.paint.Color.WHITE);
        logoIconBg.getChildren().add(cubeIcon);

        VBox textCol = new VBox(0);
        sidebarLogoText = new Label("M5zany");
        sidebarLogoText.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
        sidebarSubText = new Label("ERP PLATFORM");
        sidebarSubText.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: -text-secondary; -fx-letter-spacing: 1px;");
        textCol.getChildren().addAll(sidebarLogoText, sidebarSubText);

        logoRow.getChildren().addAll(logoIconBg, textCol);
        logoBox.getChildren().addAll(logoRow);
        sidebar.getChildren().add(logoBox);

        // Navigation section
        Label navLbl = makeSectionLabel("COMPONENT TREE");
        sidebar.getChildren().add(navLbl);

        MFXButton dashBtn = addNav(sidebar, "mdi2v-view-dashboard", "Dashboard", true);
        dashBtn.setOnAction(e -> {
            showDashboardHome();
            setActiveNav("Dashboard");
            loadDashboardData();
        });

        User user = authService.getCurrentUser();
        boolean isAdmin = user != null && user.isAdmin();

        if (isAdmin) {
            MFXButton branchBtn = addNav(sidebar, "mdi2d-domain", "Branches", false);
            branchBtn.setOnAction(e -> openPage(new BranchManagementController(authService, stage), "Branches"));

            MFXButton usersBtn = addNav(sidebar, "mdi2a-account-group", "Users", false);
            usersBtn.setOnAction(e -> openPage(new UserManagementController(authService, stage), "Users"));
        }

        MFXButton productsBtn = addNav(sidebar, "mdi2p-package-variant-closed", "Products", false);
        productsBtn.setOnAction(e -> openPage(new ProductManagementController(authService, stage), "Products"));

        MFXButton transferBtn = addNav(sidebar, "mdi2s-swap-horizontal", "Stock Transfer", false);
        transferBtn.setOnAction(e -> openPage(new StockTransferController(authService, stage), "Stock Transfer"));

        MFXButton alertsBtn = addNav(sidebar, "mdi2b-bell", "Alerts", false);
        alertsBtn.setOnAction(e -> openPage(new AlertManagementController(authService, stage), "Alerts"));

        if (user != null && (user.isAdmin() || user.isManager())) {
            Label repLbl = makeSectionLabel("REPORTS");
            sidebar.getChildren().add(repLbl);

            MFXButton sumBtn = addNav(sidebar, "mdi2r-robot", "AI Summary", false);
            sumBtn.setOnAction(e -> generateSummary(sumBtn));

            MFXButton pdfBtn = addNav(sidebar, "mdi2f-file-pdf-box", "Export PDF", false);
            pdfBtn.setOnAction(e -> exportPDF());

            MFXButton csvBtn = addNav(sidebar, "mdi2f-file-delimited", "Export CSV", false);
            csvBtn.setOnAction(e -> exportCSV());
        }

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        sidebar.getChildren().add(makeDivider());

        MFXButton logoutBtn = new MFXButton("LOGOUT");
        logoutBtn.setGraphic(new FontIcon("mdi2l-logout"));
        logoutBtn.getStyleClass().add("nav-btn-danger");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> logout());
        sidebar.getChildren().add(logoutBtn);

        return sidebar;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN AREA (header + content host)
    // ═══════════════════════════════════════════════════════════════════════
    private VBox buildMainArea() {
        VBox main = new VBox(0);
        HBox.setHgrow(main, Priority.ALWAYS);
        main.getStyleClass().add("root");

        // Header
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 20, 12, 20));
        header.setStyle("-fx-background-color: -sidebar-bg; -fx-border-color: transparent transparent -sidebar-border transparent; -fx-border-width: 0 0 1 0;");

        welcomeLabel = new Label("System Admin");
        welcomeLabel.setStyle("-fx-text-fill: -text-primary; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label liveBadge = new Label("LIVE");
        liveBadge.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10B981; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        TextField globalSearchField = new TextField();
        globalSearchField.setPromptText("\uD83D\uDD0D Global Search (Ctrl+K)");
        globalSearchField.setStyle("-fx-background-color: -input-bg; -fx-border-color: -input-border; -fx-border-radius: 6; -fx-text-fill: -text-primary; -fx-padding: 6 12;");
        globalSearchField.setPrefWidth(250);

        ContextMenu searchResultsMenu = new ContextMenu();
        searchResultsMenu.setStyle("-fx-background-color: -card-bg; -fx-border-color: -card-border;");
        
        globalSearchField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                searchResultsMenu.hide();
                return;
            }
            performGlobalSearch(val.trim(), searchResultsMenu, globalSearchField);
        });

        Platform.runLater(() -> {
            Scene scene = globalSearchField.getScene();
            if (scene != null) {
                scene.getAccelerators().put(
                    new javafx.scene.input.KeyCharacterCombination("k", javafx.scene.input.KeyCombination.SHORTCUT_DOWN),
                    globalSearchField::requestFocus
                );
            }
        });

        alertBadgeBtn = new MFXButton("13 ALERTS");
        alertBadgeBtn.setGraphic(new FontIcon("mdi2c-circle"));
        ((FontIcon)alertBadgeBtn.getGraphic()).setIconColor(javafx.scene.paint.Color.web("#EF4444"));
        ((FontIcon)alertBadgeBtn.getGraphic()).setIconSize(10);
        alertBadgeBtn.getStyleClass().add("alert-badge");
        alertBadgeBtn.setOnAction(e -> openPage(new AlertManagementController(authService, stage), "Alerts"));

        header.getChildren().addAll(welcomeLabel, liveBadge, hSpacer, globalSearchField, alertBadgeBtn);
        main.getChildren().add(header);

        // Content host (swapped per page)
        contentHost = buildHomeContent();
        VBox.setVgrow(contentHost, Priority.ALWAYS);
        main.getChildren().add(contentHost);

        return main;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HOME DASHBOARD CONTENT
    // ═══════════════════════════════════════════════════════════════════════
    private VBox buildHomeContent() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(24);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: -bg-color;");
        
        buildStatsCards(content);
        
        HBox lowerSection = new HBox(20);
        
        VBox feedSection = new VBox(14);
        HBox.setHgrow(feedSection, Priority.ALWAYS); // Let AI summary fill the width
        buildSummaryArea(feedSection);
        
        lowerSection.getChildren().addAll(feedSection);
        content.getChildren().add(lowerSection);
        
        scroll.setContent(content);

        VBox host = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox.setVgrow(host, Priority.ALWAYS);
        return host;
    }

    private void buildStatsCards(VBox parent) {
        HBox headerRow = new HBox();
        Label lbl = new Label("# PLATFORM HEALTH");
        lbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text-secondary; -fx-letter-spacing: 1.5px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(lbl, spacer);

        HBox statsBox = new HBox(16);
        branchCountLabel    = new Label("0");
        totalProductsLabel  = new Label("0");
        lowStockAlertsLabel = new Label("0");
        unreadAlertsLabel   = new Label("0");

        VBox branchCard = statCard("BRANCHES",      branchCountLabel,    "mdi2m-map-marker-outline", "#3B82F6");
        branchCard.setOnMouseClicked(e -> openPage(new BranchManagementController(authService, stage), "Branches"));
        branchCard.setStyle(branchCard.getStyle() + "-fx-cursor: hand;");

        VBox productsCard = statCard("PRODUCTS",    totalProductsLabel,  "mdi2c-cube-outline", "#10B981");
        productsCard.setOnMouseClicked(e -> openPage(new ProductManagementController(authService, stage), "Products"));
        productsCard.setStyle(productsCard.getStyle() + "-fx-cursor: hand;");

        VBox lowStockCard = statCard("LOW STOCK",   lowStockAlertsLabel, "mdi2a-alert-outline", "#F59E0B");
        lowStockCard.setOnMouseClicked(e -> openPage(new ProductManagementController(authService, stage), "Low Stock"));
        lowStockCard.setStyle(lowStockCard.getStyle() + "-fx-cursor: hand;");

        VBox alertsCard = statCard("UNREAD ALERTS", unreadAlertsLabel,   "mdi2i-inbox-outline", "#EC4899");
        alertsCard.setOnMouseClicked(e -> openPage(new AlertManagementController(authService, stage), "Alerts"));
        alertsCard.setStyle(alertsCard.getStyle() + "-fx-cursor: hand;");

        statsBox.getChildren().addAll(branchCard, productsCard, lowStockCard, alertsCard);
        parent.getChildren().addAll(headerRow, statsBox);
    }

    private VBox statCard(String title, Label valueLabel, String iconCode, String bottomBarColor) {
        VBox card = new VBox(0);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS);
        
        HBox topRow = new HBox();
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconColor(javafx.scene.paint.Color.web(bottomBarColor));
        icon.setIconSize(16);
        topRow.getChildren().addAll(titleLbl, spacer, icon);
        
        valueLabel.setStyle("-fx-text-fill: -text-primary; -fx-font-size: 32px; -fx-font-weight: bold;");
        VBox.setMargin(valueLabel, new Insets(12, 0, 16, 0));
        
        Region bottomBar = new Region();
        bottomBar.setPrefHeight(4);
        bottomBar.setStyle("-fx-background-color: " + bottomBarColor + "; -fx-background-radius: 2;");
        bottomBar.setMaxWidth(80);
        
        Label clickHint = new Label("Click to view →");
        clickHint.setStyle("-fx-text-fill: " + bottomBarColor + "; -fx-font-size: 10px; -fx-opacity: 0.7;");
        VBox.setMargin(clickHint, new Insets(8, 0, 0, 0));

        card.getChildren().addAll(topRow, valueLabel, bottomBar, clickHint);
        return card;
    }



    private void buildSummaryArea(VBox parent) {
        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);
        FontIcon icn = new FontIcon("mdi2c-crosshairs-gps");
        icn.setIconColor(javafx.scene.paint.Color.web("#6366F1"));
        Label lbl = new Label("Weekly Sammary ");
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -text-primary; -fx-letter-spacing: 1px;");
        topRow.getChildren().addAll(icn, lbl);

        summaryArea = new TextArea();
        summaryArea.setPrefHeight(260);
        summaryArea.setEditable(false);
        summaryArea.setWrapText(true);
        summaryArea.setPromptText("AI insights will appear here...");
        summaryArea.setStyle("-fx-control-inner-background: -card-bg; -fx-text-fill: -text-primary; -fx-border-width: 0;");

        VBox card = new VBox(14, topRow, summaryArea);
        card.setStyle("-fx-background-color: -card-bg; -fx-background-radius: 12; -fx-padding: 20; -fx-border-color: -card-border; -fx-border-radius: 12; -fx-border-width: 1;");
        parent.getChildren().add(card);

        // Auto-load last saved summary in background
        new Thread(() -> {
            String last = weeklySummaryService.getLastSummaryText();
            Platform.runLater(() -> {
                if (last != null && !last.isBlank() && summaryArea != null) {
                    summaryArea.setText(last);
                }
            });
        }).start();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // NAVIGATION — swap content in-place
    // ═══════════════════════════════════════════════════════════════════════
    public void openPage(VBox pageCtrl, String title) {
        // Wrap page in a VBox with a back-to-dashboard button at top
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
    }



    private void swapContent(VBox newContent) {
        VBox main = (VBox) contentHost.getParent();
        main.getChildren().remove(contentHost);
        contentHost = newContent;
        VBox.setVgrow(contentHost, Priority.ALWAYS);
        
        contentHost.setOpacity(0);
        contentHost.setTranslateY(15);
        main.getChildren().add(contentHost);
        
        FadeTransition ft = new FadeTransition(Duration.millis(250), contentHost);
        ft.setToValue(1.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(250), contentHost);
        tt.setToY(0);
        
        ft.play();
        tt.play();
    }

    private void performGlobalSearch(String query, ContextMenu menu, TextField field) {
        menu.getItems().clear();
        String q = query.toLowerCase();

        branchService.getAllBranches().stream()
            .filter(b -> (b.getName() != null && b.getName().toLowerCase().contains(q)) || 
                         (b.getLocation() != null && b.getLocation().toLowerCase().contains(q)))
            .limit(3)
            .forEach(b -> {
                MenuItem item = new MenuItem("🏢 Branch: " + b.getName() + " (" + b.getLocation() + ")");
                item.setOnAction(e -> openPage(new BranchDetailController(b, authService, stage), b.getName()));
                menu.getItems().add(item);
            });

        new com.smartstock.dao.ProductDAO().findAll().stream()
            .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q)) || 
                         (p.getBarcode() != null && p.getBarcode().toLowerCase().contains(q)))
            .limit(5)
            .forEach(p -> {
                MenuItem item = new MenuItem("📦 Product: " + p.getName() + " - $" + String.format("%.2f", p.getSellingPrice()));
                item.setOnAction(e -> openPage(new ProductDetailController(p, authService, stage), "Product: " + p.getName()));
                menu.getItems().add(item);
            });

        if (menu.getItems().isEmpty()) {
            MenuItem empty = new MenuItem("No results found");
            empty.setDisable(true);
            menu.getItems().add(empty);
        }

        if (!menu.isShowing()) {
            menu.show(field, javafx.geometry.Side.BOTTOM, 0, 5);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SIDEBAR TOGGLE ANIMATION
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

        // Toggle text visibility
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
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getFullName() + "  ·  " + user.getRole());
        }
        try {
            reportService.generateOperationalAlerts();
            List<Branch> branches;
            int unread;
            if (user != null && user.isAdmin()) {
                branches = reportService.getAllBranchesWithSummary();
                unread   = reportService.getUnreadAlerts().size();
            } else if (user != null && user.getBranchId() != null) {
                Branch single = branchService.getBranchById(user.getBranchId());
                branches = single != null ? List.of(single) : List.of();
                unread = (int) reportService.getUnreadAlerts().stream()
                        .filter(a -> a.getBranchId() == null || a.getBranchId() == user.getBranchId()).count();
            } else { branches = List.of(); unread = 0; }

            // Update filtered observable list
            if (allBranchData != null) {
                allBranchData.setAll(branches);
            }
            if (branchCountLabel    != null) branchCountLabel.setText(String.valueOf(branches.size()));
            if (totalProductsLabel  != null) totalProductsLabel.setText(String.valueOf(branches.stream().mapToInt(Branch::getProductCount).sum()));
            if (lowStockAlertsLabel != null) lowStockAlertsLabel.setText(String.valueOf(branches.stream().mapToInt(Branch::getLowStockCount).sum()));
            if (unreadAlertsLabel   != null) unreadAlertsLabel.setText(String.valueOf(unread));

            alertBadgeBtn.setText(unread + " ALERTS");
            alertBadgeBtn.getStyleClass().setAll(unread > 0 ? "alert-badge" : "alert-badge-ok");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void generateSummary(Button btn) {
        btn.setDisable(true); btn.setText(sidebarExpanded ? "⏳  Generating..." : "⏳");
        new Thread(() -> {
            try {
                String summary = weeklySummaryService.generateAISummary();
                Platform.runLater(() -> {
                    if (summaryArea != null) summaryArea.setText(summary);
                    btn.setDisable(false);
                    btn.setText(sidebarExpanded ? "🤖  AI Summary" : "🤖");
                    loadDashboardData();
                });
            } catch (Exception e) {
                Platform.runLater(() -> { btn.setDisable(false); btn.setText(sidebarExpanded ? "🤖  AI Summary" : "🤖"); });
            }
        }).start();
    }

    private void exportPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export PDF"); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("branch_report.pdf");
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try { PDFExporter.exportBranchReport(branchService.getAllBranches(), file.getAbsolutePath()); showAlert("Success", "PDF exported."); }
            catch (Exception e) { showAlert("Error", e.getMessage()); }
        }
    }

    private void exportCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export CSV"); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("branch_report.csv");
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try { CSVExporter.exportBranchReport(branchService.getAllBranches(), file.getAbsolutePath()); showAlert("Success", "CSV exported."); }
            catch (Exception e) { showAlert("Error", e.getMessage()); }
        }
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
    private MFXButton addNav(VBox parent, String iconCode, String label, boolean active) {
        MFXButton btn = new MFXButton(label);
        btn.setGraphic(new FontIcon(iconCode));
        btn.getStyleClass().add(active ? "nav-btn-active" : "nav-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        navEntries.add(new NavEntry(btn, iconCode, label));
        parent.getChildren().add(btn);
        return btn;
    }

    private void setActiveNav(String targetLabel) {
        String match = targetLabel;
        if ("Low Stock".equalsIgnoreCase(match)) match = "Products";

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
        Label lbl = new Label(text);
        lbl.getStyleClass().add("nav-section");
        navSectionLabels.add(lbl);
        return lbl;
    }

    private Region makeDivider() {
        Region r = new Region(); r.setPrefHeight(1); r.setStyle("-fx-background-color: #E2E8F0;"); return r;
    }

    private void showAlert(String t, String m) {
        FontIcon icon = new FontIcon("mdi2i-information-outline");
        icon.setIconSize(32);
        icon.setIconColor(javafx.scene.paint.Color.web("#6366F1"));

        Notifications.create()
            .title(t)
            .text(m)
            .graphic(icon)
            .position(Pos.CENTER)
            .hideAfter(Duration.seconds(4))
            .show();
    }
}
