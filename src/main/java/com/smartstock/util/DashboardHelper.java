package com.smartstock.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import java.io.IOException;

/**
 * Utility helper for dashboard navigation and FXML loading
 */
public class DashboardHelper {

    /**
     * Loads an FXML file and returns its root as a VBox.
     * Assumes the FXML root is a VBox as per the project's view structure.
     * 
     * @param fxmlPath The path to the FXML file (e.g., "/views/QRCodeView.fxml")
     * @return The root VBox of the loaded FXML
     * @throws IOException If the FXML cannot be loaded
     */
    public static VBox loadView(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(DashboardHelper.class.getResource(fxmlPath));
        Object root = loader.load();
        
        if (root instanceof VBox) {
            return (VBox) root;
        } else {
            throw new IOException("FXML root is not a VBox: " + fxmlPath);
        }
    }
}
