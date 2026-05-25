package com.smartstock.controller;

import com.smartstock.model.Branch;
import com.smartstock.model.User;
import com.smartstock.service.AuthService;
import com.smartstock.service.BranchService;
import com.smartstock.service.UserService;
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

public class UserManagementController extends VBox {

    private TableView<User> userTable;
    private TableColumn<User, Integer> idCol;
    private TableColumn<User, String>  usernameCol, fullNameCol, roleCol;
    private TableColumn<User, Integer> branchCol;
    private TableColumn<User, Boolean> activeCol;

    private TextField usernameField, fullNameField, emailField, phoneField;
    private PasswordField passwordField;
    private ComboBox<String>  roleCombo;
    private ComboBox<Branch>  branchCombo;
    private CheckBox activeCheckBox;
    private Button saveBtn, deleteBtn, clearBtn, resetPasswordBtn, backBtn;

    private final AuthService authService;
    private final Stage stage;
    private final UserService userService;
    private final BranchService branchService;
    private User selectedUser;
    private final boolean isManager;

    public UserManagementController(AuthService authService, Stage stage) {
        this.authService  = authService;
        this.stage        = stage;
        this.userService  = new UserService(authService);
        this.branchService = new BranchService();
        this.isManager    = authService.getCurrentUser() != null && authService.getCurrentUser().isManager();

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

        List<Branch> branches = branchService.getAllBranches();
        branchCombo.setItems(FXCollections.observableArrayList(branches));

        if (isManager) {
            roleCombo.setItems(FXCollections.observableArrayList("CASHIER"));
            roleCombo.setValue("CASHIER");
            roleCombo.setDisable(true);
            User current = authService.getCurrentUser();
            branches.stream().filter(b -> b.getId() == current.getBranchId()).findFirst().ifPresent(branchCombo::setValue);
            branchCombo.setDisable(true);
        } else {
            roleCombo.setItems(FXCollections.observableArrayList("ADMIN", "MANAGER", "CASHIER"));
        }

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

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                selectedUser = sel;
                usernameField.setText(sel.getUsername());
                fullNameField.setText(sel.getFullName());
                emailField.setText(sel.getEmail());
                phoneField.setText(sel.getPhone());
                roleCombo.setValue(sel.getRole());
                activeCheckBox.setSelected(sel.isActive());
                passwordField.clear();
                if (sel.getBranchId() != null)
                    branchCombo.getItems().stream().filter(b -> b.getId() == sel.getBranchId()).findFirst().ifPresent(branchCombo::setValue);
            }
        });

        saveBtn.setOnAction(e -> saveUser());
        deleteBtn.setOnAction(e -> deleteUser());
        clearBtn.setOnAction(e -> clearForm());
        resetPasswordBtn.setOnAction(e -> resetPassword());
        backBtn.setOnAction(e -> NavigationHelper.goToDashboard(authService, stage));

        loadUsers();
    }

    private void buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");
        header.setPadding(new Insets(0, 0, 10, 0));

        Label iconLbl = new Label();
        iconLbl.setGraphic(new FontIcon("mdi2a-account-outline"));
        iconLbl.setStyle("-fx-background-color: rgba(99,102,241,0.1); -fx-border-color: #4F46E5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6; -fx-text-fill: #818CF8;");
        
        Label title = new Label("USER MANAGEMENT");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-letter-spacing: 1px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        backBtn = new Button("< BACK");
        backBtn.setStyle("-fx-background-color: #1A1D24; -fx-border-color: #334155; -fx-border-radius: 6; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-padding: 6 16;");

        header.getChildren().addAll(iconLbl, title, spacer, backBtn);
        getChildren().add(header);
    }

    private void buildTableCard(VBox parent) {
        Label lbl = new Label("USER REGISTRY");
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-letter-spacing: 1.5px;");

        TextField searchBox = new TextField();
        searchBox.setPromptText("🔍 Search database...");
        searchBox.setStyle("-fx-background-color: #1A1D24; -fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white; -fx-padding: 6 12;");
        searchBox.setPrefWidth(250);

        HBox tableHeader = new HBox(lbl);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        tableHeader.getChildren().addAll(sp, searchBox);
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setPadding(new Insets(0, 0, 10, 0));

        userTable = new TableView<>();
        userTable.setPrefHeight(300);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        idCol = new TableColumn<>("ID"); idCol.setMaxWidth(60);
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText("#" + item); setStyle("-fx-text-fill: #94A3B8;"); }
            }
        });

        TableColumn<User, String> identCol = new TableColumn<>("NAME");
        identCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        identCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { 
                    setText(item); 
                    setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 13px;"); 
                    setGraphic(null);
                }
            }
        });

        roleCol = new TableColumn<>("ACCESS ROLE");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label badge = new Label(item);
                    badge.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 4;");
                    if (item.equals("ADMIN")) badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(99,102,241,0.1); -fx-text-fill: #818CF8;");
                    else badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(148,163,184,0.1); -fx-text-fill: #94A3B8;");
                    setGraphic(badge);
                }
            }
        });

        branchCol = new TableColumn<>("BRANCH");
        branchCol.setCellValueFactory(new PropertyValueFactory<>("branchId"));
        branchCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setStyle(""); }
                else if (item == null) { setText("SYSTEM"); setStyle("-fx-text-fill: #64748B;"); }
                else { 
                    Branch b = branchService.getBranchById(item);
                    setText(b != null ? b.getName() : "Unknown"); 
                    setStyle("-fx-text-fill: #94A3B8;"); 
                }
            }
        });

        activeCol = new TableColumn<>("ACTIVITY");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    HBox box = new HBox(6);
                    box.setAlignment(Pos.CENTER_LEFT);
                    javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(3);
                    dot.setFill(javafx.scene.paint.Color.web(item ? "#10B981" : "#EF4444"));
                    Label lbl = new Label(item ? "ACTIVE" : "INACTIVE");
                    lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (item ? "#10B981" : "#EF4444") + "; -fx-font-size: 11px;");
                    box.getChildren().addAll(dot, lbl);
                    setGraphic(box);
                }
            }
        });
        
        userTable.getColumns().addAll(idCol, identCol, roleCol, branchCol, activeCol);

        VBox card = new VBox(tableHeader, userTable);
        parent.getChildren().add(card);
    }

    private void buildFormCard(VBox parent) {
        usernameField  = field("Enter username");
        passwordField  = new PasswordField(); passwordField.setPromptText("Enter password");
        fullNameField  = field("Enter full name");
        emailField     = field("user@example.com");
        phoneField     = field("Enter contact phone");
        roleCombo      = new ComboBox<>(); roleCombo.setPromptText("Role");
        branchCombo    = new ComboBox<>(); branchCombo.setPromptText("Branch");
        
        // Column 1: IDENTITY
        Label idTitle = new Label("— IDENTITY");
        idTitle.setStyle("-fx-text-fill: #6366F1; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");
        VBox col1 = new VBox(16, idTitle, labeled("USERNAME", usernameField), labeled("FULL NAME", fullNameField), labeled("EMAIL ADDRESS", emailField));
        col1.setPrefWidth(300);

        // Column 2: ACCESS
        Label accessTitle = new Label("— ACCESS");
        accessTitle.setStyle("-fx-text-fill: #6366F1; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");
        VBox col2 = new VBox(16, accessTitle, labeled("PASSWORD", passwordField), labeled("CONTACT PHONE", phoneField), labeled("USER ROLE", roleCombo));
        col2.setPrefWidth(300);

        // Column 3: CONTEXT
        Label contextTitle = new Label("— CONTEXT");
        contextTitle.setStyle("-fx-text-fill: #6366F1; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");

        HBox activeCard = new HBox(12);
        activeCard.setAlignment(Pos.CENTER_LEFT);
        activeCard.setStyle("-fx-background-color: #11141A; -fx-border-color: #2A2F3A; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 14;");
        activeCheckBox = new CheckBox(); activeCheckBox.setSelected(true);
        VBox activeText = new VBox(2);
        Label actMain = new Label("ACCOUNT ACTIVE"); actMain.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 12px;");
        Label actSub = new Label("Grant visibility in directories"); actSub.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px; -fx-font-style: italic;");
        activeText.getChildren().addAll(actMain, actSub);
        activeCard.getChildren().addAll(activeCheckBox, activeText);

        VBox col3 = new VBox(16, contextTitle, labeled("ASSIGNED BRANCH", branchCombo), activeCard);
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

        resetPasswordBtn = new Button("RESET SECURITY");
        resetPasswordBtn.setGraphic(new FontIcon("mdi2k-key-outline"));
        resetPasswordBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #818CF8; -fx-border-color: #3730A3; -fx-border-radius: 8; -fx-padding: 10 24; -fx-font-weight: bold;");

        clearBtn = new Button("CLEAR");
        clearBtn.setGraphic(new FontIcon("mdi2e-eraser"));
        clearBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-border-color: #334155; -fx-border-radius: 8; -fx-padding: 10 24; -fx-font-weight: bold;");

        HBox btnRow = new HBox(16, saveBtn, deleteBtn, resetPasswordBtn, clearBtn);
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

    private void loadUsers() {
        try {
            List<User> allUsers = userService.getAllUsers();
            if (isManager) {
                Integer branchId = authService.getCurrentUser().getBranchId();
                allUsers = allUsers.stream()
                        .filter(u -> u.getBranchId() != null && u.getBranchId().equals(branchId))
                        .filter(u -> "CASHIER".equals(u.getRole()))
                        .toList();
            }
            userTable.setItems(FXCollections.observableArrayList(allUsers));
        } catch (SecurityException e) { showAlert("Access Denied", e.getMessage()); }
    }

    private void saveUser() {
        String username = usernameField.getText().trim(), fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim(), phone = phoneField.getText().trim();
        String password = passwordField.getText();
        String role = isManager ? "CASHIER" : roleCombo.getValue();
        Branch branch = branchCombo.getValue();

        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || role == null || branch == null) {
            showAlert("Validation", "All fields are required."); return;
        }
        if (!email.endsWith("@gmail.com")) {
            showAlert("Validation", "Email must be a @gmail.com address."); return;
        }
        if (!phone.matches("\\d{11}")) {
            showAlert("Validation", "Phone number must be exactly 11 digits."); return;
        }
        try {
            if (selectedUser != null) {
                selectedUser.setUsername(username); selectedUser.setFullName(fullName); selectedUser.setEmail(email);
                selectedUser.setPhone(phone); selectedUser.setRole(role); selectedUser.setActive(activeCheckBox.isSelected());
                selectedUser.setBranchId(branch != null ? branch.getId() : null);
                userService.updateUser(selectedUser); showAlert("Success", "User updated.");
            } else {
                if (password.isEmpty()) { showAlert("Validation", "Password required for new users."); return; }
                Integer branchId = isManager ? authService.getCurrentUser().getBranchId()
                        : (branch != null ? branch.getId() : null);
                User u = userService.createUser(username, password, fullName, email, phone, role, branchId);
                showAlert(u != null ? "Success" : "Error", u != null ? "User created." : "Failed to create user.");
            }
            clearForm(); loadUsers();
        } catch (SecurityException e) { showAlert("Access Denied", e.getMessage()); }
    }

    private void deleteUser() {
        if (selectedUser == null) { showAlert("No Selection", "Select a user to delete."); return; }
        if (isManager && (!"CASHIER".equals(selectedUser.getRole()) ||
                !authService.getCurrentUser().getBranchId().equals(selectedUser.getBranchId()))) {
            showAlert("Access Denied", "You can only delete CASHIER users from your branch."); return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete user " + selectedUser.getUsername() + "?");
        confirm.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) {
            try { userService.deleteUser(selectedUser.getId()); clearForm(); loadUsers(); }
            catch (SecurityException e) { showAlert("Access Denied", e.getMessage()); }
        }});
    }

    private void resetPassword() {
        if (selectedUser == null) { showAlert("No Selection", "Select a user first."); return; }
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Reset Password"); dlg.setHeaderText("New password for " + selectedUser.getUsername()); dlg.setContentText("New password:");
        dlg.showAndWait().ifPresent(p -> { if (!p.isEmpty()) {
            try { userService.resetPassword(selectedUser.getId(), p); showAlert("Success", "Password reset."); }
            catch (SecurityException e) { showAlert("Access Denied", e.getMessage()); }
        }});
    }

    private void clearForm() {
        selectedUser = null;
        usernameField.clear(); passwordField.clear(); fullNameField.clear(); emailField.clear(); phoneField.clear();
        if (isManager) { roleCombo.setValue("CASHIER"); }
        else { roleCombo.setValue(null); branchCombo.setValue(null); }
        activeCheckBox.setSelected(true);
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
