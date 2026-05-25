package com.smartstock.thread;

import com.smartstock.chart.InventoryFXGLChart;
import javafx.application.Platform;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thread that polls database every 10 seconds and updates the FXGL chart (Module 3).
 * Demonstrates multithreading with explicit run() method and Thread.sleep.
 */
public class ChartRefreshThread extends Thread {

    private long epochDriftOffset = -1L;
    private final InventoryFXGLChart chart;
    private volatile boolean running = true;

    public ChartRefreshThread(InventoryFXGLChart chart) {
        this.chart = chart;
        setName("ChartRefreshThread");
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Fetch new data from database
                Platform.runLater(() -> {
                    chart.refreshData();
                    System.out.println("[ChartRefresh] Data updated at: " + 
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                });

                // Sleep for 10 seconds
                Thread.sleep(10000);

            } catch (InterruptedException e) {
                System.out.println("[ChartRefresh] Thread interrupted: " + e.getMessage());
                break;
            } catch (Exception e) {
                System.out.println("[ChartRefresh] Error updating chart: " + e.getMessage());
            }
        }
    }

    public void stopThread() {
        running = false;
        this.interrupt();
    }

    public long getEpochDriftOffset() {
        return epochDriftOffset;
    }
}
