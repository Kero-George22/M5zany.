package com.smartstock.util;

import com.smartstock.controller.AdminDashboardController;
import com.smartstock.controller.CashierDashboardController;
import com.smartstock.controller.ManagerDashboardController;
import com.smartstock.model.User;
import com.smartstock.service.AuthService;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavigationHelper {

    public static void goToDashboard(AuthService authService, Stage stage) {
        try {
            User user = authService.getCurrentUser();
            if (user == null) return;

            if (user.isAdmin()) {
                AdminDashboardController ctrl = new AdminDashboardController(authService, stage);
                ctrl.loadDashboardData();
                Scene scene = new Scene(ctrl, 1200, 800);
                scene.getStylesheets().add(NavigationHelper.class.getResource("/styles/main.css").toExternalForm());
                com.smartstock.util.ThemeManager.applyTheme(scene);
                stage.setScene(scene);
                stage.setTitle("M5zany — Admin Dashboard");

            } else if (user.isManager()) {
                ManagerDashboardController ctrl = new ManagerDashboardController(authService, stage);
                ctrl.loadDashboardData();
                Scene scene = new Scene(ctrl, 1200, 800);
                scene.getStylesheets().add(NavigationHelper.class.getResource("/styles/main.css").toExternalForm());
                com.smartstock.util.ThemeManager.applyTheme(scene);
                stage.setScene(scene);
                stage.setTitle("M5zany — Manager Dashboard");

            } else {
                CashierDashboardController ctrl = new CashierDashboardController(authService, stage);
                Scene scene = new Scene(ctrl, 1100, 750);
                scene.getStylesheets().add(NavigationHelper.class.getResource("/styles/main.css").toExternalForm());
                com.smartstock.util.ThemeManager.applyTheme(scene);
                stage.setScene(scene);
                stage.setTitle("M5zany — Cashier POS");
            }

            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
