package com.smartstock.controller;

import com.smartstock.model.Branch;
import com.smartstock.service.AuthService;
import com.smartstock.service.BranchService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.controlsfx.control.Notifications;

import java.io.IOException;
import java.util.List;

public class BranchesController extends VBox {

    private final AuthService authService;
    private final Stage stage;
    private final BranchService branchService;
    private FlowPane cardFlowPane;

    public BranchesController(AuthService authService, Stage stage) {
        this.authService = authService;
        this.stage = stage;
        this.branchService = new BranchService();

        com.smartstock.util.ThemeManager.applyTheme(this);
        setSpacing(20);
        setPadding(new Insets(24));
        VBox.setVgrow(this, Priority.ALWAYS);

        buildHeader();
        
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        cardFlowPane = new FlowPane();
        cardFlowPane.setHgap(20);
        cardFlowPane.setVgap(20);
        cardFlowPane.setPadding(new Insets(10));
        cardFlowPane.setStyle("-fx-background-color: transparent;");

        scroll.setContent(cardFlowPane);
        getChildren().add(scroll);

        loadBranches();
    }

    private void buildHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("BRANCHES OVERVIEW");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -text-primary; -fx-letter-spacing: 1px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ ADD BRANCH");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> showAddBranchDialog());

        header.getChildren().addAll(title, spacer, addBtn);
        getChildren().add(header);
    }

    private void loadBranches() {
        cardFlowPane.getChildren().clear();
        List<Branch> branches = branchService.getAllBranches();
        for (Branch b : branches) {
            cardFlowPane.getChildren().add(createBranchCard(b));
        }
    }

    private VBox createBranchCard(Branch branch) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "branch-card"); // Requirement 3: Add CSS class
        card.setPrefWidth(280);
        card.setPadding(new Insets(20));
        
        // Requirement 3: Hand cursor
        card.setStyle("-fx-background-color: -card-bg; -fx-border-color: -card-border; -fx-border-radius: 12; -fx-background-radius: 12; -fx-cursor: hand;");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        StackPane iconBg = new StackPane();
        iconBg.setStyle("-fx-background-color: rgba(99,102,241,0.1); -fx-background-radius: 8;");
        iconBg.setPrefSize(36, 36);
        FontIcon branchIcon = new FontIcon("mdi2s-storefront");
        branchIcon.setIconSize(18);
        branchIcon.setIconColor(Color.web("#6366F1"));
        iconBg.getChildren().add(branchIcon);

        VBox titleBox = new VBox(2);
        Label nameLbl = new Label(branch.getName());
        nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");
        Label locLbl = new Label(branch.getLocation());
        locLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-secondary;");
        titleBox.getChildren().addAll(nameLbl, locLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button();
        deleteBtn.setGraphic(new FontIcon("mdi2t-trash-can-outline"));
        deleteBtn.getStyleClass().add("action-btn-danger");
        deleteBtn.setOnAction(e -> {
            e.consume(); // Ensure navigation doesn't trigger
            deleteBranch(branch);
        });

        topRow.getChildren().addAll(iconBg, titleBox, spacer, deleteBtn);

        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.1;");

        VBox statsBox = new VBox(8);
        statsBox.getChildren().addAll(
            statRow("Products", String.valueOf(branch.getProductCount()), "#10B981"),
            statRow("Status", branch.isActive() ? "Active" : "Inactive", branch.isActive() ? "#10B981" : "#EF4444")
        );

        card.getChildren().addAll(topRow, sep, statsBox);
        
        // Requirement 1: Inject Mouse Event
        card.setOnMouseClicked(event -> handleBranchClick(branch));

        return card;
    }

    // Requirement 2: The Navigation Method
    private void handleBranchClick(Branch branch) {
        // Requirement 4: Debugging Check
        System.out.println("Branch clicked: " + branch.getName());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/FinancialTracking.fxml"));
            VBox view = loader.load();
            
            // Crucial: Pass the branch object to the controller before showing
            FinancialTrackingController controller = loader.getController();
            controller.initData(branch); // Requirement 2 & 5: Using initData
            
            // Get AdminDashboardController to swap content
            AdminDashboardController dashboard = (AdminDashboardController) getScene().getRoot();
            dashboard.openPage(view, "Financial Tracking: " + branch.getName());
            
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Failed to load FinancialTracking.fxml: " + ex.getMessage());
        }
    }

    private HBox statRow(String label, String value, String color) {
        HBox row = new HBox();
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: -text-secondary; -fx-font-size: 12px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }

    private void showAddBranchDialog() {
        Dialog<Branch> dialog = new Dialog<>();
        dialog.setTitle("Add New Branch");
        dialog.setHeaderText("Register a new branch in the monitoring system.");

        ButtonType saveButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField name = new TextField();
        name.setPromptText("Branch Name");
        TextField location = new TextField();
        location.setPromptText("Location");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Location:"), 0, 1);
        grid.add(location, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return branchService.createBranch(name.getText(), location.getText(), "N/A", "branch@m5zany.com");
            }
            return null;
        });

        dialog.showAndWait().ifPresent(branch -> {
            if (branch != null) {
                loadBranches();
                showNotification("Success", "New branch added to monitoring.", "mdi2c-check", "#10B981");
            }
        });
    }

    private void deleteBranch(Branch branch) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete monitoring for " + branch.getName() + "?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                branchService.deleteBranch(branch.getId());
                loadBranches();
                showNotification("Deleted", "Branch removed from monitoring.", "mdi2t-trash-can", "#EF4444");
            }
        });
    }

    private void showNotification(String title, String msg, String iconCode, String color) {
        Notifications.create()
            .title(title)
            .text(msg)
            .graphic(new FontIcon(iconCode))
            .position(Pos.TOP_RIGHT)
            .show();
    }
}
