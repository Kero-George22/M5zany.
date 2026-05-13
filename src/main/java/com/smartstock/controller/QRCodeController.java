package com.smartstock.controller;

import com.smartstock.dao.BranchDAO;
import com.smartstock.dao.ProductDAO;
import com.smartstock.model.Product;
import com.smartstock.service.QRCodeService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.print.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.application.Platform;
import javafx.concurrent.Task;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Controller for QR Code Generation Interface
 * Handles QR code generation, preview, and printing for products
 */
public class QRCodeController {

    @FXML private Label branchLabel;
    
    @FXML private ComboBox<Product> productComboBox;
    @FXML private TextField productIdField;
    @FXML private TextField barcodeField;
    @FXML private TextField productNameField;
    @FXML private TextField categoryField;
    @FXML private TextField priceField;
    
    @FXML private ImageView qrCodeImageView;
    @FXML private Label qrStatusText;
    @FXML private TextArea qrDataTextArea;
    
    @FXML private Button saveQRCodeButton;
    @FXML private Button printQRCodeButton;
    
    @FXML private TableView<QRCodeItem> qrCodesTable;
    @FXML private TableColumn<QRCodeItem, Integer> qrProductIdColumn;
    @FXML private TableColumn<QRCodeItem, String> qrProductNameColumn;
    @FXML private TableColumn<QRCodeItem, String> qrBarcodeColumn;
    @FXML private TableColumn<QRCodeItem, String> qrPathColumn;
    @FXML private TableColumn<QRCodeItem, String> qrActionColumn;
    
    private final QRCodeService qrCodeService;
    private final ProductDAO productDAO;
    private final BranchDAO branchDAO;
    
    private int currentBranchId;
    private Product selectedProduct;
    private Image currentQRImage;
    private String currentQRPath;
    private ObservableList<QRCodeItem> qrCodeItems;
    
    public QRCodeController() {
        this.qrCodeService = new QRCodeService();
        this.productDAO = new ProductDAO();
        this.branchDAO = new BranchDAO();
    }
    
    @FXML
    public void initialize() {
        // Initialize table columns
        initializeColumns();
        
        // Set default branch (would normally come from session)
        currentBranchId = 1;
        branchLabel.setText("Branch: " + getBranchName(currentBranchId));
        
        // Load products into combo box
        loadProducts();
        
        // Initialize data list
        qrCodeItems = FXCollections.observableArrayList();
        qrCodesTable.setItems(qrCodeItems);
        
        // Load existing QR codes
        loadExistingQRCodes();
        
        // Set up product selection listener
        productComboBox.setOnAction(e -> handleProductSelection());
    }
    
    private void initializeColumns() {
        qrProductIdColumn.setCellValueFactory(cellData -> cellData.getValue().productIdProperty().asObject());
        qrProductNameColumn.setCellValueFactory(cellData -> cellData.getValue().productNameProperty());
        qrBarcodeColumn.setCellValueFactory(cellData -> cellData.getValue().barcodeProperty());
        qrPathColumn.setCellValueFactory(cellData -> cellData.getValue().qrPathProperty());
        qrActionColumn.setCellValueFactory(cellData -> cellData.getValue().actionProperty());
    }
    
    private void loadProducts() {
        List<Product> products = productDAO.findByBranchId(currentBranchId);
        productComboBox.setItems(FXCollections.observableArrayList(products));
    }
    
    private void handleProductSelection() {
        selectedProduct = productComboBox.getValue();
        if (selectedProduct == null) {
            return;
        }
        
        // Populate product fields
        productIdField.setText(String.valueOf(selectedProduct.getId()));
        barcodeField.setText(selectedProduct.getBarcode() != null ? selectedProduct.getBarcode() : "N/A");
        productNameField.setText(selectedProduct.getName());
        categoryField.setText(selectedProduct.getCategory() != null ? selectedProduct.getCategory() : "N/A");
        priceField.setText(String.format("%.2f", selectedProduct.getSellingPrice()));
        
        // Check if QR code already exists
        String existingPath = qrCodeService.getQRCodePath(selectedProduct.getId());
        if (existingPath != null) {
            loadExistingQRCode(existingPath);
        } else {
            clearQRPreview();
        }
    }
    
    @FXML
    private void handleGenerateQRCode() {
        if (selectedProduct == null) {
            showAlert("Error", "Please select a product first", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            // Generate QR code as BufferedImage
            BufferedImage bufferedImage = qrCodeService.generateQRCodeAsImage(
                    selectedProduct.getId(),
                    selectedProduct.getName(),
                    selectedProduct.getBarcode() != null ? selectedProduct.getBarcode() : "N/A"
            );
            
            if (bufferedImage != null) {
                // Convert BufferedImage to JavaFX Image
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(bufferedImage, "png", baos);
                byte[] imageBytes = baos.toByteArray();
                currentQRImage = new Image(new java.io.ByteArrayInputStream(imageBytes));
                
                // Display in ImageView
                qrCodeImageView.setImage(currentQRImage);
                qrStatusText.setText("QR Code Generated");
                
                // Display QR data
                String qrData = String.format("PROD:%d|BAR:%s|NAME:%s",
                        selectedProduct.getId(),
                        selectedProduct.getBarcode() != null ? selectedProduct.getBarcode() : "N/A",
                        selectedProduct.getName());
                qrDataTextArea.setText(qrData);
                
                // Enable save and print buttons
                saveQRCodeButton.setDisable(false);
                printQRCodeButton.setDisable(false);
                
                showNotification("Success", "QR Code generated successfully", "mdi2c-check-circle", "#10B981");
            } else {
                showNotification("Error", "Failed to generate QR code", "mdi2a-alert-circle", "#EF4444");
            }
        } catch (Exception e) {
            showNotification("Error", "Generation failed: " + e.getMessage(), "mdi2a-alert-circle", "#EF4444");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleSaveQRCode() {
        if (selectedProduct == null || currentQRImage == null) {
            showAlert("Error", "Please generate a QR code first", Alert.AlertType.ERROR);
            return;
        }
        
        try {
            // Generate and save QR code file
            currentQRPath = qrCodeService.generateQRCode(
                    selectedProduct.getId(),
                    selectedProduct.getName(),
                    selectedProduct.getBarcode() != null ? selectedProduct.getBarcode() : "N/A"
            );
            
            if (currentQRPath != null) {
                // Update product in database with QR code path
                selectedProduct.setQrCode(currentQRPath);
                selectedProduct.setQrCodePath(currentQRPath);
                productDAO.update(selectedProduct);
                
                qrStatusText.setText("QR Code Saved: " + currentQRPath);
                showNotification("Saved", "QR Code saved to database", "mdi2c-content-save-check", "#6366F1");
                
                // Refresh table
                loadExistingQRCodes();
            } else {
                showNotification("Error", "Failed to save QR code", "mdi2a-alert-circle", "#EF4444");
            }
        } catch (Exception e) {
            showNotification("Error", "Save failed: " + e.getMessage(), "mdi2a-alert-circle", "#EF4444");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handlePrintQRCode() {
        if (currentQRImage == null) {
            showNotification("Warning", "Generate a QR code before printing", "mdi2a-alert", "#F59E0B");
            return;
        }
        
        // Use a Task for printing to avoid UI freezing
        Task<Boolean> printTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                Printer printer = Printer.getDefaultPrinter();
                if (printer == null) {
                    throw new RuntimeException("No default printer found on this system.");
                }

                // Standard Label size (approx 2x2 inches / 144x144 points)
                Paper customLabel = Paper.DESIGNATED_LONG; // Or a more specific one if needed
                PageLayout pageLayout = printer.createPageLayout(Paper.NA_LETTER, PageOrientation.PORTRAIT, Printer.MarginType.HARDWARE_MINIMUM);
                
                PrinterJob job = PrinterJob.createPrinterJob(printer);
                if (job == null) {
                    throw new RuntimeException("Could not create print job.");
                }

                // Prepare print node on JFX thread later if needed, but here we can build it
                // Note: Nodes must be created on FX thread
                return true; // Just a placeholder for the logic below which I'll move to Platform.runLater
            }
        };

        // Building the UI on FX thread
        Platform.runLater(() -> {
            try {
                PrinterJob job = PrinterJob.createPrinterJob();
                if (job != null && job.showPrintDialog(qrCodeImageView.getScene().getWindow())) {
                    
                    // Create a beautiful label node for printing
                    VBox labelNode = new VBox(5);
                    labelNode.setAlignment(Pos.CENTER);
                    labelNode.setStyle("-fx-background-color: white; -fx-padding: 10;");
                    
                    // Product Header
                    Text header = new Text("M5ZANY ERP");
                    header.setFont(Font.font("System", FontWeight.BOLD, 10));
                    header.setFill(Color.BLACK);
                    
                    // QR Image
                    ImageView printImageView = new ImageView(currentQRImage);
                    printImageView.setFitWidth(150);
                    printImageView.setFitHeight(150);
                    printImageView.setPreserveRatio(true);
                    
                    // Details
                    Text prodName = new Text(selectedProduct.getName());
                    prodName.setFont(Font.font("System", FontWeight.BOLD, 12));
                    
                    Text barcode = new Text("ID: " + selectedProduct.getId() + " | " + selectedProduct.getBarcode());
                    barcode.setFont(Font.font("System", 10));
                    
                    labelNode.getChildren().addAll(header, printImageView, prodName, barcode);
                    
                    // Scale to fit page if needed
                    PageLayout pageLayout = job.getJobSettings().getPageLayout();
                    double scaleX = pageLayout.getPrintableWidth() / labelNode.getBoundsInParent().getWidth();
                    double scaleY = pageLayout.getPrintableHeight() / labelNode.getBoundsInParent().getHeight();
                    double minimumScale = Math.min(scaleX, scaleY);
                    if (minimumScale < 1.0) {
                        labelNode.getTransforms().add(new Scale(minimumScale, minimumScale));
                    }

                    if (job.printPage(labelNode)) {
                        job.endJob();
                        showNotification("Printed", "Label sent to printer", "mdi2p-printer-check", "#10B981");
                    } else {
                        showNotification("Error", "Print job failed", "mdi2a-alert-circle", "#EF4444");
                    }
                }
            } catch (Exception e) {
                showAlert("Printer Error", "Failed to print: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void showNotification(String title, String message, String iconCode, String color) {
        Platform.runLater(() -> {
            FontIcon icon = new FontIcon(iconCode);
            icon.setIconSize(24);
            icon.setIconColor(Color.web(color));

            Notifications.create()
                .title(title)
                .text(message)
                .graphic(icon)
                .hideAfter(Duration.seconds(3))
                .position(Pos.TOP_RIGHT)
                .owner(qrCodeImageView.getScene().getWindow())
                .show();
        });
    }
    
    @FXML
    private void handleClear() {
        clearQRPreview();
        selectedProduct = null;
        productComboBox.getSelectionModel().clearSelection();
        productIdField.clear();
        barcodeField.clear();
        productNameField.clear();
        categoryField.clear();
        priceField.clear();
        qrDataTextArea.clear();
        currentQRImage = null;
        currentQRPath = null;
        saveQRCodeButton.setDisable(true);
        printQRCodeButton.setDisable(true);
    }
    
    @FXML
    private void handleRefresh() {
        loadProducts();
        loadExistingQRCodes();
    }
    
    private void loadExistingQRCode(String path) {
        try {
            File qrFile = new File(path);
            if (qrFile.exists()) {
                BufferedImage bufferedImage = ImageIO.read(qrFile);
                // Convert BufferedImage to JavaFX Image
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(bufferedImage, "png", baos);
                byte[] imageBytes = baos.toByteArray();
                Image fxImage = new Image(new java.io.ByteArrayInputStream(imageBytes));
                
                qrCodeImageView.setImage(fxImage);
                qrStatusText.setText("QR Code Loaded");
                currentQRImage = fxImage;
                currentQRPath = path;
                saveQRCodeButton.setDisable(false);
                printQRCodeButton.setDisable(false);
            }
        } catch (IOException e) {
            System.err.println("Error loading existing QR code: " + e.getMessage());
        }
    }
    
    private void loadExistingQRCodes() {
        qrCodeItems.clear();
        
        List<Product> products = productDAO.findAll();
        for (Product product : products) {
            if (product.getQrCode() != null && !product.getQrCode().isEmpty()) {
                QRCodeItem item = new QRCodeItem(
                        product.getId(),
                        product.getName(),
                        product.getBarcode() != null ? product.getBarcode() : "N/A",
                        product.getQrCode(),
                        "View"
                );
                qrCodeItems.add(item);
            }
        }
    }
    
    private void clearQRPreview() {
        qrCodeImageView.setImage(null);
        qrStatusText.setText("No QR code generated");
        currentQRImage = null;
        currentQRPath = null;
        saveQRCodeButton.setDisable(true);
        printQRCodeButton.setDisable(true);
    }
    
    private String getBranchName(int branchId) {
        // This would use BranchDAO to get the branch name
        // For now, return a default
        return "Branch " + branchId;
    }
    
    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner class for QR code table items
    public static class QRCodeItem {
        private final SimpleIntegerProperty productId;
        private final SimpleStringProperty productName;
        private final SimpleStringProperty barcode;
        private final SimpleStringProperty qrPath;
        private final SimpleStringProperty action;
        
        public QRCodeItem(int productId, String productName, String barcode, String qrPath, String action) {
            this.productId = new SimpleIntegerProperty(productId);
            this.productName = new SimpleStringProperty(productName);
            this.barcode = new SimpleStringProperty(barcode);
            this.qrPath = new SimpleStringProperty(qrPath);
            this.action = new SimpleStringProperty(action);
        }
        
        public SimpleIntegerProperty productIdProperty() { return productId; }
        public SimpleStringProperty productNameProperty() { return productName; }
        public SimpleStringProperty barcodeProperty() { return barcode; }
        public SimpleStringProperty qrPathProperty() { return qrPath; }
        public SimpleStringProperty actionProperty() { return action; }
    }
}
