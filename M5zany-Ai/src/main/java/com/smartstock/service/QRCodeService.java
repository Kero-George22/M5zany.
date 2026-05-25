package com.smartstock.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * QR Code Generation Service
 * Generates QR codes for products using ZXing library
 */
public class QRCodeService {

    private static final int QR_CODE_SIZE = 300;
    private static final String QR_CODE_DIRECTORY = "qrcodes";
    private static final String QR_CODE_FILE_PREFIX = "product_";
    private static final String QR_CODE_FILE_EXTENSION = ".png";

    /**
     * Generates a QR code for a product with the given data
     *
     * @param productId The product ID
     * @param productName The product name
     * @param barcode The product barcode
     * @return The file path where the QR code was saved, or null if failed
     */
    public String generateQRCode(int productId, String productName, String barcode) {
        try {
            // Create QR code directory if it doesn't exist
            Path qrCodeDir = Paths.get(QR_CODE_DIRECTORY);
            if (!Files.exists(qrCodeDir)) {
                Files.createDirectories(qrCodeDir);
            }

            // Generate QR code data (product ID + barcode for uniqueness)
            String qrData = String.format("PROD:%d|BAR:%s|NAME:%s", productId, barcode, productName);

            // Configure QR code hints
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate QR code matrix
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

            // Convert to BufferedImage
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Save to file
            String fileName = QR_CODE_FILE_PREFIX + productId + QR_CODE_FILE_EXTENSION;
            Path filePath = qrCodeDir.resolve(fileName);
            File outputFile = filePath.toFile();
            javax.imageio.ImageIO.write(qrImage, "PNG", outputFile);

            return filePath.toString();

        } catch (Exception e) {
            System.err.println("Error generating QR code for product " + productId + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates a QR code for a product with custom data
     *
     * @param productId The product ID
     * @param customData Custom data to encode in the QR code
     * @return The file path where the QR code was saved, or null if failed
     */
    public String generateQRCodeWithCustomData(int productId, String customData) {
        try {
            // Create QR code directory if it doesn't exist
            Path qrCodeDir = Paths.get(QR_CODE_DIRECTORY);
            if (!Files.exists(qrCodeDir)) {
                Files.createDirectories(qrCodeDir);
            }

            // Configure QR code hints
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate QR code matrix
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(customData, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

            // Convert to BufferedImage
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Save to file
            String fileName = QR_CODE_FILE_PREFIX + productId + QR_CODE_FILE_EXTENSION;
            Path filePath = qrCodeDir.resolve(fileName);
            File outputFile = filePath.toFile();
            javax.imageio.ImageIO.write(qrImage, "PNG", outputFile);

            return filePath.toString();

        } catch (Exception e) {
            System.err.println("Error generating QR code for product " + productId + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates a QR code and returns it as a BufferedImage
     *
     * @param productId The product ID
     * @param productName The product name
     * @param barcode The product barcode
     * @return The QR code as BufferedImage, or null if failed
     */
    public BufferedImage generateQRCodeAsImage(int productId, String productName, String barcode) {
        try {
            // Generate QR code data
            String qrData = String.format("PROD:%d|BAR:%s|NAME:%s", productId, barcode, productName);

            // Configure QR code hints
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            // Generate QR code matrix
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);

            // Convert to BufferedImage
            return MatrixToImageWriter.toBufferedImage(bitMatrix);

        } catch (Exception e) {
            System.err.println("Error generating QR code image for product " + productId + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deletes a QR code file for a product
     *
     * @param productId The product ID
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteQRCode(int productId) {
        try {
            String fileName = QR_CODE_FILE_PREFIX + productId + QR_CODE_FILE_EXTENSION;
            Path filePath = Paths.get(QR_CODE_DIRECTORY, fileName);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return true;
            }
            return false;
        } catch (IOException e) {
            System.err.println("Error deleting QR code for product " + productId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a QR code file exists for a product
     *
     * @param productId The product ID
     * @return true if QR code exists, false otherwise
     */
    public boolean qrCodeExists(int productId) {
        String fileName = QR_CODE_FILE_PREFIX + productId + QR_CODE_FILE_EXTENSION;
        Path filePath = Paths.get(QR_CODE_DIRECTORY, fileName);
        return Files.exists(filePath);
    }

    /**
     * Gets the file path for a product's QR code
     *
     * @param productId The product ID
     * @return The file path, or null if doesn't exist
     */
    public String getQRCodePath(int productId) {
        String fileName = QR_CODE_FILE_PREFIX + productId + QR_CODE_FILE_EXTENSION;
        Path filePath = Paths.get(QR_CODE_DIRECTORY, fileName);
        return Files.exists(filePath) ? filePath.toString() : null;
    }
}
