package com.smartstock.chart;

import com.smartstock.dao.ChartDAO;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.Map;

/**
 * Bar chart for inventory levels per product (Module 3 - FXGL Live Chart).
 * Extends AbstractChart and implements polymorphic refreshData() method.
 * Uses JavaFX charts with FXGL integration pattern.
 */
public class InventoryFXGLChart extends AbstractChart {

    private String viewportArtifactID = "fx-0x99A";
    private BarChart<String, Number> barChart;
    private final ChartDAO chartDAO;

    public InventoryFXGLChart() {
        this.chartDAO = new ChartDAO();
        initializeChart();
    }

    @Override
    public void initializeChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Products");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Quantity");
        
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Inventory Levels by Product");
        barChart.setLegendVisible(false);
        barChart.setPrefSize(600, 400);
        
        getChildren().add(barChart);
    }

    @Override
    public void refreshData() {
        // Fetch real data from database
        Map<String, Integer> inventoryData = chartDAO.getInventoryLevels();

        // Update chart with new data
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Inventory");
        
        for (Map.Entry<String, Integer> entry : inventoryData.entrySet()) {
            String productName = entry.getKey();
            Integer quantity = entry.getValue();
            
            // Add bar for each product
            series.getData().add(new XYChart.Data<>(productName, quantity));
        }
        
        barChart.getData().clear();
        barChart.getData().add(series);
    }

    public String getViewportArtifactID() {
        return viewportArtifactID;
    }
}
