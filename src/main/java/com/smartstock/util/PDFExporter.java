package com.smartstock.util;

import com.smartstock.model.Branch;
import com.smartstock.model.Product;
import com.smartstock.model.User;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.io.IOException;
import java.util.List;

public class PDFExporter {

    public static void exportBranchReport(List<Branch> branches, String filePath) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(50, 750);
                cs.showText("SmartStock ERP - Branch Report");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(50, 720);
                cs.showText("Generated: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                cs.endText();

                int y = 690;
                for (Branch b : branches) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(50, y);
                    cs.showText(String.format("Branch: %s | Location: %s | Products: %d | Total Qty: %d | Low Stock: %d",
                            b.getName(), b.getLocation(), b.getProductCount(), b.getTotalQuantity(), b.getLowStockCount()));
                    cs.endText();
                    y -= 20;
                }
            }
            doc.save(filePath);
        }
    }

    public static void exportInventoryReport(List<Product> products, String filePath) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(50, 750);
                cs.showText("SmartStock ERP - Inventory Report");
                cs.endText();

                int y = 700;
                for (Product p : products) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 9);
                    cs.newLineAtOffset(50, y);
                    cs.showText(String.format("%s | Qty: %d | Price: %.2f | Min: %d",
                            p.getName(), p.getQuantity(), p.getSellingPrice(), p.getMinStock()));
                    cs.endText();
                    y -= 15;
                    if (y < 50) break;
                }
            }
            doc.save(filePath);
        }
    }

    public static void exportFullBranchReport(Branch branch, List<Product> products, List<User> users, String filePath) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            int y = 770;
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 17);
            cs.newLineAtOffset(40, y);
            cs.showText("SmartStock ERP - Full Branch Report");
            cs.endText();
            y -= 24;

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
            cs.newLineAtOffset(40, y);
            cs.showText("Generated: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            cs.endText();
            y -= 24;

            y = writeLine(cs, y, "Branch: " + safe(branch.getName()));
            y = writeLine(cs, y, "Location: " + safe(branch.getLocation()));
            y = writeLine(cs, y, "Phone: " + safe(branch.getPhone()) + " | Email: " + safe(branch.getEmail()));
            y = writeLine(cs, y, "Status: " + (branch.isActive() ? "ACTIVE" : "INACTIVE"));
            y = writeLine(cs, y, "Products: " + branch.getProductCount() + " | Total Qty: " + branch.getTotalQuantity() + " | Low Stock: " + branch.getLowStockCount());
            y -= 10;

            y = writeLineBold(cs, y, "Products");
            for (Product p : products) {
                String line = String.format("- %s | Qty: %d | Min: %d | Price: %.2f",
                        safe(p.getName()), p.getQuantity(), p.getMinStock(), p.getSellingPrice());
                y = writeLine(cs, y, line);
                if (y < 80) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = 770;
                }
            }

            y -= 8;
            y = writeLineBold(cs, y, "Users");
            for (User u : users) {
                String line = String.format("- %s (@%s) | Role: %s | %s",
                        safe(u.getFullName()), safe(u.getUsername()), safe(u.getRole()), u.isActive() ? "Active" : "Inactive");
                y = writeLine(cs, y, line);
                if (y < 80) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = 770;
                }
            }

            cs.close();
            doc.save(filePath);
        }
    }

    private static int writeLine(PDPageContentStream cs, int y, String text) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 10);
        cs.newLineAtOffset(40, y);
        cs.showText(text.length() > 110 ? text.substring(0, 110) : text);
        cs.endText();
        return y - 16;
    }

    private static int writeLineBold(PDPageContentStream cs, int y, String text) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.newLineAtOffset(40, y);
        cs.showText(text);
        cs.endText();
        return y - 18;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
