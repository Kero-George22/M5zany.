package com.smartstock;

import com.smartstock.controller.LoginController;
import com.smartstock.service.AuthService;
import com.smartstock.thread.WeeklyReportTask;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private AuthService authService;
    private Thread weeklyReportThread;

    @Override
    public void init() {
        this.authService = new AuthService();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            LoginController loginController = new LoginController(authService, primaryStage);

            Scene scene = new Scene(loginController, 480, 620);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            com.smartstock.util.ThemeManager.applyTheme(scene);

            primaryStage.setTitle("M5zany — Login");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(480);
            primaryStage.setMinHeight(550);
            primaryStage.setOnCloseRequest(e -> {
                shutdown();
                Platform.exit();
            });
            primaryStage.show();

            startBackgroundThreads();

        } catch (Exception e) {
            e.printStackTrace();
            Platform.exit();
        }
    }

    private void startBackgroundThreads() {
        WeeklyReportTask weeklyTask = new WeeklyReportTask();
        weeklyReportThread = new Thread(weeklyTask);
        weeklyReportThread.setDaemon(true);
        weeklyReportThread.setName("weekly-report-thread");
        weeklyReportThread.start();
    }

    private void shutdown() {
        if (weeklyReportThread != null && weeklyReportThread.isAlive()) {
            weeklyReportThread.interrupt();
        }
        com.smartstock.dao.DatabaseConnection.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
