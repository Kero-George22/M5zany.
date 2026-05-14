package com.smartstock.controller;

import com.smartstock.dao.PricingDAO;
import com.smartstock.dao.BranchDAO;
import com.smartstock.model.PricingHistory;
import com.smartstock.model.Branch;
import com.smartstock.service.ClaudeAPIService;
import com.smartstock.service.AuthService;
import com.smartstock.util.NavigationHelper;
import javafx.collections.FXCollections;
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

public class DynamicPricingController extends VBox {

    private TextField productNameField;
    private TextField quantityField;
    private TextField wholesalePriceField;
    private ComboBox<Branch> branchCombo;
    private Button suggestBtn;
    private Label suggestedPriceLabel;
    private TextArea reasoningArea;
    private TableView<PricingHistory> historyTable;
    
    private final PricingDAO pricingDAO;
    private final BranchDAO branchDAO;
    private final ClaudeAPIService claudeService;
    private final AuthService authService;
    private final Stage stage;

    public DynamicPricingController(AuthService authService, Stage stage) {
        this.authService = authService;
        this.stage = stage;
        this.pricingDAO = new PricingDAO();
        this.branchDAO = new BranchDAO();
        this.claudeService = new ClaudeAPIService();
        
        initializeUI();
        loadBranches();
        loadHistory();
    }

    private void initializeUI() {
        setSpacing(15);
        setPadding(new Insets(20));
        getStyleClass().add("main-container");

        // Header
        Label headerLabel = new Label("AI Dynamic Pricing");
        headerLabel.getStyleClass().add("header-label");
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Input Form
        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(15);
        formGrid.setPadding(new Insets(15));
        formGrid.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");

        Label productLabel = new Label("Product Name:");
        productNameField = new TextField();
        productNameField.setPromptText("Enter product name");

        Label quantityLabel = new Label("Quantity:");
        quantityField = new TextField();
        quantityField.setPromptText("Enter quantity");

        Label wholesaleLabel = new Label("Wholesale Price:");
        wholesalePriceField = new TextField();
        wholesalePriceField.setPromptText("Enter wholesale price");

        Label branchLabel = new Label("Branch:");
        branchCombo = new ComboBox<>();
        branchCombo.setPromptText("Select branch");

        suggestBtn = new Button("Get AI Suggestion");
        suggestBtn.getStyleClass().add("mfx-button");
        suggestBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 10 20;");
        suggestBtn.setGraphic(new FontIcon("fas-magic"));
        suggestBtn.setOnAction(e -> handleSuggest());

        formGrid.add(productLabel, 0, 0);
        formGrid.add(productNameField, 1, 0);
        formGrid.add(quantityLabel, 0, 1);
        formGrid.add(quantityField, 1, 1);
        formGrid.add(wholesaleLabel, 0, 2);
        formGrid.add(wholesalePriceField, 1, 2);
        formGrid.add(branchLabel, 0, 3);
        formGrid.add(branchCombo, 1, 3);
        formGrid.add(suggestBtn, 1, 4);

        // Result Section
        VBox resultBox = new VBox(10);
        resultBox.setPadding(new Insets(15));
        resultBox.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 8;");

        Label resultLabel = new Label("AI Suggestion Result:");
        resultLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2e7d32;");

        suggestedPriceLabel = new Label("Suggested Price: -");
        suggestedPriceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;");

        Label reasoningLabel = new Label("Reasoning:");
        reasoningLabel.setStyle("-fx-font-weight: bold;");

        reasoningArea = new TextArea();
        reasoningArea.setPrefHeight(80);
        reasoningArea.setEditable(false);
        reasoningArea.setWrapText(true);

        resultBox.getChildren().addAll(resultLabel, suggestedPriceLabel, reasoningLabel, reasoningArea);

        // History Table
        Label historyLabel = new Label("Pricing History");
        historyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        historyTable = new TableView<>();
        historyTable.setPrefHeight(300);

        TableColumn<PricingHistory, String> productCol = new TableColumn<>("Product");
        productCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productCol.setPrefWidth(150);

        TableColumn<PricingHistory, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(80);

        TableColumn<PricingHistory, Double> wholesaleCol = new TableColumn<>("Wholesale");
        wholesaleCol.setCellValueFactory(new PropertyValueFactory<>("wholesalePrice"));
        wholesaleCol.setPrefWidth(100);

        TableColumn<PricingHistory, Double> suggestedCol = new TableColumn<>("Suggested");
        suggestedCol.setCellValueFactory(new PropertyValueFactory<>("suggestedPrice"));
        suggestedCol.setPrefWidth(100);

        TableColumn<PricingHistory, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("suggestedAt"));
        dateCol.setPrefWidth(150);

        historyTable.getColumns().addAll(productCol, quantityCol, wholesaleCol, suggestedCol, dateCol);

        // Back Button
        Button backBtn = new Button("Back to Dashboard");
        backBtn.getStyleClass().add("mfx-button");
        backBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 10 20;");
        backBtn.setOnAction(e -> NavigationHelper.goToDashboard(authService, stage));

        getChildren().addAll(headerLabel, formGrid, resultBox, historyLabel, historyTable, backBtn);
    }

    private void loadBranches() {
        List<Branch> branches = branchDAO.findAll();
        branchCombo.setItems(FXCollections.observableArrayList(branches));
    }

    private void loadHistory() {
        List<PricingHistory> history = pricingDAO.findAll();
        historyTable.setItems(FXCollections.observableArrayList(history));
    }

    private void handleSuggest() {
        String productName = productNameField.getText().trim();
        String quantityStr = quantityField.getText().trim();
        String wholesaleStr = wholesalePriceField.getText().trim();
        Branch selectedBranch = branchCombo.getValue();

        if (productName.isEmpty() || quantityStr.isEmpty() || wholesaleStr.isEmpty() || selectedBranch == null) {
            Notifications.create()
                    .title("Validation Error")
                    .text("Please fill in all fields")
                    .position(Pos.TOP_RIGHT)
                    .hideAfter(Duration.seconds(3))
                    .show();
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);
            double wholesalePrice = Double.parseDouble(wholesaleStr);

            // Call Claude API
            ClaudeAPIService.PricingSuggestion suggestion = claudeService.suggestRetailPrice(productName, quantity, wholesalePrice);

            suggestedPriceLabel.setText(String.format("Suggested Price: %.2f", suggestion.getSuggestedPrice()));
            reasoningArea.setText(suggestion.getReasoning());

            // Save to database
            PricingHistory history = new PricingHistory();
            history.setProductId(0); // Will be updated if product exists
            history.setBranchId(selectedBranch.getId());
            history.setQuantity(quantity);
            history.setWholesalePrice(wholesalePrice);
            history.setSuggestedPrice(suggestion.getSuggestedPrice());

            pricingDAO.insert(history);
            loadHistory();

            Notifications.create()
                    .title("Success")
                    .text("Pricing suggestion saved successfully")
                    .position(Pos.TOP_RIGHT)
                    .hideAfter(Duration.seconds(3))
                    .show();

        } catch (NumberFormatException e) {
            Notifications.create()
                    .title("Validation Error")
                    .text("Please enter valid numbers for quantity and price")
                    .position(Pos.TOP_RIGHT)
                    .hideAfter(Duration.seconds(3))
                    .show();
        }
    }
}
