package com.smartstock.controller;

import com.smartstock.chart.InventoryFXGLChart;
import com.smartstock.thread.ChartRefreshThread;
import com.smartstock.service.AuthService;
import com.smartstock.util.NavigationHelper;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for the live inventory chart screen (Module 3 - FXGL Live Chart).
 * Displays auto-refreshing bar chart of inventory levels.
 */
public class InventoryChartController extends VBox {

    private final InventoryFXGLChart chart;
    private final ChartRefreshThread refreshThread;
    private final AuthService authService;
    private final Stage stage;

    public InventoryChartController(AuthService authService, Stage stage) {
        this.authService = authService;
        this.stage = stage;
        this.chart = new InventoryFXGLChart();
        this.refreshThread = new ChartRefreshThread(chart);
        
        initializeUI();
        startRefreshThread();
    }

    private void initializeUI() {
        setSpacing(15);
        setPadding(new Insets(20));
        getStyleClass().add("main-container");

        // Header
        Label headerLabel = new Label("Live Inventory Chart");
        headerLabel.getStyleClass().add("header-label");
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Info Label
        Label infoLabel = new Label("Chart auto-refreshes every 10 seconds with real database data");
        infoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        // Chart Container
        VBox chartContainer = new VBox(10);
        chartContainer.setPadding(new Insets(15));
        chartContainer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
        chartContainer.getChildren().add(chart);

        // Back Button
        Button backBtn = new Button("Back to Dashboard");
        backBtn.getStyleClass().add("mfx-button");
        backBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 10 20;");
        backBtn.setOnAction(e -> {
            stopRefreshThread();
            NavigationHelper.goToDashboard(authService, stage);
        });

        getChildren().addAll(headerLabel, infoLabel, chartContainer, backBtn);
    }

    private void startRefreshThread() {
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private void stopRefreshThread() {
        refreshThread.stopThread();
    }
}
