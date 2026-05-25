package com.smartstock.controller;

import com.smartstock.dao.AlertDAO;
import com.smartstock.model.Alert;
import com.smartstock.service.AuthService;
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

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AlertManagementController extends VBox {

    private TableView<Alert> alertTable;
    private TextArea detailArea;
    private Button markReadBtn;
    private Button deleteBtn;
    private Button markAllReadBtn;
    private Button refreshBtn;
    private Button backBtn;
    private Label statusLabel;

    private AuthService authService;
    private Stage stage;
    private AlertDAO alertDAO;
    private Alert selectedAlert;

    public AlertManagementController(AuthService authService, Stage stage) {
        this.authService = authService;
        this.stage = stage;
        this.alertDAO = new AlertDAO();

        com.smartstock.util.ThemeManager.applyTheme(this);
        setSpacing(0);

        buildHeader();

        HBox mainLayout = new HBox(20);
        mainLayout.setPadding(new Insets(14));
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        VBox listSection = new VBox(12);
        HBox.setHgrow(listSection, Priority.ALWAYS);
        buildAlertTable(listSection);

        VBox detailSection = new VBox(12);
        detailSection.setMinWidth(400);
        detailSection.setMaxWidth(400);
        buildDetailArea(detailSection);
        buildButtons(detailSection);

        mainLayout.getChildren().addAll(listSection, detailSection);
        getChildren().add(mainLayout);

        loadAlerts();
    }

    private void buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");
        header.setPadding(new Insets(0, 0, 10, 0));

        Label iconLbl = new Label();
        iconLbl.setGraphic(new FontIcon("mdi2b-bell-ring-outline"));
        iconLbl.setStyle("-fx-background-color: rgba(99,102,241,0.1); -fx-border-color: #4F46E5; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6; -fx-text-fill: #818CF8;");
        
        String title = authService.isAdmin() ? "GLOBAL ALERT MANAGEMENT" : "BRANCH ALERTS";
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-letter-spacing: 1px;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        refreshBtn = new Button("REFRESH");
        refreshBtn.setGraphic(new FontIcon("mdi2r-refresh"));
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #818CF8; -fx-border-color: #334155; -fx-border-radius: 6; -fx-padding: 6 16; -fx-font-weight: bold;");
        refreshBtn.setOnAction(e -> loadAlerts());

        markAllReadBtn = new Button("MARK ALL READ");
        markAllReadBtn.setGraphic(new FontIcon("mdi2c-check-all"));
        markAllReadBtn.setStyle("-fx-background-color: rgba(16, 185, 129, 0.1); -fx-text-fill: #10B981; -fx-border-color: rgba(16, 185, 129, 0.2); -fx-border-radius: 6; -fx-padding: 6 16; -fx-font-weight: bold;");
        markAllReadBtn.setOnAction(e -> markAllRead());

        backBtn = new Button("< BACK");
        backBtn.setStyle("-fx-background-color: #1A1D24; -fx-border-color: #334155; -fx-border-radius: 6; -fx-text-fill: #94A3B8; -fx-font-weight: bold; -fx-padding: 6 16;");
        backBtn.setOnAction(e -> NavigationHelper.goToDashboard(authService, stage));

        header.getChildren().addAll(iconLbl, titleLbl, spacer, statusLabel, refreshBtn, markAllReadBtn, backBtn);
        getChildren().add(header);
    }

    private void buildAlertTable(VBox parent) {
        Label lbl = new Label(authService.isAdmin()
                ? "SYSTEM NOTIFICATIONS LOG"
                : "BRANCH NOTIFICATIONS LOG");
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-letter-spacing: 1.5px;");

        alertTable = new TableView<>();
        VBox.setVgrow(alertTable, Priority.ALWAYS);
        alertTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        alertTable.setPlaceholder(new Label("No alerts found in registry"));

        TableColumn<Alert, String> typeCol = new TableColumn<>("TYPE");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setMaxWidth(90);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); }
            }
        });

        TableColumn<Alert, String> fromCol = new TableColumn<>("ORIGIN");
        fromCol.setCellValueFactory(new PropertyValueFactory<>("senderName"));
        fromCol.setMaxWidth(130);
        fromCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: #94A3B8;"); }
            }
        });

        TableColumn<Alert, String> messageCol = new TableColumn<>("PAYLOAD MESSAGE");
        messageCol.setCellValueFactory(new PropertyValueFactory<>("message"));
        messageCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: #E2E8F0;"); }
            }
        });

        TableColumn<Alert, String> severityCol = new TableColumn<>("SEVERITY");
        severityCol.setCellValueFactory(new PropertyValueFactory<>("severity"));
        severityCol.setMaxWidth(90);
        severityCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "CRITICAL" -> setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 10px; -fx-letter-spacing: 1px;");
                    case "WARNING"  -> setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-font-size: 10px; -fx-letter-spacing: 1px;");
                    default         -> setStyle("-fx-text-fill: #818CF8; -fx-font-weight: bold; -fx-font-size: 10px; -fx-letter-spacing: 1px;");
                }
            }
        });

        TableColumn<Alert, String> dateCol = new TableColumn<>("TIMESTAMP");
        dateCol.setMaxWidth(140);
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item); setStyle("-fx-text-fill: #64748B; -fx-font-family: monospace;"); }
            }
        });
        dateCol.setCellValueFactory(cd -> {
            Alert a = cd.getValue();
            if (a == null || a.getCreatedAt() == null) return new javafx.beans.property.SimpleStringProperty("-");
            return new javafx.beans.property.SimpleStringProperty(
                    a.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        });

        TableColumn<Alert, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setMaxWidth(90);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(item);
                if (item.contains("Read") || item.contains("READ")) {
                    badge.setStyle("-fx-font-weight: bold; -fx-font-size: 9px; -fx-padding: 3 8; -fx-background-radius: 4; -fx-background-color: rgba(16,185,129,0.1); -fx-text-fill: #10B981;");
                } else {
                    badge.setStyle("-fx-font-weight: bold; -fx-font-size: 9px; -fx-padding: 3 8; -fx-background-radius: 4; -fx-background-color: rgba(99,102,241,0.1); -fx-text-fill: #818CF8;");
                }
                setGraphic(badge);
                setText(null);
            }
        });
        statusCol.setCellValueFactory(cd -> {
            Alert a = cd.getValue();
            if (a == null) return new javafx.beans.property.SimpleStringProperty("");
            return new javafx.beans.property.SimpleStringProperty(a.isRead() ? "✓ READ" : "● UNREAD");
        });

        alertTable.getColumns().addAll(typeCol, fromCol, messageCol, severityCol, dateCol, statusCol);

        alertTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            selectedAlert = sel;
            if (sel != null) {
                String info = "Type: " + sel.getType() + " | Severity: " + sel.getSeverity()
                        + (sel.getSenderName() != null ? " | From: " + sel.getSenderName() : "")
                        + "\n\n" + (sel.getMessage() != null ? sel.getMessage() : "(no message)");
                detailArea.setText(info);
            }
        });

        VBox card = new VBox(8, lbl, alertTable);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);
        parent.getChildren().add(card);
    }

    private void buildDetailArea(VBox parent) {
        Label lbl = new Label("\uD83D\uDCC4  ALERT DETAIL");
        lbl.setStyle("-fx-text-fill: #6366F1; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");

        detailArea = new TextArea();
        detailArea.setPrefHeight(150);
        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setPromptText("Select a notification to inspect payload details...");
        detailArea.setStyle("-fx-background-color: transparent; -fx-border-color: #334155; -fx-border-radius: 6; -fx-text-fill: #E2E8F0; -fx-font-family: monospace; -fx-padding: 8;");

        markReadBtn = new Button("✓ Mark as Read");
        markReadBtn.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(16, 185, 129, 0.4); -fx-text-fill: #10B981; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 8; -fx-border-radius: 8;");
        markReadBtn.setOnAction(e -> markAsRead());
        
        deleteBtn = new Button("\uD83D\uDDD1 Delete");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-border-color: rgba(239, 68, 68, 0.4); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 20; -fx-font-weight: bold;");
        deleteBtn.setOnAction(e -> deleteAlert());
        
        HBox btnBox = new HBox(12, markReadBtn, deleteBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        VBox card = new VBox(12, lbl, detailArea, btnBox);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("card");
        parent.getChildren().add(card);
    }

    private void buildButtons(VBox parent) {
        // Buttons are moved inside detail area builder
    }

    private void loadAlerts() {
        try {
            List<Alert> allAlerts;
            Integer branchId = authService.getCurrentUser() != null
                    ? authService.getCurrentUser().getBranchId() : null;

            System.out.println("[AlertMgmt] isAdmin=" + authService.isAdmin() + " branchId=" + branchId);

            if (authService.isAdmin()) {
                allAlerts = alertDAO.findAll();
                System.out.println("[AlertMgmt] findAll returned " + allAlerts.size() + " alerts");
            } else if (branchId != null) {
                allAlerts = alertDAO.findForBranch(branchId);
                System.out.println("[AlertMgmt] findForBranch(" + branchId + ") returned " + allAlerts.size());
            } else {
                allAlerts = List.of();
            }

            ObservableList<Alert> data = FXCollections.observableArrayList(allAlerts);
            alertTable.setItems(data);

            long unread = allAlerts.stream().filter(a -> !a.isRead()).count();
            statusLabel.setText(allAlerts.size() + " alerts  |  " + unread + " unread");
            statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " +
                    (unread > 0 ? "#EF4444" : "#10B981") + ";");

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error loading alerts: " + e.getMessage());
        }
    }

    private void markAsRead() {
        if (selectedAlert == null) {
            showInfo("No Selection", "Please select an alert first.");
            return;
        }
        alertDAO.markAsRead(selectedAlert.getId());
        loadAlerts();
        detailArea.clear();
        selectedAlert = null;
    }

    private void deleteAlert() {
        if (selectedAlert == null) {
            showInfo("No Selection", "Please select an alert first.");
            return;
        }
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION, "Delete selected alert?");
        confirm.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) {
            alertDAO.delete(selectedAlert.getId());
            loadAlerts();
            detailArea.clear();
            selectedAlert = null;
        }});
    }

    private void markAllRead() {
        alertDAO.markAllRead();
        loadAlerts();
        detailArea.clear();
        selectedAlert = null;
        showInfo("Done", "All alerts marked as read.");
    }

    private void showInfo(String title, String msg) {
        FontIcon icon = new FontIcon("mdi2i-information-outline");
        icon.setIconSize(32);
        icon.setIconColor(javafx.scene.paint.Color.web("#6366F1"));

        Notifications.create()
            .title(title)
            .text(msg)
            .graphic(icon)
            .position(Pos.CENTER)
            .hideAfter(Duration.seconds(4))
            .show();
    }
}
