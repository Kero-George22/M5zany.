package com.smartstock.chart;

import javafx.scene.layout.Pane;

/**
 * Abstract base class for FXGL charts (Module 3 - FXGL Live Chart).
 * Demonstrates polymorphism with abstract refreshData() method.
 */
public abstract class AbstractChart extends Pane {

    /**
     * Abstract method to refresh chart data.
     * Subclasses must implement this to update their specific chart type.
     */
    public abstract void refreshData();

    /**
     * Abstract method to initialize the chart.
     * Subclasses must implement this to set up their specific chart configuration.
     */
    public abstract void initializeChart();
}
