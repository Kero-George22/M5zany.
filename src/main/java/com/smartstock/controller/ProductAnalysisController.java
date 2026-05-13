package com.smartstock.controller;

import com.smartstock.dao.AnalysisDAO;
import com.smartstock.dao.BranchDAO;
import com.smartstock.model.SlowMovingProduct;
import com.smartstock.model.LossMakingProduct;
import com.smartstock.model.Branch;
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

public class ProductAnalysisController extends VBox {

    private ComboBox<Branch> branchCombo;
    private Button refreshBtn;
    private TableView<SlowMovingProduct> slowMovingTable;
    private TableView<LossMakingProduct> lossMakingTable;
    
    private final AnalysisDAO analysisDAO;
    private final BranchDAO branchDAO;
    private final AuthService authService;
    private final Stage stage;

    public ProductAnalysisController(AuthService authService, Stage stage) {
        this.authService = authService;
        this.stage = stage;
        this.analysisDAO = new AnalysisDAO();
        this.branchDAO = new BranchDAO();
        
        initializeUI();
        loadBranches();
    }

    private void initializeUI() {
        setSpacing(15);
        setPadding(new Insets(20));
        getStyleClass().add("main-container");

        // Header
        Label headerLabel = new Label("Product Analysis");
        headerLabel.getStyleClass().add("header-label");
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Filter Section
        HBox filterBox = new HBox(15);
        filterBox.setPadding(new Insets(10));
        filterBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Label branchLabel = new Label("Branch:");
        branchCombo = new ComboBox<>();
        branchCombo.setPromptText("Select branch");
        branchCombo.setPrefWidth(200);

        refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("mfx-button");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 10 20;");
        refreshBtn.setGraphic(new FontIcon("fas-sync"));
        refreshBtn.setOnAction(e -> handleRefresh());

        filterBox.getChildren().addAll(branchLabel, branchCombo, refreshBtn);

        // Slow Moving Products Section
        VBox slowMovingBox = new VBox(10);
        slowMovingBox.setPadding(new Insets(15));
        slowMovingBox.setStyle("-fx-background-color: #fff3e0; -fx-background-radius: 8;");

        Label slowMovingLabel = new Label("Slow-Moving Products (Fewest Sales This Month)");
        slowMovingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e65100;");

        slowMovingTable = new TableView<>();
        slowMovingTable.setPrefHeight(250);

        TableColumn<SlowMovingProduct, String> smProductCol = new TableColumn<>("Product");
        smProductCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        smProductCol.setPrefWidth(150);

        TableColumn<SlowMovingProduct, String> smBranchCol = new TableColumn<>("Branch");
        smBranchCol.setCellValueFactory(new PropertyValueFactory<>("branchName"));
        smBranchCol.setPrefWidth(120);

        TableColumn<SlowMovingProduct, Integer> smQtyCol = new TableColumn<>("Quantity");
        smQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        smQtyCol.setPrefWidth(80);

        TableColumn<SlowMovingProduct, Integer> smSalesCol = new TableColumn<>("Sales Count");
        smSalesCol.setCellValueFactory(new PropertyValueFactory<>("salesCount"));
        smSalesCol.setPrefWidth(100);

        TableColumn<SlowMovingProduct, Integer> smMinStockCol = new TableColumn<>("Min Stock");
        smMinStockCol.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        smMinStockCol.setPrefWidth(80);

        slowMovingTable.getColumns().addAll(smProductCol, smBranchCol, smQtyCol, smSalesCol, smMinStockCol);

        slowMovingBox.getChildren().addAll(slowMovingLabel, slowMovingTable);

        // Loss Making Products Section
        VBox lossMakingBox = new VBox(10);
        lossMakingBox.setPadding(new Insets(15));
        lossMakingBox.setStyle("-fx-background-color: #ffebee; -fx-background-radius: 8;");

        Label lossMakingLabel = new Label("Loss-Making Products (Cost > Selling Price)");
        lossMakingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #c62828;");

        lossMakingTable = new TableView<>();
        lossMakingTable.setPrefHeight(250);

        TableColumn<LossMakingProduct, String> lmProductCol = new TableColumn<>("Product");
        lmProductCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        lmProductCol.setPrefWidth(150);

        TableColumn<LossMakingProduct, String> lmBranchCol = new TableColumn<>("Branch");
        lmBranchCol.setCellValueFactory(new PropertyValueFactory<>("branchName"));
        lmBranchCol.setPrefWidth(120);

        TableColumn<LossMakingProduct, Double> lmCostCol = new TableColumn<>("Unit Cost");
        lmCostCol.setCellValueFactory(new PropertyValueFactory<>("unitCost"));
        lmCostCol.setPrefWidth(100);

        TableColumn<LossMakingProduct, Double> lmPriceCol = new TableColumn<>("Selling Price");
        lmPriceCol.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        lmPriceCol.setPrefWidth(100);

        TableColumn<LossMakingProduct, Double> lmLossCol = new TableColumn<>("Loss/Unit");
        lmLossCol.setCellValueFactory(new PropertyValueFactory<>("lossPerUnit"));
        lmLossCol.setPrefWidth(100);

        TableColumn<LossMakingProduct, Double> lmTotalLossCol = new TableColumn<>("Total Loss");
        lmTotalLossCol.setCellValueFactory(new PropertyValueFactory<>("totalLoss"));
        lmTotalLossCol.setPrefWidth(100);

        lossMakingTable.getColumns().addAll(lmProductCol, lmBranchCol, lmCostCol, lmPriceCol, lmLossCol, lmTotalLossCol);

        lossMakingBox.getChildren().addAll(lossMakingLabel, lossMakingTable);

        // Back Button
        Button backBtn = new Button("Back to Dashboard");
        backBtn.getStyleClass().add("mfx-button");
        backBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 10 20;");
        backBtn.setOnAction(e -> NavigationHelper.goToDashboard(authService, stage));

        getChildren().addAll(headerLabel, filterBox, slowMovingBox, lossMakingBox, backBtn);
    }

    private void loadBranches() {
        List<Branch> branches = branchDAO.findAll();
        branchCombo.setItems(FXCollections.observableArrayList(branches));
    }

    private void handleRefresh() {
        Branch selectedBranch = branchCombo.getValue();
        if (selectedBranch == null) {
            Notifications.create()
                    .title("Validation Error")
                    .text("Please select a branch")
                    .position(Pos.TOP_RIGHT)
                    .hideAfter(Duration.seconds(3))
                    .show();
            return;
        }

        // Load slow-moving products
        List<SlowMovingProduct> slowMoving = analysisDAO.findSlowMovingProducts(selectedBranch.getId(), 10);
        slowMovingTable.setItems(FXCollections.observableArrayList(slowMoving));

        // Load loss-making products
        List<LossMakingProduct> lossMaking = analysisDAO.findLossMakingProducts(selectedBranch.getId());
        lossMakingTable.setItems(FXCollections.observableArrayList(lossMaking));

        Notifications.create()
                .title("Success")
                .text("Analysis refreshed successfully")
                .position(Pos.TOP_RIGHT)
                .hideAfter(Duration.seconds(3))
                .show();
    }
}
