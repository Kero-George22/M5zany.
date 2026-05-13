package com.smartstock.controller;

import com.smartstock.model.User;
import com.smartstock.service.AuthService;
import com.smartstock.util.NavigationHelper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXButton;
import org.kordamp.ikonli.javafx.FontIcon;

public class LoginController extends VBox {

    private MFXTextField usernameField;
    private MFXPasswordField passwordField;
    private MFXButton loginButton;
    private Label errorLabel;

    private final AuthService authService;
    private final Stage stage;

    public LoginController(AuthService authService, Stage stage) {
        this.authService = authService;
        this.stage = stage;

        setAlignment(Pos.CENTER);
        com.smartstock.util.ThemeManager.applyTheme(this);
        VBox.setVgrow(this, Priority.ALWAYS);

        // ── Card ──────────────────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setAlignment(Pos.TOP_CENTER);
        card.setMaxWidth(400);
        card.getStyleClass().add("card");

        // ── Logo strip ────────────────────────────────────────────────────────
        VBox logoStrip = new VBox(6);
        logoStrip.setAlignment(Pos.CENTER);
        logoStrip.setPadding(new Insets(36, 32, 24, 32));
        logoStrip.setStyle("-fx-background-color: -table-header-bg; -fx-background-radius: 13 13 0 0;");

        Label logoIcon = new Label("📦");
        logoIcon.setStyle("-fx-font-size: 40px;");

        Label titleLabel = new Label("M5zany ERP");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -text-primary;");

        Label subtitleLabel = new Label("Inventory & Operations Platform");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");

        logoStrip.getChildren().addAll(logoIcon, titleLabel, subtitleLabel);

        // ── Form body ─────────────────────────────────────────────────────────
        VBox body = new VBox(14);
        body.setPadding(new Insets(28, 32, 32, 32));

        Label signInLbl = new Label("Sign in to your account");
        signInLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -text-secondary;");
        signInLbl.setPadding(new Insets(0, 0, 10, 0));

        usernameField = new MFXTextField();
        usernameField.setPromptText("Enter username");
        usernameField.setFloatMode(io.github.palexdev.materialfx.enums.FloatMode.BORDER);
        usernameField.setMaxWidth(Double.MAX_VALUE);
        VBox userBox = new VBox(usernameField);

        passwordField = new MFXPasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setFloatMode(io.github.palexdev.materialfx.enums.FloatMode.BORDER);
        passwordField.setMaxWidth(Double.MAX_VALUE);
        VBox passBox = new VBox(passwordField);

        loginButton = new MFXButton("Sign In");
        loginButton.setGraphic(new FontIcon("mdi2l-login"));
        loginButton.getStyleClass().addAll("button", "btn-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle("-fx-font-size: 14px; -fx-padding: 11 0; -fx-background-color: -accent-color; -fx-text-fill: white;");

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        MFXButton themeBtn = new MFXButton(com.smartstock.util.ThemeManager.isDarkMode() ? "☀️ Light" : "🌙 Dark");
        themeBtn.getStyleClass().add("theme-toggle-btn");
        themeBtn.setOnAction(e -> {
            com.smartstock.util.ThemeManager.toggleTheme(this.getScene());
            themeBtn.setText(com.smartstock.util.ThemeManager.isDarkMode() ? "☀️ Light" : "🌙 Dark");
        });

        HBox topBox = new HBox(themeBtn);
        topBox.setAlignment(Pos.TOP_RIGHT);
        topBox.setPadding(new Insets(10));
        VBox.setMargin(topBox, new Insets(-20, 0, 10, 0));

        body.getChildren().addAll(signInLbl, userBox, passBox, errorLabel, loginButton);
        card.getChildren().addAll(topBox, logoStrip, body);
        getChildren().add(card);

        loginButton.setOnAction(e -> handleLogin());
        passwordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleLogin(); });
        usernameField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) passwordField.requestFocus(); });
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("⚠ Please enter username and password.");
            errorLabel.setVisible(true);
            return;
        }

        // Disable login button and show loading state
        loginButton.setDisable(true);
        loginButton.setText("Signing in...");
        errorLabel.setVisible(false);

        // Create a background task for database authentication
        Task<User> loginTask = new Task<User>() {
            @Override
            protected User call() throws Exception {
                // Perform database authentication on background thread
                return authService.login(username, password);
            }

            @Override
            protected void succeeded() {
                User user = getValue();
                try {
                    if (user != null) {
                        // Successful login - navigate to dashboard on JavaFX thread
                        errorLabel.setText("");
                        errorLabel.setVisible(false);
                        NavigationHelper.goToDashboard(authService, stage);
                    } else {
                        // Failed login - show error on JavaFX thread
                        errorLabel.setText("❌ Invalid username or password.");
                        errorLabel.setVisible(true);
                        passwordField.clear();
                    }
                } finally {
                    // Always re-enable login button on JavaFX thread
                    resetLoginButton();
                }
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                try {
                    // Show error message on JavaFX thread
                    errorLabel.setText("❌ Login failed: " + (exception.getMessage() != null ? exception.getMessage() : "Database connection error"));
                    errorLabel.setVisible(true);
                    passwordField.clear();
                } finally {
                    // Always re-enable login button on JavaFX thread
                    resetLoginButton();
                }
            }
        };

        // Start the background task
        Thread loginThread = new Thread(loginTask);
        loginThread.setDaemon(true);
        loginThread.start();
    }

    private void resetLoginButton() {
        Platform.runLater(() -> {
            loginButton.setDisable(false);
            loginButton.setText("Sign In");
        });
    }
}
