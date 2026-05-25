package com.smartstock.util;

import com.smartstock.model.Transaction;
import com.smartstock.model.TransactionItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InvoicePDFExporter {

    public static void exportInvoice(Transaction tx, List<TransactionItem> items, String filePath) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                int y = 780;
                y = write(cs, y, "SmartStock ERP - INVOICE", true);
                y = write(cs, y, "Invoice #: " + tx.getTransactionId(), false);
                y = write(cs, y, "Date: " + (tx.getTransactionAt() != null
                        ? tx.getTransactionAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : "-"), false);
                y = write(cs, y, "Branch: " + safe(tx.getBranchName()), false);
                y = write(cs, y, "Cashier: " + safe(tx.getCashierName()), false);
                y -= 6;
                y = write(cs, y, "Items", true);

                for (TransactionItem item : items) {
                    String line = String.format("- %s | Qty: %d | Unit: %.2f | Subtotal: %.2f",
                            safe(item.getProductName()), item.getQuantity(), item.getUnitPrice(), item.getSubtotal());
                    y = write(cs, y, line, false);
                    if (y < 80) break;
                }

                y -= 8;
                y = write(cs, y, String.format("Total: %.2f EGP", tx.getTotalAmount()), true);
                y = write(cs, y, String.format("Discount: %.2f EGP", tx.getDiscountAmount()), false);
                y = write(cs, y, String.format("Final Amount: %.2f EGP", tx.getFinalAmount()), true);
                y = write(cs, y, "Payment Method: " + safe(tx.getPaymentMethod()), false);
                write(cs, y - 16, "Status: " + safe(tx.getStatus()), false);
            }
            doc.save(filePath);
        }
    }

    private static int write(PDPageContentStream cs, int y, String text, boolean bold) throws IOException {
        cs.beginText();
        cs.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, bold ? 12 : 10);
        cs.newLineAtOffset(40, y);
        cs.showText(text.length() > 110 ? text.substring(0, 110) : text);
        cs.endText();
        return y - (bold ? 18 : 15);
    }

    private static String safe(String v) {
        return v == null || v.isBlank() ? "-" : v;
    }
}
