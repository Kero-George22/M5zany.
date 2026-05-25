package com.smartstock.controller;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.smartstock.dao.InventoryDAO;
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
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

public class QRScannerController extends VBox {

    public enum ScanMode { CASHIER, MANAGER }

    private final ProductDAO productDAO = new ProductDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final ImageView imageView = new ImageView();
    private final Label resultLabel = new Label("Align QR code / Barcode within the frame");
    private final VBox detailCard = new VBox();
    private Webcam webcam = null;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Stage stage;
    private final ScanMode mode;
    private final Integer userBranchId;
    private final java.util.function.Consumer<Product> onProductScanned;
    
    private Product currentProduct;

    public QRScannerController(Stage stage, ScanMode mode, Integer userBranchId, java.util.function.Consumer<Product> onProductScanned) {
        this.stage = stage;
        this.mode = mode;
        this.userBranchId = userBranchId != null ? userBranchId : 1; // fallback for Admin
        this.onProductScanned = onProductScanned;

        setSpacing(15);
        setPadding(new Insets(20));
        setAlignment(Pos.CENTER);
        ThemeManager.applyTheme(this);

        Label title = new Label("📷 REAL-TIME DETECTION");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #9CA3AF; -fx-letter-spacing: 1px;");
        
        imageView.setFitWidth(350);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-border-color: #3B82F6; -fx-border-width: 2px; -fx-border-radius: 8px;");

        resultLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        detailCard.setVisible(false);
        detailCard.setManaged(false);

        Button closeBtn = new Button("Close Scanner");
        closeBtn.getStyleClass().addAll("button", "btn-danger");
        closeBtn.setOnAction(e -> close());

        getChildren().addAll(title, imageView, resultLabel, detailCard, closeBtn);

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
        resultLabel.setText("🔍 Scanning: " + code);

        Product product = null;

        if (mode == ScanMode.CASHIER) {
            // Cashier: ONLY find products that exist in their own branch
            product = productDAO.findByBarcodeInBranch(code, this.userBranchId);
            if (product == null) {
                try {
                    int id = Integer.parseInt(code.trim());
                    product = productDAO.findByIdInBranch(id, this.userBranchId);
                } catch (NumberFormatException ignored) {}
            }
        } else {
            // Manager/Admin: find globally, then get branch-specific quantity
            product = productDAO.findByBarcode(code);
            if (product == null) {
                try {
                    int id = Integer.parseInt(code.trim());
                    product = productDAO.findById(id);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (product != null) {
            currentProduct = product;
            currentProduct.setBranchId(this.userBranchId);
            currentProduct.setQuantity(inventoryDAO.getQuantity(currentProduct.getId(), this.userBranchId));
            renderProductCard(currentProduct);
        } else {
            if (mode == ScanMode.CASHIER) {
                // Check if product exists globally but not in this branch
                Product globalProduct = productDAO.findByBarcode(code);
                if (globalProduct != null) {
                    resultLabel.setText("⚠️ Product not available in your branch");
                    resultLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
                } else {
                    resultLabel.setText("❌ Product not found: " + code);
                    resultLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                }
            } else {
                resultLabel.setText("❌ Product not found in database: " + code);
                resultLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
            }

            Button retryBtn = new Button("🔄 Scan Again");
            retryBtn.getStyleClass().addAll("button", "btn-secondary");
            retryBtn.setOnAction(e -> {
                running.set(true);
                resultLabel.setText("Align QR code / Barcode within the frame");
                resultLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
                getChildren().remove(retryBtn);
                startScanning();
            });
            getChildren().add(retryBtn);
        }
    }

    private void renderProductCard(Product product) {
        detailCard.getChildren().clear();
        detailCard.setVisible(true);
        detailCard.setManaged(true);
        detailCard.setStyle("-fx-background-color: #1E1E2E; -fx-border-color: #3B82F6; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 20; -fx-border-width: 1px;");
        detailCard.setSpacing(15);

        // Header (Status + Pass)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label statusLbl = new Label("STATUS: AUTHENTICATED");
        statusLbl.setStyle("-fx-text-fill: #3B82F6; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label passLbl = new Label("PASS");
        passLbl.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 10px;");
        header.getChildren().addAll(statusLbl, spacer, passLbl);

        // Name
        Label nameLbl = new Label(product.getName());
        nameLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");

        // Info Grid
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.getColumnConstraints().addAll(
            new ColumnConstraints(150),
            new ColumnConstraints(150)
        );

        grid.add(infoCell("INVENTORY LOCATION", "Branch " + userBranchId), 0, 0);
        grid.add(infoCell("UNIT PRICE", String.format("%.2f EGP", product.getSellingPrice())), 1, 0);
        grid.add(infoCell("AVAILABLE STOCK", product.getQuantity() + " units"), 0, 1);
        grid.add(infoCell("SHELF EXPIRY", product.getExpiryDate() != null ? product.getExpiryDate().toString() : "N/A"), 1, 1);

        // Action Buttons
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(10, 0, 0, 0));

        if (mode == ScanMode.CASHIER) {
            Button logSaleBtn = new Button("🛒 LOG SALE");
            logSaleBtn.getStyleClass().addAll("button", "btn-primary");
            logSaleBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(logSaleBtn, Priority.ALWAYS);
            logSaleBtn.setOnAction(e -> {
                if (onProductScanned != null) onProductScanned.accept(product);
                close();
            });

            Button refreshBtn = new Button("🔄 SCAN AGAIN");
            refreshBtn.getStyleClass().addAll("button", "btn-secondary");
            refreshBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(refreshBtn, Priority.ALWAYS);
            refreshBtn.setOnAction(e -> resetScanner());
            
            actionBox.getChildren().addAll(refreshBtn, logSaleBtn);
        } else {
            // Manager/Admin: Adjust stock with quantity dialog
            Button minusBtn = new Button("➖ REMOVE STOCK");
            minusBtn.getStyleClass().addAll("button", "btn-danger");
            minusBtn.setOnAction(e -> askAndAdjustStock(product, -1));

            Button plusBtn = new Button("➕ ADD STOCK");
            plusBtn.getStyleClass().addAll("button", "btn-success");
            plusBtn.setOnAction(e -> askAndAdjustStock(product, 1));
            
            Button scanAgainBtn = new Button("🔄 SCAN AGAIN");
            scanAgainBtn.getStyleClass().addAll("button", "btn-secondary");
            scanAgainBtn.setOnAction(e -> resetScanner());

            actionBox.getChildren().addAll(minusBtn, scanAgainBtn, plusBtn);
        }

        detailCard.getChildren().addAll(header, nameLbl, grid, actionBox);
    }

    private VBox infoCell(String title, String value) {
        VBox box = new VBox(4);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 10; -fx-background-radius: 8;");
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        box.getChildren().addAll(t, v);
        return box;
    }
    
    private void askAndAdjustStock(Product product, int direction) {
        // Step 1: Get all branches that have this product
        java.util.List<InventoryDAO.BranchStock> branches = inventoryDAO.getProductBranchStock(product.getId());

        if (branches.isEmpty()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("No Inventory Found");
            alert.setContentText("This product has no inventory records in any branch.");
            alert.showAndWait();
            return;
        }

        String emoji  = direction > 0 ? "➕" : "➖";
        String action = direction > 0 ? "Add to" : "Remove from";

        // Step 2: Show branch chooser dialog
        javafx.scene.control.ChoiceDialog<InventoryDAO.BranchStock> branchDlg =
            new javafx.scene.control.ChoiceDialog<>(branches.get(0), branches);
        branchDlg.setTitle(emoji + " Select Branch");
        branchDlg.setHeaderText(action + " stock: " + product.getName());
        branchDlg.setContentText("Choose branch:");

        branchDlg.showAndWait().ifPresent(selectedBranch -> {
            // Step 3: Ask for quantity
            javafx.scene.control.TextInputDialog qtyDlg = new javafx.scene.control.TextInputDialog("1");
            qtyDlg.setTitle(emoji + " Adjust Stock");
            qtyDlg.setHeaderText(action + " stock @ " + selectedBranch.branchName);
            qtyDlg.setContentText("Current: " + selectedBranch.quantity + " units\nQuantity to " + (direction > 0 ? "add" : "remove") + ":");
            qtyDlg.showAndWait().ifPresent(input -> {
                try {
                    int qty = Integer.parseInt(input.trim());
                    if (qty <= 0) return;
                    if (direction < 0 && qty > selectedBranch.quantity) {
                        javafx.scene.control.Alert warn = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                        warn.setTitle("Not Enough Stock");
                        warn.setContentText("Cannot remove " + qty + " from " + selectedBranch.branchName + ". Only " + selectedBranch.quantity + " available.");
                        warn.showAndWait();
                        return;
                    }
                    // Adjust in the selected branch
                    inventoryDAO.addOrUpdateQuantity(product.getId(), selectedBranch.branchId, direction * qty);
                    // Refresh the card using the scanner's own branch
                    Product updated = productDAO.findById(product.getId());
                    if (updated != null) {
                        updated.setBranchId(this.userBranchId);
                        updated.setQuantity(inventoryDAO.getQuantity(updated.getId(), this.userBranchId));
                        renderProductCard(updated);
                    }
                    javafx.scene.control.Alert ok = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    ok.setTitle("Done");
                    ok.setContentText(emoji + " " + qty + " units " + (direction > 0 ? "added to" : "removed from") + " " + selectedBranch.branchName + " successfully!");
                    ok.showAndWait();
                } catch (NumberFormatException ignored) {}
            });
        });
    }

    private void adjustStock(Product product, int delta) {
        try {
            inventoryDAO.addOrUpdateQuantity(product.getId(), this.userBranchId, delta);
            // Refresh product data
            Product updated = productDAO.findById(product.getId());
            if (updated != null) {
                updated.setBranchId(this.userBranchId);
                updated.setQuantity(inventoryDAO.getQuantity(updated.getId(), this.userBranchId));
                renderProductCard(updated);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void resetScanner() {
        running.set(true);
        detailCard.setVisible(false);
        detailCard.setManaged(false);
        resultLabel.setText("Align QR code / Barcode within the frame");
        resultLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        startScanning();
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
