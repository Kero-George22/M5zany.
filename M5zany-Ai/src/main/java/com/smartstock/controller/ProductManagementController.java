package com.smartstock.controller;

import com.smartstock.dao.InventoryDAO;
import com.smartstock.dao.ProductDAO;
import com.smartstock.dao.CategoryDAO;
import com.smartstock.model.Branch;
import com.smartstock.model.Category;
import com.smartstock.model.Product;
import com.smartstock.model.User;
import com.smartstock.service.AuthService;
import com.smartstock.service.BranchService;
import com.smartstock.util.NavigationHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.controlsfx.control.Notifications;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductManagementController extends VBox {

    private TableView<Product> productTable;
    private TableColumn<Product, Integer> idCol;
    private TableColumn<Product, String>  nameCol, barcodeCol, categoryCol;
    private TableColumn<Product, Double>  priceCol;
    private TableColumn<Product, Integer> qtyCol, minStockCol;

    private TextField nameField, barcodeField, unitCostField, sellingPriceField, quantityField, minStockField;
    private DatePicker expiryDatePicker;
    private ComboBox<Branch> branchCombo;
    private ComboBox<Category> categoryCombo;
    private Button saveBtn, deleteBtn, clearBtn, backBtn;
    // Buttons are promoted to footer — declared here so buildFormCard can init them
    
    private Label totalProductsLbl, totalValueLbl, lowStockLbl, outOfStockLbl;

    private final AuthService authService;
    private final Stage stage;
    private final ProductDAO productDAO;
    private final InventoryDAO inventoryDAO;
    private final BranchService branchService;
    private final CategoryDAO categoryDAO;
    private Product selectedProduct;
    private final boolean isAdmin;

    public ProductManagementController(AuthService authService, Stage stage) {
        this.authService  = authService;
        this.stage        = stage;
        this.productDAO   = new ProductDAO();
        this.inventoryDAO = new InventoryDAO();
        this.branchService = new BranchService();
        this.categoryDAO = new CategoryDAO();
        User user = authService.getCurrentUser();
        this.isAdmin = user != null && user.isAdmin();

        com.smartstock.util.ThemeManager.applyTheme(this);
        setSpacing(0);

        buildHeader();

        // Main scrollable content (table + form, NO buttons)
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 20, 10, 20));
        VBox.setVgrow(content, Priority.ALWAYS);
        buildTableCard(content);
        buildFormCard(content);
        getChildren().add(content);

        // Pinned footer with action buttons
        getChildren().add(buildButtonFooter());

        // Columns are wired in buildTableCard

        if (isAdmin) branchCombo.setItems(FXCollections.observableArrayList(branchService.getAllBranches()));
        else branchCombo.setDisable(true);

        ensureDefaultCategories();
        List<Category> categories = categoryDAO.findAll();
        categoryCombo.setItems(FXCollections.observableArrayList(categories));
        backfillExistingProductsWithoutCategory(categories);

        branchCombo.setConverter(new javafx.util.StringConverter<Branch>() {
            @Override
            public String toString(Branch object) {
                return object == null ? "" : object.getName();
            }
            @Override
            public Branch fromString(String string) {
                return null;
            }
        });

        categoryCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Category object) {
                return object == null ? "" : object.getName();
            }
            @Override
            public Category fromString(String string) {
                return null;
            }
        });

        productTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                selectedProduct = sel;
                nameField.setText(sel.getName());
                barcodeField.setText(sel.getBarcode());
                selectCategoryForProduct(sel);
                unitCostField.setText(String.valueOf(sel.getUnitCost()));
                sellingPriceField.setText(String.valueOf(sel.getSellingPrice()));
                expiryDatePicker.setValue(sel.getExpiryDate());
                quantityField.setText(String.valueOf(sel.getQuantity()));
                minStockField.setText(String.valueOf(sel.getMinStock()));
                if (isAdmin && sel.getBranchId() != null) {
                    branchCombo.getItems().stream().filter(b -> b.getId() == sel.getBranchId()).findFirst().ifPresent(branchCombo::setValue);
                }
            }
        });

        backBtn.setOnAction(e -> NavigationHelper.goToDashboard(authService, stage));

        loadProducts();
    }

    private void buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");
        header.setPadding(new Insets(0, 0, 10, 0));

        Label iconLbl = new Label();
        iconLbl.setGraphic(new FontIcon("mdi2p-package-variant-closed"));
        iconLbl.setStyle("-fx-background-color: rgba(99,102,241,0.1); -fx-border-color: #4F46E5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6; -fx-text-fill: #818CF8;");
        
        Label title = new Label("PRODUCT & INVENTORY MANAGEMENT");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-letter-spacing: 1px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        backBtn = new Button("< BACK");
        backBtn.setStyle("-fx-background-color: #1A1D24; -fx-border-color: #334155; -fx-border-radius: 6; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-padding: 6 16;");

        header.getChildren().addAll(iconLbl, title, spacer, backBtn);
        getChildren().add(header);
    }

    private void buildTableCard(VBox parent) {
        Label lbl = new Label("PRODUCT INVENTORY");
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-letter-spacing: 1.5px;");

        TextField searchBox = new TextField();
        searchBox.setPromptText("🔍 Search products...");
        searchBox.setStyle("-fx-background-color: #1A1D24; -fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white; -fx-padding: 6 12;");
        searchBox.setPrefWidth(250);

        HBox tableHeader = new HBox(lbl);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        tableHeader.getChildren().addAll(sp, searchBox);
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(0, 0, 10, 0));

        productTable = new TableView<>();
        productTable.setPrefHeight(300);
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        productTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            // Context Menu
            ContextMenu contextMenu = new ContextMenu();
            
            MenuItem viewItem = new MenuItem("👁 View Details");
            viewItem.setOnAction(e -> {
                Product selected = row.getItem();
                if (selected != null) {
                    openProductDetails(selected);
                }
            });

            MenuItem editItem = new MenuItem("✎ Edit Product");
            editItem.setOnAction(e -> {
                productTable.getSelectionModel().select(row.getItem());
                nameField.requestFocus();
                // trigger selection listener
            });
            MenuItem deleteItem = new MenuItem("🗑 Delete Product");
            deleteItem.setStyle("-fx-text-fill: #EF4444;");
            deleteItem.setOnAction(e -> {
                productTable.getSelectionModel().select(row.getItem());
                deleteProduct();
            });
            MenuItem transferItem = new MenuItem("🚚 Transfer Stock");
            transferItem.setOnAction(e -> {
                showAlert("Info", "Stock Transfer module opened. Select this product there.");
            });

            contextMenu.getItems().addAll(viewItem, editItem, transferItem, new SeparatorMenuItem(), deleteItem);

            row.contextMenuProperty().bind(
                javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((ContextMenu) null)
                .otherwise(contextMenu)
            );

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    Product selected = row.getItem();
                    openProductDetails(selected);
                }
            });

            return row;
        });

        idCol = new TableColumn<>("ID"); idCol.setMaxWidth(60);
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText("#" + item); setStyle("-fx-text-fill: #94A3B8;"); }
            }
        });

        nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); }
            }
        });

        barcodeCol = new TableColumn<>("BARCODE");
        barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        barcodeCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: #818CF8; -fx-font-family: monospace; -fx-font-size: 11px;"); }
            }
        });

        categoryCol = new TableColumn<>("CATEGORY");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label badge = new Label(item.toUpperCase());
                    badge.setStyle("-fx-font-weight: bold; -fx-font-size: 9px; -fx-padding: 3 8; -fx-background-radius: 4; -fx-background-color: rgba(148,163,184,0.1); -fx-text-fill: #94A3B8;");
                    setGraphic(badge);
                }
            }
        });

        priceCol = new TableColumn<>("PRICE");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        priceCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(String.format("$%.2f", item)); setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;"); }
            }
        });

        qtyCol = new TableColumn<>("QTY");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(String.valueOf(item)); setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); }
            }
        });

        minStockCol = new TableColumn<>("MIN");
        minStockCol.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        minStockCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(String.valueOf(item)); setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;"); }
            }
        });

        productTable.getColumns().addAll(idCol, nameCol, barcodeCol, categoryCol, priceCol, qtyCol, minStockCol);

        VBox card = new VBox(tableHeader, productTable);
        parent.getChildren().add(card);
    }

    private void buildFormCard(VBox parent) {
        nameField        = field("Enter product name");
        unitCostField    = field("0.00");
        sellingPriceField= field("0.00");
        expiryDatePicker = new DatePicker(); expiryDatePicker.setPromptText("Select Expiry");
        quantityField    = field("0");
        minStockField    = field("0");
        branchCombo      = new ComboBox<>(); branchCombo.setPromptText("Select Branch");
        categoryCombo    = new ComboBox<>(); categoryCombo.setPromptText("Select Category");
        if (!isAdmin) branchCombo.setDisable(true);

        barcodeField = new TextField();
        barcodeField.setPromptText("Barcode");
        HBox.setHgrow(barcodeField, Priority.ALWAYS);
        Button genBarcodeBtn = new Button();
        genBarcodeBtn.setGraphic(new FontIcon("mdi2b-barcode-scan"));
        genBarcodeBtn.setStyle("-fx-background-color: #1A1D24; -fx-border-color: #334155; -fx-border-radius: 6; -fx-text-fill: #94A3B8; -fx-padding: 4 8;");
        genBarcodeBtn.setTooltip(new Tooltip("Generate EAN-13 Barcode (622)"));
        genBarcodeBtn.setOnAction(e -> generateEAN13Barcode());
        HBox barcodeBox = new HBox(4, barcodeField, genBarcodeBtn);
        barcodeBox.setAlignment(Pos.CENTER_LEFT);

        // Column 1: DETAILS
        Label idTitle = new Label("— PRODUCT DETAILS");
        idTitle.setStyle("-fx-text-fill: #6366F1; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");
        VBox col1 = new VBox(16, idTitle, labeled("NAME", nameField), labeled("CATEGORY", categoryCombo), labeled("BARCODE", barcodeBox));
        col1.setPrefWidth(300);

        // Column 2: PRICING
        Label accessTitle = new Label("— PRICING");
        accessTitle.setStyle("-fx-text-fill: #6366F1; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");
        VBox col2 = new VBox(16, accessTitle, labeled("UNIT COST", unitCostField), labeled("SELLING PRICE", sellingPriceField), labeled("EXPIRY DATE", expiryDatePicker));
        col2.setPrefWidth(300);

        // Column 3: STOCK & BRANCH
        Label contextTitle = new Label("— STOCK & LOCATION");
        contextTitle.setStyle("-fx-text-fill: #6366F1; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");
        VBox col3 = new VBox(16, contextTitle, labeled("INITIAL QTY", quantityField), labeled("MIN STOCK", minStockField), labeled("BRANCH", branchCombo));
        col3.setPrefWidth(300);

        HBox formGrid = new HBox(30, col1, col2, col3);
        formGrid.setAlignment(Pos.TOP_CENTER);
        formGrid.setPadding(new Insets(20, 0, 40, 0));

        // Buttons are built in buildButtonFooter() — init them here so actions can be wired
        saveBtn   = new Button("SAVE INSTANCE");
        deleteBtn = new Button("DELETE");
        clearBtn  = new Button("CLEAR");

        VBox card = new VBox(formGrid);
        parent.getChildren().add(card);
    }

    private HBox buildButtonFooter() {
        saveBtn.setGraphic(new FontIcon("mdi2c-content-save-outline"));
        saveBtn.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8;");
        saveBtn.setOnAction(e -> saveProduct());

        deleteBtn.setGraphic(new FontIcon("mdi2t-trash-can-outline"));
        deleteBtn.setStyle("-fx-background-color: rgba(239,68,68,0.1); -fx-text-fill: #EF4444; -fx-border-color: rgba(239,68,68,0.2); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 24; -fx-font-weight: bold;");
        deleteBtn.setOnAction(e -> deleteProduct());

        clearBtn.setGraphic(new FontIcon("mdi2e-eraser"));
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-border-color: #334155; -fx-border-radius: 8; -fx-padding: 10 24; -fx-font-weight: bold;");
        clearBtn.setOnAction(e -> clearForm());

        HBox footer = new HBox(16, saveBtn, deleteBtn, clearBtn);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(14, 20, 14, 20));
        footer.setStyle("-fx-background-color: #0D1117; -fx-border-color: #1E232E; -fx-border-width: 1 0 0 0;");
        return footer;
    }

    private TextField field(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt); return tf;
    }

    private VBox labeled(String label, Region ctrl) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-letter-spacing: 1px;");
        ctrl.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, lbl, ctrl);
    }

    private void generateEAN13Barcode() {
        String prefix = "622";
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 9; i++) {
            sb.append((int)(Math.random() * 10));
        }
        String withoutCheck = sb.toString();
        
        int sumOdd = 0;
        int sumEven = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(withoutCheck.charAt(i));
            if (i % 2 == 0) sumOdd += digit;
            else sumEven += digit;
        }
        int totalSum = sumOdd + (sumEven * 3);
        int checkDigit = (10 - (totalSum % 10)) % 10;
        
        barcodeField.setText(withoutCheck + checkDigit);
    }

    private void loadProducts() {
        try {
            List<Product> products;
            if (isAdmin) products = productDAO.findAll();
            else {
                Integer branchId = authService.getCurrentUser().getBranchId();
                products = branchId != null ? productDAO.findByBranchId(branchId) : List.of();
            }
            productTable.setItems(FXCollections.observableArrayList(products));
            
            // Update real numbers for stat cards
            int totalProducts = products.size();
            double totalValue = products.stream().mapToDouble(p -> p.getUnitCost() * p.getQuantity()).sum();
            int lowStock = (int) products.stream().filter(p -> p.getQuantity() > 0 && p.getQuantity() <= p.getMinStock()).count();
            int outOfStock = (int) products.stream().filter(p -> p.getQuantity() == 0).count();
            
            if (totalProductsLbl != null) totalProductsLbl.setText(String.valueOf(totalProducts));
            if (totalValueLbl != null) {
                // format value nicely (e.g. 84K)
                if (totalValue >= 1000) {
                    totalValueLbl.setText(String.format("%.1fK", totalValue / 1000.0));
                } else {
                    totalValueLbl.setText(String.format("%.0f", totalValue));
                }
            }
            if (lowStockLbl != null) lowStockLbl.setText(String.valueOf(lowStock));
            if (outOfStockLbl != null) outOfStockLbl.setText(String.valueOf(outOfStock));
            
        } catch (Exception e) { showAlert("Error", "Failed to load products: " + e.getMessage()); }
    }

    private void saveProduct() {
        String name = nameField.getText().trim();
        String barcode = barcodeField.getText().trim();
        Category selectedCategory = categoryCombo.getValue();
        String unitCostStr = unitCostField.getText().trim();
        String sellingPriceStr = sellingPriceField.getText().trim();
        String qtyStr = quantityField.getText().trim();
        String minStockStr = minStockField.getText().trim();

        if (name.isEmpty() || selectedCategory == null || unitCostStr.isEmpty() || sellingPriceStr.isEmpty() || qtyStr.isEmpty() || minStockStr.isEmpty()) {
            showAlert("Validation", "All fields are required, including category."); return;
        }
        if (barcode.isEmpty()) {
            barcode = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        try {
            double uc = parseDouble(unitCostField), sp = parseDouble(sellingPriceField);
            int qty = parseInt(quantityField), min = parseInt(minStockField);
            Integer branchId = isAdmin ? (branchCombo.getValue() != null ? branchCombo.getValue().getId() : null)
                    : authService.getCurrentUser().getBranchId();
            if (branchId == null) { showAlert("Validation", "Please select a branch"); return; }
            if (selectedProduct != null) {
                selectedProduct.setName(name); selectedProduct.setBarcode(barcode);
                selectedProduct.setCategory(selectedCategory.getName());
                selectedProduct.setCategoryId(selectedCategory.getCategoryId());
                selectedProduct.setUnitCost(uc);
                selectedProduct.setSellingPrice(sp); selectedProduct.setExpiryDate(expiryDatePicker.getValue());
                selectedProduct.setBranchId(branchId); selectedProduct.setQuantity(qty); selectedProduct.setMinStock(min);
                productDAO.update(selectedProduct);
                inventoryDAO.updateQuantity(selectedProduct.getId(), branchId, qty);
                showAlert("Success", "Product updated.");
            } else {
                Product p = new Product(name, barcode, selectedCategory.getName(), uc, sp, branchId);
                p.setCategoryId(selectedCategory.getCategoryId());
                p.setExpiryDate(expiryDatePicker.getValue()); p.setQuantity(qty); p.setMinStock(min);
                int id = productDAO.insert(p);
                showAlert(id > 0 ? "Success" : "Error", id > 0 ? "Product created." : "Failed to create product.");
            }
            clearForm(); loadProducts();
        } catch (NumberFormatException e) { showAlert("Validation", "Invalid number format in cost/price/quantity fields."); }
        catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    private void deleteProduct() {
        if (selectedProduct == null) { showAlert("No Selection", "Select a product to delete"); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selectedProduct.getName() + "?");
        confirm.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) {
            try { productDAO.delete(selectedProduct.getId()); clearForm(); loadProducts(); showAlert("Deleted", "Product removed.");
            } catch (Exception e) { showAlert("Error", e.getMessage()); }
        }});
    }

    private void clearForm() {
        selectedProduct = null;
        nameField.clear(); barcodeField.clear(); categoryCombo.setValue(null); unitCostField.clear();
        sellingPriceField.clear(); expiryDatePicker.setValue(null); quantityField.clear(); minStockField.clear();
        branchCombo.setValue(null);
    }

    private double parseDouble(TextField tf) { return tf.getText().trim().isEmpty() ? 0 : Double.parseDouble(tf.getText().trim()); }
    private int    parseInt(TextField tf)    { return tf.getText().trim().isEmpty() ? 0 : Integer.parseInt(tf.getText().trim()); }

    private void openProductDetails(Product selected) {
        if (getScene() == null || getScene().getRoot() == null) return;
        if (getScene().getRoot() instanceof AdminDashboardController adminDashboard) {
            adminDashboard.openPage(new ProductDetailController(selected, authService, stage), "Product: " + selected.getName());
            return;
        }
        if (getScene().getRoot() instanceof ManagerDashboardController managerDashboard) {
            managerDashboard.openPage(new ProductDetailController(selected, authService, stage), "Product: " + selected.getName());
        }
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

    private void selectCategoryForProduct(Product sel) {
        if (categoryCombo == null || categoryCombo.getItems() == null) return;
        if (sel.getCategoryId() != null) {
            categoryCombo.getItems().stream()
                    .filter(c -> c.getCategoryId() == sel.getCategoryId())
                    .findFirst()
                    .ifPresent(categoryCombo::setValue);
            if (categoryCombo.getValue() != null) return;
        }
        if (sel.getCategory() != null) {
            categoryCombo.getItems().stream()
                    .filter(c -> sel.getCategory().equalsIgnoreCase(c.getName()))
                    .findFirst()
                    .ifPresent(categoryCombo::setValue);
        }
    }

    private void ensureDefaultCategories() {
        if (!categoryDAO.findAll().isEmpty()) return;
        categoryDAO.insert(new Category("Dairy & Eggs", "Milk, cheese, yogurt, butter, eggs"));
        categoryDAO.insert(new Category("Beverages", "Water, juice, soft drinks, energy drinks, tea, coffee"));
        categoryDAO.insert(new Category("Snacks", "Chips, biscuits, chocolate, candy, nuts"));
        categoryDAO.insert(new Category("Grains & Staples", "Rice, flour, pasta, sugar, salt, oil"));
        categoryDAO.insert(new Category("Canned & Packaged", "Canned tomatoes, beans, tuna, sauces, condiments"));
        categoryDAO.insert(new Category("Cleaning & Household", "Detergents, dish soap, fabric softener, trash bags"));
        categoryDAO.insert(new Category("Personal Care", "Shampoo, soap, toothpaste, deodorant, razors"));
        categoryDAO.insert(new Category("Frozen Foods", "Frozen vegetables, frozen meals, ice cream"));
        categoryDAO.insert(new Category("Bread & Bakery", "White bread, toast, buns, pita, croissants"));
        categoryDAO.insert(new Category("Meat & Poultry", "Chicken, beef, frozen cuts, sausages"));
    }

    private void backfillExistingProductsWithoutCategory(List<Category> categories) {
        if (categories == null || categories.isEmpty()) return;
        Map<String, Integer> categoryMap = categories.stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(Locale.ROOT), Category::getCategoryId, (a, b) -> a));
        int updated = productDAO.backfillMissingCategories(categoryMap);
        if (updated > 0) {
            showAlert("Categories Updated", updated + " existing products were assigned categories.");
        }
    }
}
