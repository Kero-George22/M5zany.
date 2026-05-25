package com.smartstock.util;

import javafx.scene.Scene;
import javafx.scene.Parent;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;

public class ThemeManager {
    
    private static boolean isDarkMode = true; // Default to dark mode

    /**
     * Applies the current theme to the given scene.
     */
    public static void applyTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;
        updateRootClasses(scene.getRoot());
        applyAtlantaFxTheme();
    }

    /**
     * Applies the current theme to a Parent node (useful before a scene is created).
     */
    public static void applyTheme(Parent root) {
        if (root == null) return;
        updateRootClasses(root);
        applyAtlantaFxTheme();
    }

    /**
     * Toggles the theme and applies it to the given scene.
     */
    public static void toggleTheme(Scene scene) {
        isDarkMode = !isDarkMode;
        applyTheme(scene);
    }
    
    /**
     * Toggles the theme without requiring a scene right away.
     */
    public static void toggleTheme() {
        isDarkMode = !isDarkMode;
        applyAtlantaFxTheme();
    }

    public static boolean isDarkMode() {
        return isDarkMode;
    }

    private static void updateRootClasses(Parent root) {
        root.getStyleClass().removeAll("light-theme", "dark-theme");
        root.getStyleClass().add(isDarkMode ? "dark-theme" : "light-theme");
    }

    public static void applyAtlantaFxTheme() {
        if (isDarkMode) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }
    }
}
