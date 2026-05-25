package com.smartstock.controller;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.smartstock.dao.ProductDAO;
import com.smartstock.model.Product;
import com.smartstock.util.ThemeManager;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

public class QRScannerController extends VBox {

    private final ProductDAO productDAO = new ProductDAO();
    private final ImageView imageView = new ImageView();
    private final Label resultLabel = new Label("Align QR code / Barcode within the frame");
    private final Label detailLabel = new Label("");
    private Webcam webcam = null;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Stage stage;
    private final java.util.function.Consumer<Product> onProductScanned;

    public QRScannerController(Stage stage, java.util.function.Consumer<Product> onProductScanned) {
        this.stage = stage;
        this.onProductScanned = onProductScanned;

        setSpacing(15);
        setPadding(new Insets(20));
        setAlignment(Pos.CENTER);
        ThemeManager.applyTheme(this);

        Label title = new Label("📷 QR / Barcode Scanner");
        title.getStyleClass().add("header-title");
        title.setStyle("-fx-font-size: 18px;");

        imageView.setFitWidth(400);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-border-color: #3B82F6; -fx-border-width: 2px;");

        resultLabel.getStyleClass().add("text-primary");
        resultLabel.setStyle("-fx-font-weight: bold;");

        detailLabel.setWrapText(true);
        detailLabel.setStyle("-fx-text-fill: -text-secondary;");

        Button closeBtn = new Button("Close Scanner");
        closeBtn.getStyleClass().addAll("button", "btn-danger");
        closeBtn.setOnAction(e -> close());

        getChildren().addAll(title, imageView, resultLabel, detailLabel, closeBtn);

        initializeWebcam();
    }

    private void initializeWebcam() {
        new Thread(() -> {
            try {
                webcam = Webcam.getDefault();
                if (webcam != null) {
                    Dimension size = WebcamResolution.VGA.getSize();
                    webcam.setViewSize(size);
                    webcam.open();
                    startScanning();
                } else {
                    Platform.runLater(() -> {
                        resultLabel.setText("❌ No webcam detected!");
                        resultLabel.setStyle("-fx-text-fill: #EF4444;");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> resultLabel.setText("❌ Error opening webcam: " + e.getMessage()));
            }
        }).start();
    }

    private void startScanning() {
        Thread thread = new Thread(() -> {
            while (running.get() && webcam != null && webcam.isOpen()) {
                BufferedImage image = webcam.getImage();
                if (image != null) {
                    WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
                    Platform.runLater(() -> imageView.setImage(fxImage));

                    try {
                        LuminanceSource source = new BufferedImageLuminanceSource(image);
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                        Result result = new MultiFormatReader().decode(bitmap);

                        if (result != null) {
                            String code = result.getText();
                            running.set(false); // Pause scanning on match
                            Platform.runLater(() -> handleDetectedCode(code));
                        }
                    } catch (NotFoundException e) {
                        // Fall through, keep scanning
                    }
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void handleDetectedCode(String code) {
        resultLabel.setText("🔍 Found: " + code);
        
        // Try finding by barcode first
        Product product = productDAO.findByBarcode(code);

        // If not found and code is numeric, try finding by ID
        if (product == null) {
            try {
                int id = Integer.parseInt(code.trim());
                product = productDAO.findById(id);
            } catch (NumberFormatException ignored) {}
        }

        if (product != null) {
            StringBuilder info = new StringBuilder();
            info.append("Product: ").append(product.getName()).append("\n");
            info.append("Category: ").append(product.getCategory()).append("\n");
            info.append("In Stock: ").append(product.getQuantity()).append("\n");

            if (product.getExpiryDate() != null) {
                LocalDate today = LocalDate.now();
                if (product.getExpiryDate().isBefore(today)) {
                    info.append("STATUS: ❌ EXPIRED (").append(product.getExpiryDate()).append(")");
                } else {
                    info.append("Expiry Date: ").append(product.getExpiryDate());
                }
            }

            detailLabel.setText(info.toString());
            detailLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");

            Button useBtn = new Button("Add to Cart");
            useBtn.getStyleClass().addAll("button", "btn-success");
            useBtn.setOnAction(e1 -> {
                if (onProductScanned != null) onProductScanned.accept(product);
                close();
            });

            Button retryBtn = new Button("Scan Again");
            retryBtn.getStyleClass().addAll("button", "btn-secondary");
            retryBtn.setOnAction(e2 -> {
                running.set(true);
                detailLabel.setText("");
                resultLabel.setText("Align QR code / Barcode within the frame");
                getChildren().removeAll(useBtn, retryBtn);
                startScanning();
            });

            getChildren().addAll(useBtn, retryBtn);
        } else {
            detailLabel.setText("❌ Product not found in database.");
            detailLabel.setStyle("-fx-text-fill: #EF4444;");

            Button retryBtn = new Button("Scan Again");
            retryBtn.getStyleClass().addAll("button", "btn-secondary");
            retryBtn.setOnAction(e -> {
                running.set(true);
                detailLabel.setText("");
                resultLabel.setText("Align QR code / Barcode within the frame");
                getChildren().remove(retryBtn);
                startScanning();
            });
            getChildren().add(retryBtn);
        }
    }

    public void close() {
        running.set(false);
        if (webcam != null) {
            try {
                webcam.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        stage.close();
    }
}
