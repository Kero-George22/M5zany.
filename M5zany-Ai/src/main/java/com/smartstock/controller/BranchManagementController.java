package com.smartstock.controller;

import com.smartstock.model.Branch;
import com.smartstock.model.Product;
import com.smartstock.model.User;
import com.smartstock.dao.ProductDAO;
import com.smartstock.dao.UserDAO;
import com.smartstock.service.AuthService;
import com.smartstock.service.BranchService;
import com.smartstock.util.NavigationHelper;
import com.smartstock.util.PDFExporter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.util.List;

public class BranchManagementController extends VBox {

    private TableView<Branch> branchTable;
    private FilteredList<Branch> filteredBranches;
    private ObservableList<Branch> allBranchData = FXCollections.observableArrayList();
    private TableColumn<Branch, Integer> idCol;
    private TableColumn<Branch, String>  nameCol, locationCol, phoneCol;
    private TableColumn<Branch, Integer> productCountCol, totalQtyCol, lowStockCol;

    private TextField nameField, locationField, phoneField, emailField;
    private CheckBox  activeCheckBox;
    private Button    saveBtn, deleteBtn, clearBtn, backBtn;

    private final AuthService   authService;
    private final Stage         stage;
    private final BranchService branchService;
    private final ProductDAO productDAO = new ProductDAO();
    private final UserDAO userDAO = new UserDAO();
    private Branch selectedBranch;

    public BranchManagementController(AuthService authService, Stage stage) {
        this.authService   = authService;
        this.stage         = stage;
        this.branchService = new BranchService();

        com.smartstock.util.ThemeManager.applyTheme(this);
        setSpacing(0);

        buildHeader();

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        buildTableCard(content);
        buildFormCard(content);

        scroll.setContent(content);
        getChildren().add(scroll);

        // Columns are wired in buildTableCard

        branchTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                selectedBranch = sel;
                nameField.setText(sel.getName());
                locationField.setText(sel.getLocation());
                phoneField.setText(sel.getPhone());
                emailField.setText(sel.getEmail());
                activeCheckBox.setSelected(sel.isActive());
            }
        });

        saveBtn.setOnAction(e -> saveBranch());
        deleteBtn.setOnAction(e -> deleteBranch());
        clearBtn.setOnAction(e -> clearForm());
        backBtn.setOnAction(e -> NavigationHelper.goToDashboard(authService, stage));

        loadBranches();
    }

    private void buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");
        header.setPadding(new Insets(0, 0, 10, 0));

        Label iconLbl = new Label();
        iconLbl.setGraphic(new FontIcon("mdi2s-storefront-outline"));
        iconLbl.setStyle("-fx-background-color: rgba(99,102,241,0.1); -fx-border-color: #4F46E5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6; -fx-text-fill: #818CF8;");
        
        Label title = new Label("BRANCH MANAGEMENT");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-letter-spacing: 1px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        backBtn = new Button("< BACK");
        backBtn.setStyle("-fx-background-color: #1A1D24; -fx-border-color: #334155; -fx-border-radius: 6; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-padding: 6 16;");

        header.getChildren().addAll(iconLbl, title, spacer, backBtn);
        getChildren().add(header);
    }

    private FlowPane branchCardsBox;

    private void buildTableCard(VBox parent) {
        Label lbl = new Label("BRANCH REGISTRY");
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-letter-spacing: 1.5px;");

        TextField searchBox = new TextField();
        searchBox.setPromptText("🔍 Search branches...");
        searchBox.setStyle("-fx-background-color: #1A1D24; -fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white; -fx-padding: 6 12;");
        searchBox.setPrefWidth(200);

        ToggleButton lowStockToggle = new ToggleButton("⚠ Low Stock Only");
        lowStockToggle.setStyle("-fx-background-color: #1A1D24; -fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #94A3B8; -fx-padding: 6 12;");

        Button printReportBtn = new Button("🖨 Print Selected Report");
        printReportBtn.setStyle("-fx-background-color: rgba(99,102,241,0.15); -fx-border-color: #6366F1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #818CF8; -fx-padding: 6 12; -fx-font-weight: bold;");
        printReportBtn.setOnAction(e -> exportSelectedBranchReport());

        HBox tableHeader = new HBox(lbl);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        tableHeader.getChildren().addAll(sp, printReportBtn, searchBox, lowStockToggle);
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setSpacing(10);
        tableHeader.setPadding(new Insets(0, 0, 10, 0));

        branchTable = new TableView<>();
        branchTable.setPrefHeight(250);
        branchTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        branchTable.setStyle("-fx-cursor: hand;");

        idCol = new TableColumn<>("ID"); idCol.setMaxWidth(60);
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText("#" + item); setStyle("-fx-text-fill: #94A3B8;"); }
            }
        });

        nameCol = new TableColumn<>("Branch Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-font-weight: bold; -fx-text-fill: white;"); }
            }
        });

        locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        locationCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: #94A3B8;"); }
            }
        });

        productCountCol = new TableColumn<>("Products");
        productCountCol.setCellValueFactory(new PropertyValueFactory<>("productCount"));
        productCountCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(String.valueOf(item)); setStyle("-fx-text-fill: #6366F1; -fx-font-weight: bold;"); }
            }
        });

        totalQtyCol = new TableColumn<>("Total Qty");
        totalQtyCol.setCellValueFactory(new PropertyValueFactory<>("totalQuantity"));
        totalQtyCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(String.valueOf(item)); setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;"); }
            }
        });

        lowStockCol = new TableColumn<>("⚠ Low Stock");
        lowStockCol.setCellValueFactory(new PropertyValueFactory<>("lowStockCount"));
        lowStockCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(String.valueOf(item)); setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;"); }
            }
        });

        branchTable.getColumns().addAll(idCol, nameCol, locationCol, productCountCol, totalQtyCol, lowStockCol);

        branchTable.setRowFactory(tv -> {
            TableRow<Branch> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) {
                    Branch b = row.getItem();
                    selectedBranch = b;
                    nameField.setText(b.getName());
                    locationField.setText(b.getLocation());
                    phoneField.setText(b.getPhone());
                    emailField.setText(b.getEmail());
                    activeCheckBox.setSelected(b.isActive());
                    if (e.getClickCount() == 2) {
                        openPage(new BranchDetailController(b, authService, stage), "Branch: " + b.getName());
                    }
                }
            });
            return row;
        });

        VBox topArea = new VBox(4, tableHeader, branchTable);

        filteredBranches = new FilteredList<>(allBranchData, b -> true);
        branchTable.setItems(filteredBranches);



        Runnable applyFilter = () -> {
            String search = searchBox.getText() == null ? "" : searchBox.getText().toLowerCase();
            boolean lowOnly = lowStockToggle.isSelected();
            filteredBranches.setPredicate(b -> {
                boolean nameMatch = b.getName() != null && b.getName().toLowerCase().contains(search)
                        || b.getLocation() != null && b.getLocation().toLowerCase().contains(search);
                boolean stockMatch = !lowOnly || b.getLowStockCount() > 0;
                return nameMatch && stockMatch;
            });
        };

        searchBox.textProperty().addListener((obs, o, n) -> applyFilter.run());
        lowStockToggle.selectedProperty().addListener((obs, o, n) -> applyFilter.run());

        VBox container = new VBox(topArea);
        container.getStyleClass().add("card");
        VBox.setVgrow(container, Priority.ALWAYS);
        parent.getChildren().add(container);
    }

    private void updateBranchCards() {
        // Method unused as we reverted to TableView
    }

    private void openPage(VBox page, String title) {
        AdminDashboardController dashboard = (AdminDashboardController) getScene().getRoot();
        dashboard.openPage(page, title);
    }

    private void buildFormCard(VBox parent) {
        nameField     = field("Enter branch name");
        locationField = field("Enter location");
        phoneField    = field("Enter phone");
        emailField    = field("branch@example.com");
        
        // Column 1: DETAILS
        Label idTitle = new Label("— BRANCH DETAILS");
        idTitle.setStyle("-fx-text-fill: #6366F1; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");
        VBox col1 = new VBox(16, idTitle, labeled("NAME", nameField), labeled("LOCATION", locationField));
        col1.setPrefWidth(300);

        // Column 2: CONTACT
        Label accessTitle = new Label("— CONTACT INFO");
        accessTitle.setStyle("-fx-text-fill: #6366F1; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");
        VBox col2 = new VBox(16, accessTitle, labeled("PHONE", phoneField), labeled("EMAIL", emailField));
        col2.setPrefWidth(300);

        // Column 3: CONTEXT
        Label contextTitle = new Label("— SETTINGS");
        contextTitle.setStyle("-fx-text-fill: #6366F1; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");

        HBox activeCard = new HBox(12);
        activeCard.setAlignment(Pos.CENTER_LEFT);
        activeCard.setStyle("-fx-background-color: #11141A; -fx-border-color: #2A2F3A; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 14;");
        activeCheckBox = new CheckBox(); activeCheckBox.setSelected(true);
        VBox activeText = new VBox(2);
        Label actMain = new Label("BRANCH ACTIVE"); actMain.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 12px;");
        Label actSub = new Label("Allow transactions and operations"); actSub.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px; -fx-font-style: italic;");
        activeText.getChildren().addAll(actMain, actSub);
        activeCard.getChildren().addAll(activeCheckBox, activeText);

        VBox col3 = new VBox(16, contextTitle, activeCard);
        col3.setPrefWidth(300);

        HBox formGrid = new HBox(30, col1, col2, col3);
        formGrid.setAlignment(Pos.TOP_CENTER);
        formGrid.setPadding(new Insets(20, 0, 40, 0));

        saveBtn = new Button("SAVE INSTANCE");
        saveBtn.setGraphic(new FontIcon("mdi2c-content-save-outline"));
        saveBtn.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.2), 10, 0, 0, 0);");

        deleteBtn = new Button("DELETE");
        deleteBtn.setGraphic(new FontIcon("mdi2t-trash-can-outline"));
        deleteBtn.setStyle("-fx-background-color: rgba(239, 68, 68, 0.1); -fx-text-fill: #EF4444; -fx-border-color: rgba(239, 68, 68, 0.2); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 24; -fx-font-weight: bold;");

        clearBtn = new Button("CLEAR");
        clearBtn.setGraphic(new FontIcon("mdi2e-eraser"));
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-border-color: #334155; -fx-border-radius: 8; -fx-padding: 10 24; -fx-font-weight: bold;");

        HBox btnRow = new HBox(16, saveBtn, deleteBtn, clearBtn);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(20, 0, 0, 0));
        btnRow.setStyle("-fx-border-color: #1E232E; -fx-border-width: 1 0 0 0; -fx-padding: 30 0 10 0;");

        VBox card = new VBox(formGrid, btnRow);
        parent.getChildren().add(card);
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

    private void loadBranches() {
        allBranchData.setAll(branchService.getAllBranches());
    }

    private void saveBranch() {
        String name = nameField.getText().trim();
        String location = locationField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty() || location.isEmpty() || phone.isEmpty() || email.isEmpty()) { 
            showAlert("Validation", "All fields are required."); return; 
        }
        if (!phone.matches("\\d{5,11}")) {
            showAlert("Validation", "Phone number must be between 5 and 11 digits."); return;
        }
        if (!email.endsWith("@gmail.com")) {
            showAlert("Validation", "Email must be a @gmail.com address."); return;
        }
        if (selectedBranch != null) {
            selectedBranch.setName(name);
            selectedBranch.setLocation(locationField.getText().trim());
            selectedBranch.setPhone(phoneField.getText().trim());
            selectedBranch.setEmail(emailField.getText().trim());
            selectedBranch.setActive(activeCheckBox.isSelected());
            branchService.updateBranch(selectedBranch);
            showAlert("Success", "Branch updated.");
        } else {
            Branch b = branchService.createBranch(name, locationField.getText().trim(),
                    phoneField.getText().trim(), emailField.getText().trim());
            showAlert(b != null ? "Success" : "Error", b != null ? "Branch created." : "Failed to create branch.");
        }
        clearForm(); loadBranches();
    }

    private void deleteBranch() {
        if (selectedBranch == null) { showAlert("No Selection", "Select a branch to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete branch " + selectedBranch.getName() + "?");
        confirm.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) {
            branchService.deleteBranch(selectedBranch.getId()); clearForm(); loadBranches();
        }});
    }

    private void clearForm() {
        selectedBranch = null;
        nameField.clear(); locationField.clear(); phoneField.clear(); emailField.clear();
        activeCheckBox.setSelected(true);
    }

    private void openBranchDetails(Branch selected) {
        BranchDetailController detailCtrl = new BranchDetailController(selected, authService, stage);
        
        Button goBack = new Button("< BACK TO REGISTRY");
        goBack.setStyle("-fx-background-color: #1A1D24; -fx-border-color: #334155; -fx-border-radius: 6; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-padding: 6 16; -fx-cursor: hand;");
        HBox backRow = new HBox(goBack);
        backRow.setPadding(new Insets(10, 0, 10, 28));
        
        goBack.setOnAction(ev -> {
            this.getChildren().setAll(new BranchManagementController(authService, stage).getChildren());
        });
        
        this.getChildren().clear();
        this.getChildren().addAll(backRow, detailCtrl);
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

    private void exportSelectedBranchReport() {
        if (selectedBranch == null) {
            showAlert("No Selection", "Select a branch first to print its full report.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Full Branch Report (PDF)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("branch_" + selectedBranch.getId() + "_full_report.pdf");
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        try {
            List<Product> products = productDAO.findByBranchId(selectedBranch.getId());
            List<User> users = userDAO.findByBranchId(selectedBranch.getId());
            PDFExporter.exportFullBranchReport(selectedBranch, products, users, file.getAbsolutePath());
            showAlert("Success", "Full branch report exported successfully.");
        } catch (Exception ex) {
            showAlert("Error", "Failed to export report: " + ex.getMessage());
        }
    }
}
