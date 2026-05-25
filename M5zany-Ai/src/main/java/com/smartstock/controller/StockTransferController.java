package com.smartstock.controller;

import com.smartstock.dao.InventoryDAO;
import com.smartstock.dao.ProductDAO;
import com.smartstock.dao.StockMovementDAO;
import com.smartstock.dao.TransferRequestDAO;
import com.smartstock.dao.AlertDAO;
import com.smartstock.model.Branch;
import com.smartstock.model.Product;
import com.smartstock.model.StockMovement;
import com.smartstock.model.TransferRequest;
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

public class StockTransferController extends VBox {

    private ComboBox<Branch> sourceBranchCombo;
    private ComboBox<Branch> destBranchCombo;
    private TextField productSearchField;
    private TableView<Product> productTable;
    private Label availableQtyLabel;
    private TextField quantityField;
    private TextField notesField;
    private Button transferBtn;
    private Button backBtn;

    private TableView<StockMovement> historyTable;
    private TableView<TransferRequest> requestTable;

    private AuthService authService;
    private Stage stage;
    private BranchService branchService;
    private ProductDAO productDAO;
    private InventoryDAO inventoryDAO;
    private StockMovementDAO movementDAO;
    private TransferRequestDAO transferRequestDAO;
    private AlertDAO alertDAO;

    private List<Product> allBranchProducts = List.of();
    private Product selectedProduct;

    public StockTransferController(AuthService authService, Stage stage) {
        this.authService = authService;
        this.stage = stage;
        this.branchService = new BranchService();
        this.productDAO = new ProductDAO();
        this.inventoryDAO = new InventoryDAO();
        this.movementDAO = new StockMovementDAO();
        this.transferRequestDAO = new TransferRequestDAO();
        this.alertDAO = new AlertDAO();

        com.smartstock.util.ThemeManager.applyTheme(this);
        setSpacing(0);

        buildHeader();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox content = new VBox(14);
        content.setPadding(new Insets(16));
        buildTransferForm(content);
        buildRequestTable(content);
        buildHistoryTable(content);
        scroll.setContent(content);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);

        initBranches();
        loadHistory();
        loadRequests();
    }

    private void buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");
        header.setPadding(new Insets(0, 0, 10, 0));

        Label iconLbl = new Label();
        iconLbl.setGraphic(new FontIcon("mdi2s-swap-horizontal-bold"));
        iconLbl.setStyle("-fx-background-color: rgba(99,102,241,0.1); -fx-border-color: #4F46E5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6; -fx-text-fill: #818CF8;");
        
        Label title = new Label("STOCK TRANSFER");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-letter-spacing: 1px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        backBtn = new Button("< BACK");
        backBtn.setStyle("-fx-background-color: #1A1D24; -fx-border-color: #334155; -fx-border-radius: 6; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-padding: 6 16;");
        backBtn.setOnAction(e -> NavigationHelper.goToDashboard(authService, stage));

        header.getChildren().addAll(iconLbl, title, spacer, backBtn);
        getChildren().add(header);
    }

    private void buildTransferForm(VBox parent) {
        Label formTitle = new Label("TRANSFER CONFIGURATION");
        formTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-letter-spacing: 1.5px;");

        // ─── Branch row ────────────────────────────────────────
        HBox branchRow = new HBox(16);
        branchRow.setAlignment(Pos.CENTER_LEFT);

        VBox sourceBox = new VBox(6);
        Label srcLabel = new Label("FROM BRANCH:");
        srcLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #6366F1; -fx-letter-spacing: 1px;");
        sourceBranchCombo = new ComboBox<>();
        sourceBranchCombo.setPromptText("Select source branch");
        sourceBranchCombo.setMaxWidth(Double.MAX_VALUE);
        sourceBranchCombo.setOnAction(e -> onSourceBranchSelected());
        sourceBox.getChildren().addAll(srcLabel, sourceBranchCombo);
        HBox.setHgrow(sourceBox, Priority.ALWAYS);

        Label arrow = new Label();
        arrow.setGraphic(new FontIcon("mdi2a-arrow-right"));
        arrow.setStyle("-fx-font-size: 22px; -fx-text-fill: #4F46E5; -fx-padding: 10 0 0 0;");

        VBox destBox = new VBox(6);
        Label destLabel = new Label("TO BRANCH:");
        destLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #6366F1; -fx-letter-spacing: 1px;");
        destBranchCombo = new ComboBox<>();
        destBranchCombo.setPromptText("Select destination branch");
        destBranchCombo.setMaxWidth(Double.MAX_VALUE);
        destBox.getChildren().addAll(destLabel, destBranchCombo);
        HBox.setHgrow(destBox, Priority.ALWAYS);

        branchRow.getChildren().addAll(sourceBox, arrow, destBox);

        // ─── Product search & table ────────────────────────────
        Label prodLabel = new Label("— TARGET PRODUCT");
        prodLabel.setStyle("-fx-text-fill: #6366F1; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");

        productSearchField = new TextField();
        productSearchField.setPromptText("🔍  Search products database...");
        productSearchField.setStyle("-fx-background-color: #1A1D24; -fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white; -fx-padding: 6 12;");
        productSearchField.textProperty().addListener((obs, o, n) -> filterProducts(n));

        productTable = new TableView<>();
        productTable.setPrefHeight(180);
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productTable.setPlaceholder(new Label("Select a source branch first"));

        TableColumn<Product, String> nameCol = new TableColumn<>("PRODUCT");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); }
            }
        });

        TableColumn<Product, String> catCol = new TableColumn<>("CATEGORY");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setMaxWidth(110);
        catCol.setCellFactory(c -> new TableCell<>() {
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

        TableColumn<Product, Double> priceCol = new TableColumn<>("PRICE");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        priceCol.setMaxWidth(90);
        priceCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(String.format("$%.2f", item)); setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;"); }
            }
        });

        TableColumn<Product, Integer> qtyCol = new TableColumn<>("AVAILABLE");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setMaxWidth(80);
        qtyCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(String.valueOf(item)); setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); }
            }
        });

        productTable.getColumns().addAll(nameCol, catCol, priceCol, qtyCol);
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            selectedProduct = sel;
            if (sel != null) {
                availableQtyLabel.setText("AVAILABLE: " + sel.getQuantity() + " UNITS");
                availableQtyLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " +
                        (sel.getQuantity() > 0 ? "#10B981" : "#EF4444") + "; -fx-font-size: 10px; -fx-letter-spacing: 1px;");
            } else {
                availableQtyLabel.setText("AVAILABLE: —");
                availableQtyLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748B; -fx-font-size: 10px; -fx-letter-spacing: 1px;");
            }
        });

        // ─── Qty & notes row ────────────────────────────────────
        HBox qtyRow = new HBox(20);
        qtyRow.setAlignment(Pos.BOTTOM_LEFT);

        VBox qtyBox = new VBox(6);
        Label qtyLabel = new Label("TRANSFER VOLUME:");
        qtyLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #6366F1; -fx-letter-spacing: 1px;");
        quantityField = new TextField();
        quantityField.setPromptText("Enter units count");
        quantityField.setMaxWidth(160);
        availableQtyLabel = new Label("AVAILABLE: —");
        availableQtyLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748B; -fx-font-size: 10px; -fx-letter-spacing: 1px;");
        qtyBox.getChildren().addAll(qtyLabel, quantityField, availableQtyLabel);

        VBox notesBox = new VBox(6);
        Label notesLabel = new Label("OPERATION LOG (OPTIONAL):");
        notesLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #6366F1; -fx-letter-spacing: 1px;");
        notesField = new TextField();
        notesField.setPromptText("Reason for transfer...");
        HBox.setHgrow(notesBox, Priority.ALWAYS);
        notesBox.getChildren().addAll(notesLabel, notesField);

        transferBtn = new Button("EXECUTE TRANSFER");
        transferBtn.setGraphic(new FontIcon("mdi2s-swap-horizontal"));
        transferBtn.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.2), 10, 0, 0, 0);");
        transferBtn.setOnAction(e -> executeTransfer());

        qtyRow.getChildren().addAll(qtyBox, notesBox, transferBtn);
        VBox.setMargin(transferBtn, new Insets(18, 0, 18, 0));

        // ─── Wrap in card ────────────────────────────────────────
        VBox formCard = new VBox(18,
                formTitle, branchRow,
                prodLabel, productSearchField, productTable,
                qtyRow);
        formCard.setPadding(new Insets(20));
        formCard.getStyleClass().add("card");
        parent.getChildren().add(formCard);
    }

    private void buildHistoryTable(VBox parent) {
        Label histTitle = new Label("TRANSFER LEDGER");
        histTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-letter-spacing: 1.5px;");

        historyTable = new TableView<>();
        historyTable.setPrefHeight(200);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        historyTable.setPlaceholder(new Label("No transfer history found"));

        TableColumn<StockMovement, String> prodCol = new TableColumn<>("PRODUCT");
        prodCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        prodCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); }
            }
        });

        TableColumn<StockMovement, String> branchCol = new TableColumn<>("ORIGIN");
        branchCol.setCellValueFactory(new PropertyValueFactory<>("branchName"));
        branchCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: #94A3B8;"); }
            }
        });

        TableColumn<StockMovement, Integer> qtyCol = new TableColumn<>("QTY");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setMaxWidth(70);
        qtyCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(String.valueOf(item)); setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;"); }
            }
        });

        TableColumn<StockMovement, String> notesCol = new TableColumn<>("LOG NOTES");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
        notesCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: #818CF8; -fx-font-size: 11px;"); }
            }
        });

        TableColumn<StockMovement, String> dateCol = new TableColumn<>("TIMESTAMP");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAtFormatted"));
        dateCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;"); }
            }
        });

        historyTable.getColumns().addAll(prodCol, branchCol, qtyCol, notesCol, dateCol);

        VBox histCard = new VBox(12, histTitle, historyTable);
        histCard.setPadding(new Insets(20));
        histCard.getStyleClass().add("card");
        parent.getChildren().add(histCard);
    }

    private void buildRequestTable(VBox parent) {
        Label reqTitle = new Label("TRANSFER REQUESTS");
        reqTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-letter-spacing: 1.5px;");
        requestTable = new TableView<>();
        requestTable.setPrefHeight(170);
        requestTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        requestTable.getColumns().addAll(
                colReq("Product", "productName"),
                colReq("From", "fromBranchName"),
                colReq("To", "toBranchName"),
                colReq("Qty", "quantity"),
                colReq("By", "requestedByName"),
                colReq("Status", "status"),
                colReq("At", "requestedAtFormatted")
        );
        HBox actions = new HBox(8);
        Button approveBtn = new Button("Approve");
        Button rejectBtn = new Button("Reject");
        actions.getChildren().addAll(approveBtn, rejectBtn);
        User user = authService.getCurrentUser();
        boolean admin = user != null && user.isAdmin();
        approveBtn.setVisible(admin);
        rejectBtn.setVisible(admin);
        approveBtn.setManaged(admin);
        rejectBtn.setManaged(admin);
        approveBtn.setOnAction(e -> approveSelectedRequest());
        rejectBtn.setOnAction(e -> rejectSelectedRequest());

        VBox card = new VBox(10, reqTitle, requestTable, actions);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("card");
        parent.getChildren().add(card);
    }

    private void initBranches() {
        List<Branch> branches = branchService.getAllBranches();

        javafx.util.StringConverter<Branch> converter = new javafx.util.StringConverter<Branch>() {
            @Override
            public String toString(Branch object) {
                return object == null ? "" : object.getName();
            }
            @Override
            public Branch fromString(String string) {
                return null;
            }
        };

        sourceBranchCombo.setConverter(converter);
        destBranchCombo.setConverter(converter);

        sourceBranchCombo.setItems(FXCollections.observableArrayList(branches));
        destBranchCombo.setItems(FXCollections.observableArrayList(branches));

        // If manager, lock destination to their branch
        User user = authService.getCurrentUser();
        if (user != null && user.isManager() && user.getBranchId() != null) {
            for (Branch b : branches) {
                if (b.getId() == user.getBranchId()) {
                    destBranchCombo.setValue(b);
                    destBranchCombo.setDisable(true);
                    break;
                }
            }
        }
    }

    private void onSourceBranchSelected() {
        Branch src = sourceBranchCombo.getValue();
        if (src == null) return;
        allBranchProducts = productDAO.findByBranchId(src.getId());
        productTable.setItems(FXCollections.observableArrayList(allBranchProducts));
        selectedProduct = null;
        availableQtyLabel.setText("Available: —");
        productSearchField.clear();
    }

    private void filterProducts(String query) {
        if (query == null || query.isEmpty()) {
            productTable.setItems(FXCollections.observableArrayList(allBranchProducts));
            return;
        }
        String q = query.toLowerCase();
        productTable.setItems(FXCollections.observableArrayList(
                allBranchProducts.stream()
                        .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q))
                                || (p.getCategory() != null && p.getCategory().toLowerCase().contains(q)))
                        .toList()
        ));
    }

    private void executeTransfer() {
        Branch src = sourceBranchCombo.getValue();
        Branch dest = destBranchCombo.getValue();

        if (src == null || dest == null) { showAlert("Validation", "Please select both branches."); return; }
        if (src.getId() == dest.getId()) { showAlert("Validation", "Source and destination must be different branches."); return; }
        if (selectedProduct == null) { showAlert("Validation", "Please select a product."); return; }

        int qty;
        try {
            qty = Integer.parseInt(quantityField.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("Validation", "Enter a valid positive quantity.");
            return;
        }

        int available = inventoryDAO.getQuantity(selectedProduct.getId(), src.getId());
        if (qty > available) {
            showAlert("Insufficient Stock",
                    "Only " + available + " units available in " + src.getName() + ".");
            return;
        }

        // Confirm dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Transfer");
        confirm.setHeaderText("Transfer " + qty + " × " + selectedProduct.getName());
        confirm.setContentText("From: " + src.getName() + "\nTo: " + dest.getName() +
                "\n\nThis action cannot be undone.");
        confirm.showAndWait().ifPresent(resp -> {
            if (resp != ButtonType.OK) return;
            try {
                User user = authService.getCurrentUser();
                String notes = notesField.getText().trim();
                String noteText = "Transfer → " + dest.getName() + (notes.isEmpty() ? "" : " | " + notes);

                if (user != null && user.isManager()) {
                    TransferRequest req = new TransferRequest();
                    req.setProductId(selectedProduct.getId());
                    req.setFromBranchId(src.getId());
                    req.setToBranchId(dest.getId());
                    req.setQuantity(qty);
                    req.setRequestedBy(user.getId());
                    req.setNotes(noteText);
                    int reqId = transferRequestDAO.insert(req);
                    if (reqId > 0) {
                        alertDAO.insertCustomAlert(user.getId(), null, "Transfer approval needed: " + qty + " x " +
                                selectedProduct.getName() + " from " + src.getName() + " to " + dest.getName(), "WARNING");
                        showAlert("Request Sent", "Transfer request submitted for admin approval.");
                        loadRequests();
                        quantityField.clear();
                        notesField.clear();
                    } else {
                        showAlert("Error", "Failed to submit transfer request.");
                    }
                    return;
                }

                // 1. Deduct from source
                inventoryDAO.adjustQuantity(selectedProduct.getId(), src.getId(), -qty);
                StockMovement out = new StockMovement(selectedProduct.getId(), src.getId(),
                        "TRANSFER", qty, user != null ? user.getId() : null);
                out.setNotes(noteText);
                movementDAO.insert(out);

                // 2. Add to destination
                inventoryDAO.addOrUpdateQuantity(selectedProduct.getId(), dest.getId(), qty);
                StockMovement in = new StockMovement(selectedProduct.getId(), dest.getId(),
                        "TRANSFER", qty, user != null ? user.getId() : null);
                in.setNotes("Received from " + src.getName() + (notes.isEmpty() ? "" : " | " + notes));
                movementDAO.insert(in);

                showAlert("Success", "Transfer completed!\n" + qty + " units of " +
                        selectedProduct.getName() + " moved to " + dest.getName() + ".");

                // Refresh
                onSourceBranchSelected();
                loadHistory();
                loadRequests();
                quantityField.clear();
                notesField.clear();

            } catch (Exception ex) {
                showAlert("Error", "Transfer failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    private void loadHistory() {
        try {
            User user = authService.getCurrentUser();
            List<StockMovement> history;
            if (user != null && user.isAdmin()) {
                history = movementDAO.findAllTransfers();
            } else if (user != null && user.getBranchId() != null) {
                history = movementDAO.findTransfersByBranch(user.getBranchId());
            } else {
                history = List.of();
            }
            historyTable.setItems(FXCollections.observableArrayList(history));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRequests() {
        User user = authService.getCurrentUser();
        if (user == null) {
            requestTable.setItems(FXCollections.observableArrayList());
            return;
        }
        if (user.isAdmin()) requestTable.setItems(FXCollections.observableArrayList(transferRequestDAO.findPendingForAdmin()));
        else if (user.getBranchId() != null) requestTable.setItems(FXCollections.observableArrayList(transferRequestDAO.findForBranch(user.getBranchId())));
        else requestTable.setItems(FXCollections.observableArrayList());
    }

    private void approveSelectedRequest() {
        TransferRequest req = requestTable.getSelectionModel().getSelectedItem();
        User user = authService.getCurrentUser();
        if (req == null || user == null || !user.isAdmin()) return;
        try {
            int available = inventoryDAO.getQuantity(req.getProductId(), req.getFromBranchId());
            if (req.getQuantity() > available) {
                showAlert("Cannot Approve", "Insufficient stock in source branch.");
                return;
            }
            if (!transferRequestDAO.approve(req.getRequestId(), user.getId())) {
                showAlert("Warning", "Request is no longer pending or already processed.");
                loadRequests();
                return;
            }
            
            inventoryDAO.adjustQuantity(req.getProductId(), req.getFromBranchId(), -req.getQuantity());
            inventoryDAO.addOrUpdateQuantity(req.getProductId(), req.getToBranchId(), req.getQuantity());
            StockMovement out = new StockMovement(req.getProductId(), req.getFromBranchId(), "TRANSFER", req.getQuantity(), user.getId());
            out.setNotes("Approved transfer to " + req.getToBranchName() + " | request #" + req.getRequestId());
            movementDAO.insert(out);
            StockMovement in = new StockMovement(req.getProductId(), req.getToBranchId(), "TRANSFER", req.getQuantity(), user.getId());
            in.setNotes("Received from " + req.getFromBranchName() + " | request #" + req.getRequestId());
            movementDAO.insert(in);
            
            // Notify the requester's branch
            alertDAO.insertCustomAlert(user.getId(), req.getToBranchId(), 
                "Transfer APPROVED: " + req.getQuantity() + "x " + req.getProductName() + " from " + req.getFromBranchName(), "INFO");
                
            loadRequests();
            loadHistory();
            showAlert("Approved", "Transfer request approved and executed.");
        } catch (Exception ex) {
            showAlert("Error", "Approval failed: " + ex.getMessage());
        }
    }

    private void rejectSelectedRequest() {
        TransferRequest req = requestTable.getSelectionModel().getSelectedItem();
        User user = authService.getCurrentUser();
        if (req == null || user == null || !user.isAdmin()) return;
        if (transferRequestDAO.reject(req.getRequestId(), user.getId())) {
            // Notify the requester's branch
            alertDAO.insertCustomAlert(user.getId(), req.getToBranchId(), 
                "Transfer REJECTED: " + req.getQuantity() + "x " + req.getProductName() + " from " + req.getFromBranchName(), "WARNING");
                
            loadRequests();
            showAlert("Rejected", "Transfer request rejected.");
        }
    }

    @SuppressWarnings("unchecked")
    private <S, T> TableColumn<S, T> colReq(String name, String prop) {
        TableColumn<S, T> c = new TableColumn<>(name);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        return c;
    }

    private void showAlert(String title, String message) {
        FontIcon icon = new FontIcon("mdi2i-information-outline");
        icon.setIconSize(32);
        icon.setIconColor(javafx.scene.paint.Color.web("#6366F1"));

        Notifications.create()
            .title(title)
            .text(message)
            .graphic(icon)
            .position(Pos.CENTER)
            .hideAfter(Duration.seconds(4))
            .show();
    }
}
