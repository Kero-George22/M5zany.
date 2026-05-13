package com.smartstock.controller;

import com.smartstock.dao.InventoryDAO;
import com.smartstock.dao.ProductDAO;
import com.smartstock.dao.StockMovementDAO;
import com.smartstock.dao.TransactionDAO;
import com.smartstock.model.Product;
import com.smartstock.model.StockMovement;
import com.smartstock.model.Transaction;
import com.smartstock.model.TransactionItem;
import com.smartstock.model.User;
import com.smartstock.service.AuthService;
import com.smartstock.util.InvoicePDFExporter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class CashierDashboardController extends VBox {

    public static class CartItem {
        private final Product product;
        private int quantity;
        public CartItem(Product p, int q) { this.product = p; this.quantity = q; }
        public String getName()      { return product.getName(); }
        public double getPrice()     { return product.getSellingPrice(); }
        public int getQuantity()     { return quantity; }
        public void setQuantity(int q){ this.quantity = q; }
        public double getTotal()     { return product.getSellingPrice() * quantity; }
        public Product getProduct()  { return product; }
    }

    private TextField searchField;
    private TableView<Product>  productTable;
    private TableView<CartItem> cartTable;
    private Label totalLabel;
    private Label statusLabel;
    private Label welcomeLabel;
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

    private final AuthService authService;
    private final Stage stage;
    private final ProductDAO productDAO;
    private final InventoryDAO inventoryDAO;
    private final StockMovementDAO movementDAO;
    private final TransactionDAO transactionDAO;
    private List<Product> allProducts = new ArrayList<>();

    // ── Sidebar state ──────────────────────────────────────────────────────
    private VBox sidebar;
    private boolean sidebarExpanded = true;
    private static final double EXPANDED_W  = 220;
    private static final double COLLAPSED_W = 52;
    private final List<NavEntry> navEntries = new ArrayList<>();
    record NavEntry(Button btn, String icon, String label) {}

    // ── Content area ───────────────────────────────────────────────────────
    private VBox contentHost;
    private VBox posHomeContent;
    private final java.util.Map<String, VBox> viewCache = new java.util.HashMap<>();

    public CashierDashboardController(AuthService authService, Stage stage) {
        this.authService  = authService;
        this.stage        = stage;
        this.productDAO   = new ProductDAO();
        this.inventoryDAO = new InventoryDAO();
        this.movementDAO  = new StockMovementDAO();
        this.transactionDAO = new TransactionDAO();

        com.smartstock.util.ThemeManager.applyTheme(this);
        setSpacing(0);
        VBox.setVgrow(this, Priority.ALWAYS);

        HBox layout = new HBox(0);
        VBox.setVgrow(layout, Priority.ALWAYS);
        layout.getChildren().addAll(buildSidebar(), buildMainArea());
        getChildren().add(layout);

        loadProducts();
    }

    private VBox buildSidebar() {
        sidebar = new VBox(0);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(EXPANDED_W);
        sidebar.setMinWidth(EXPANDED_W);
        sidebar.setMaxWidth(EXPANDED_W);

        VBox logoBox = new VBox(3);
        logoBox.getStyleClass().add("sidebar-header");
        Label logoIcon = new Label("📦");
        logoIcon.setStyle("-fx-font-size: 18px;");
        Label logoText = new Label("M5zany");
        logoText.getStyleClass().add("sidebar-logo");
        Label subText = new Label("Cashier POS");
        subText.getStyleClass().add("sidebar-sub");
        logoBox.getChildren().addAll(new HBox(8, logoIcon, logoText), subText);
        sidebar.getChildren().add(logoBox);

        sidebar.getChildren().add(makeSectionLabel("NAVIGATION"));
        
        Button posBtn = addNav(sidebar, "🛒", "POS", true);
        posBtn.setOnAction(e -> {
            showPOSHome();
            setActiveNav("POS");
        });

        Button qrBtn = addNav(sidebar, "📱", "QR Generator", false);
        qrBtn.setOnAction(e -> {
            try {
                openPage(com.smartstock.util.DashboardHelper.loadView("/views/QRCodeView.fxml"), "QR Generator");
            } catch (Exception ex) {
                ex.printStackTrace();
                showStatus("❌ Load Error: " + ex.getMessage(), false);
            }
        });

        Button cycleBtn = addNav(sidebar, "📋", "Cycle Count", false);
        cycleBtn.setOnAction(e -> {
            try {
                openPage(com.smartstock.util.DashboardHelper.loadView("/views/CycleCountingView.fxml"), "Cycle Count");
            } catch (Exception ex) {
                ex.printStackTrace();
                showStatus("❌ Load Error: " + ex.getMessage(), false);
            }
        });

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

    private VBox buildMainArea() {
        VBox main = new VBox(0);
        HBox.setHgrow(main, Priority.ALWAYS);
        main.getStyleClass().add("root");

        buildHeader(main);

        posHomeContent = buildPOSContent();
        VBox.setVgrow(posHomeContent, Priority.ALWAYS);
        contentHost = posHomeContent;
        main.getChildren().add(contentHost);

        return main;
    }

    private void buildHeader(VBox parent) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");

        Button toggleBtn = new Button("☰");
        toggleBtn.getStyleClass().add("toggle-sidebar-btn");
        toggleBtn.setOnAction(e -> toggleSidebar());

        welcomeLabel = new Label("POS Terminal");
        welcomeLabel.getStyleClass().add("header-title");
        User user = authService.getCurrentUser();
        if (user != null) welcomeLabel.setText("POS  ·  " + user.getFullName());

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #10B981;");

        Button themeBtn = new Button("🌓 Theme");
        themeBtn.getStyleClass().add("theme-toggle-btn");
        themeBtn.setOnAction(e -> com.smartstock.util.ThemeManager.toggleTheme(this.getScene()));

        Button logoutBtn = new Button("⏻ Logout");
        logoutBtn.getStyleClass().addAll("button", "btn-danger");
        logoutBtn.setOnAction(e -> logout());

        header.getChildren().addAll(toggleBtn, welcomeLabel, hSpacer, statusLabel, themeBtn, logoutBtn);
        parent.getChildren().add(header);
    }

    private VBox buildPOSContent() {
        HBox mainArea = new HBox(14);
        mainArea.getStyleClass().add("root");
        mainArea.setPadding(new Insets(14));
        VBox.setVgrow(mainArea, Priority.ALWAYS);

        VBox leftPanel = buildProductPanel();
        HBox.setHgrow(leftPanel, Priority.ALWAYS);

        VBox rightPanel = buildCartPanel();
        rightPanel.setPrefWidth(350);
        rightPanel.setMinWidth(300);

        mainArea.getChildren().addAll(leftPanel, rightPanel);
        return new VBox(mainArea);
    }

    // ── Navigation logic ───────────────────────────────────────────────────
    public void openPage(VBox pageCtrl, String title) {
        if (viewCache.containsKey(title)) {
            swapContent(viewCache.get(title));
            welcomeLabel.setText(title);
            setActiveNav(title);
            return;
        }

        VBox wrapper = new VBox(0);
        wrapper.getStyleClass().add("root");
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        VBox.setVgrow(pageCtrl, Priority.ALWAYS);
        wrapper.getChildren().add(pageCtrl);
        
        viewCache.put(title, wrapper);
        swapContent(wrapper);
        welcomeLabel.setText(title);
        setActiveNav(title);
    }

    private void showPOSHome() {
        swapContent(posHomeContent);
        welcomeLabel.setText("POS  ·  " + (authService.getCurrentUser() != null ? authService.getCurrentUser().getFullName() : ""));
    }

    private void swapContent(VBox newContent) {
        VBox main = (VBox) contentHost.getParent();
        main.getChildren().remove(contentHost);
        contentHost = newContent;
        VBox.setVgrow(contentHost, Priority.ALWAYS);
        main.getChildren().add(contentHost);
    }

    private void toggleSidebar() {
        sidebarExpanded = !sidebarExpanded;
        double target = sidebarExpanded ? EXPANDED_W : COLLAPSED_W;
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(220),
            new KeyValue(sidebar.prefWidthProperty(), target, Interpolator.EASE_BOTH)
        ));
        tl.play();
    }

    private Button addNav(VBox parent, String icon, String label, boolean active) {
        Button btn = new Button(icon + "  " + label);
        btn.getStyleClass().add(active ? "nav-btn-active" : "nav-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        navEntries.add(new NavEntry(btn, icon, label));
        parent.getChildren().add(btn);
        return btn;
    }

    private void setActiveNav(String targetLabel) {
        for (NavEntry e : navEntries) {
            e.btn().getStyleClass().removeAll("nav-btn-active", "nav-btn");
            if (e.label().equalsIgnoreCase(targetLabel)) {
                e.btn().getStyleClass().add("nav-btn-active");
            } else {
                e.btn().getStyleClass().add("nav-btn");
            }
        }
    }

    private Label makeSectionLabel(String text) {
        Label lbl = new Label(text); lbl.getStyleClass().add("nav-section"); return lbl;
    }

    private Region makeDivider() {
        Region r = new Region(); r.setPrefHeight(1); r.setStyle("-fx-background-color: #334155;"); return r;
    }

    private VBox buildProductPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("card");

        Label title = new Label("📦  Products");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");

        searchField = new TextField();
        searchField.setPromptText("🔍  Search by name, barcode or category...");
        searchField.textProperty().addListener((obs, o, n) -> filterProducts(n));

        productTable = new TableView<>();
        VBox.setVgrow(productTable, Priority.ALWAYS);
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productTable.setPlaceholder(new Label("No products found"));

        TableColumn<Product, String>  nameCol  = col("Product Name", "name");
        TableColumn<Product, String>  catCol   = col("Category",     "category");     catCol.setMinWidth(100); catCol.setMaxWidth(180);
        TableColumn<Product, Double>  priceCol = col("Price (EGP)",  "sellingPrice"); priceCol.setMinWidth(90);  priceCol.setMaxWidth(140);
        TableColumn<Product, Integer> qtyCol   = col("In Stock",     "quantity");     qtyCol.setMinWidth(80);  qtyCol.setMaxWidth(120);
        productTable.getColumns().addAll(nameCol, catCol, priceCol, qtyCol);

        productTable.setRowFactory(tv -> new TableRow<Product>() {
            @Override protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (!empty && p != null && p.getQuantity() <= 0)
                    setStyle("-fx-background-color: #FEE2E2;");
                else setStyle("");
            }
        });

        Button addToCartBtn = new Button("➕  Add to Cart");
        addToCartBtn.getStyleClass().addAll("button", "btn-success");
        addToCartBtn.setMaxWidth(Double.MAX_VALUE);
        addToCartBtn.setOnAction(e -> addSelectedToCart());

        panel.getChildren().addAll(title, searchField, productTable, addToCartBtn);
        return panel;
    }

    private VBox buildCartPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("card");

        Label title = new Label("🛒  Cart");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");

        cartTable = new TableView<>(cartItems);
        VBox.setVgrow(cartTable, Priority.ALWAYS);
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        cartTable.setPlaceholder(new Label("Cart is empty"));

        TableColumn<CartItem, String>  nameCol  = col("Item",  "name");
        TableColumn<CartItem, Integer> qtyCol   = col("Qty",   "quantity"); qtyCol.setMinWidth(45); qtyCol.setMaxWidth(70);
        TableColumn<CartItem, Double>  priceCol = col("Price", "price");    priceCol.setMinWidth(65); priceCol.setMaxWidth(90);
        TableColumn<CartItem, Double>  totalCol = col("Total", "total");    totalCol.setMinWidth(70); totalCol.setMaxWidth(100);
        cartTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol);

        Button removeBtn = new Button("🗑  Remove Selected");
        removeBtn.getStyleClass().addAll("button", "btn-danger");
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.setOnAction(e -> {
            CartItem sel = cartTable.getSelectionModel().getSelectedItem();
            if (sel != null) { cartItems.remove(sel); updateTotal(); }
        });

        Separator sep = new Separator();

        totalLabel = new Label("Total: 0.00 EGP");
        totalLabel.getStyleClass().add("total-label");
        totalLabel.setMaxWidth(Double.MAX_VALUE);
        totalLabel.setAlignment(Pos.CENTER_RIGHT);

        Button checkoutBtn = new Button("✅  CHECKOUT");
        checkoutBtn.getStyleClass().add("checkout-btn");
        checkoutBtn.setMaxWidth(Double.MAX_VALUE);
        checkoutBtn.setOnAction(e -> checkout());

        Button clearCartBtn = new Button("🗑  Clear Cart");
        clearCartBtn.getStyleClass().addAll("button", "btn-warning");
        clearCartBtn.setMaxWidth(Double.MAX_VALUE);
        clearCartBtn.setOnAction(e -> { cartItems.clear(); updateTotal(); });

        panel.getChildren().addAll(title, cartTable, removeBtn, sep, totalLabel, checkoutBtn, clearCartBtn);
        return panel;
    }

    private void loadProducts() {
        try {
            User user = authService.getCurrentUser();
            if (user == null || user.getBranchId() == null) return;
            allProducts = productDAO.findByBranchId(user.getBranchId());
            productTable.setItems(FXCollections.observableArrayList(allProducts));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void filterProducts(String query) {
        if (query == null || query.isEmpty()) { productTable.setItems(FXCollections.observableArrayList(allProducts)); return; }
        String q = query.toLowerCase();
        productTable.setItems(FXCollections.observableArrayList(allProducts.stream()
            .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q))
                      || (p.getBarcode() != null && p.getBarcode().toLowerCase().contains(q))
                      || (p.getCategory() != null && p.getCategory().toLowerCase().contains(q)))
            .toList()));
    }

    private void addSelectedToCart() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showStatus("⚠ Select a product first", false); return; }
        if (selected.getQuantity() <= 0) { showStatus("❌ Out of stock!", false); return; }
        TextInputDialog dlg = new TextInputDialog("1");
        dlg.setTitle("Add to Cart");
        dlg.setHeaderText("Product: " + selected.getName() + "  (Available: " + selected.getQuantity() + ")");
        dlg.setContentText("Quantity:");
        dlg.showAndWait().ifPresent(input -> {
            try {
                int qty = Integer.parseInt(input.trim());
                if (qty <= 0) { showStatus("❌ Quantity must be > 0", false); return; }
                if (qty > selected.getQuantity()) { showStatus("❌ Not enough stock (max: " + selected.getQuantity() + ")", false); return; }
                for (CartItem item : cartItems) {
                    if (item.getProduct().getId() == selected.getId()) {
                        int newQty = item.getQuantity() + qty;
                        if (newQty > selected.getQuantity()) { showStatus("❌ Total exceeds stock", false); return; }
                        item.setQuantity(newQty); cartTable.refresh(); updateTotal();
                        showStatus("✅ Updated cart", true); return;
                    }
                }
                cartItems.add(new CartItem(selected, qty)); updateTotal(); showStatus("✅ Added to cart", true);
            } catch (NumberFormatException ex) { showStatus("❌ Invalid quantity", false); }
        });
    }

    private void updateTotal() {
        totalLabel.setText(String.format("Total: %.2f EGP", cartItems.stream().mapToDouble(CartItem::getTotal).sum()));
    }

    private void checkout() {
        if (cartItems.isEmpty()) { showStatus("⚠ Cart is empty", false); return; }
        double total = cartItems.stream().mapToDouble(CartItem::getTotal).sum();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Sale"); confirm.setHeaderText("Complete Sale?");
        confirm.setContentText(String.format("Total: %.2f EGP\nItems: %d", total, cartItems.size()));
        confirm.showAndWait().ifPresent(resp -> {
            if (resp != ButtonType.OK) return;
            try {
                User cashier = authService.getCurrentUser();
                int branchId = cashier != null && cashier.getBranchId() != null ? cashier.getBranchId() : 0;
                Transaction tx = new Transaction(branchId, cashier != null ? cashier.getId() : 0, total, 0, total, "CASH");
                tx.setCashierName(cashier != null ? cashier.getFullName() : "Cashier");
                tx.setStatus("COMPLETED");

                for (CartItem item : cartItems) {
                    inventoryDAO.adjustQuantity(item.getProduct().getId(), branchId, -item.getQuantity());
                    StockMovement mv = new StockMovement(item.getProduct().getId(), branchId, "SALE",
                            item.getQuantity(), cashier != null ? cashier.getId() : null);
                    mv.setUnitPrice(item.getPrice()); mv.setNotes("POS Sale — " + item.getName());
                    movementDAO.insert(mv);

                    TransactionItem txItem = new TransactionItem();
                    txItem.setProductId(item.getProduct().getId());
                    txItem.setProductName(item.getProduct().getName());
                    txItem.setQuantity(item.getQuantity());
                    txItem.setUnitPrice(item.getPrice());
                    txItem.setSubtotal(item.getTotal());
                    tx.addItem(txItem);
                }
                int txnId = transactionDAO.insertWithItems(tx);
                if (txnId > 0) {
                    tx.setTransactionId(txnId);
                    tx.setBranchName("Branch #" + branchId);
                    exportAndOfferInvoiceDownload(tx);
                }
                cartItems.clear(); updateTotal(); loadProducts();
                showStatus(String.format("✅ Sale complete! Total: %.2f EGP", total), true);
            } catch (Exception e) { showStatus("❌ Checkout error: " + e.getMessage(), false); }
        });
    }

    private void exportAndOfferInvoiceDownload(Transaction tx) {
        try {
            Path invoiceDir = Path.of(System.getProperty("user.home"), "SmartStock", "invoices");
            Files.createDirectories(invoiceDir);
            Path savedInvoice = invoiceDir.resolve("invoice_" + tx.getTransactionId() + ".pdf");
            InvoicePDFExporter.exportInvoice(tx, tx.getItems(), savedInvoice.toString());

            Alert prompt = new Alert(Alert.AlertType.CONFIRMATION);
            prompt.setHeaderText("Invoice saved automatically.");
            prompt.setContentText("Saved to:\n" + savedInvoice + "\n\nDownload a copy now?");
            prompt.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            prompt.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Download Invoice");
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
                    chooser.setInitialFileName("invoice_" + tx.getTransactionId() + ".pdf");
                    File file = chooser.showSaveDialog(stage);
                    if (file != null) {
                        try {
                            Files.copy(savedInvoice, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception ignored) {}
                    }
                }
            });
        } catch (Exception ignored) {}
    }

    private void showStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (success ? "#10B981" : "#F87171") + ";");
    }

    private void logout() {
        authService.logout();
        try {
            LoginController ctrl = new LoginController(authService, stage);
            Scene scene = new Scene(ctrl, 480, 620);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            stage.setScene(scene); stage.setTitle("M5zany ERP — Login"); stage.centerOnScreen();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    private <S, T> TableColumn<S, T> col(String name, String prop) {
        TableColumn<S, T> c = new TableColumn<>(name);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        return c;
    }
}
