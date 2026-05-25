package com.smartstock.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartstock.dao.BranchDAO;
import com.smartstock.dao.ProductDAO;
import com.smartstock.model.Branch;
import com.smartstock.model.Product;
import com.smartstock.util.EnvHelper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class AIService {
    private static final String API_KEY = EnvHelper.get("GEMINI_API_KEY", "AIzaSyA2G721RTBskif6MRmCLuI3hxUWvpLDiUM");
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
            + API_KEY;

    private final BranchDAO branchDAO;
    private final ProductDAO productDAO;

    public AIService() {
        this.branchDAO = new BranchDAO();
        this.productDAO = new ProductDAO();
    }

    public String generateWeeklySummary() {
        List<Branch> branches = branchDAO.findAll();
        StringBuilder inventoryData = new StringBuilder();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate soon  = today.plusDays(30);

        for (Branch b : branches) {
            inventoryData.append("=== Branch: ").append(b.getName()).append(" ===").append("\n");
            inventoryData.append("Location: ").append(b.getLocation()).append("\n");
            inventoryData.append("Total distinct products: ").append(b.getProductCount()).append("\n");
            inventoryData.append("Total inventory units: ").append(b.getTotalQuantity()).append("\n");
            inventoryData.append("Low-stock items count: ").append(b.getLowStockCount()).append("\n");

            // Inventory value estimate
            List<Product> allProducts = productDAO.findByBranchId(b.getId());
            double totalValue = allProducts.stream()
                    .mapToDouble(p -> p.getQuantity() * p.getSellingPrice()).sum();
            inventoryData.append(String.format("Estimated inventory value: %.2f EGP%n", totalValue));

            // Low stock
            List<Product> lowStock = productDAO.findLowStock(b.getId());
            if (!lowStock.isEmpty()) {
                inventoryData.append("LOW STOCK PRODUCTS (must reorder):\n");
                for (Product p : lowStock) {
                    inventoryData.append("  - ").append(p.getName())
                            .append(" | Current: ").append(p.getQuantity())
                            .append(" | Min Required: ").append(p.getMinStock())
                            .append(" | Shortage: ").append(p.getMinStock() - p.getQuantity())
                            .append(" units\n");
                }
            }

            // Expired / expiring soon
            java.util.List<Product> expiringSoon = new java.util.ArrayList<>();
            java.util.List<Product> alreadyExpired = new java.util.ArrayList<>();
            for (Product p : allProducts) {
                if (p.getExpiryDate() == null) continue;
                if (p.getExpiryDate().isBefore(today)) alreadyExpired.add(p);
                else if (p.getExpiryDate().isBefore(soon)) expiringSoon.add(p);
            }
            if (!alreadyExpired.isEmpty()) {
                inventoryData.append("EXPIRED PRODUCTS (remove immediately):\n");
                for (Product p : alreadyExpired) {
                    inventoryData.append("  - ").append(p.getName())
                            .append(" | Expired: ").append(p.getExpiryDate())
                            .append(" | Qty: ").append(p.getQuantity()).append("\n");
                }
            }
            if (!expiringSoon.isEmpty()) {
                inventoryData.append("EXPIRING WITHIN 30 DAYS (urgent action needed):\n");
                for (Product p : expiringSoon) {
                    long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, p.getExpiryDate());
                    inventoryData.append("  - ").append(p.getName())
                            .append(" | Expires: ").append(p.getExpiryDate())
                            .append(" (in ").append(daysLeft).append(" days) | Qty: ").append(p.getQuantity()).append("\n");
                }
            }

            // Top selling
            List<Product> fastMoving = productDAO.findFastMoving(b.getId(), 0);
            if (!fastMoving.isEmpty()) {
                inventoryData.append("BEST SELLING PRODUCTS (Top 5):\n");
                for (int i = 0; i < Math.min(5, fastMoving.size()); i++) {
                    Product p = fastMoving.get(i);
                    inventoryData.append("  - ").append(p.getName())
                            .append(" | Price: ").append(String.format("%.2f", p.getSellingPrice()))
                            .append(" EGP | Qty: ").append(p.getQuantity()).append("\n");
                }
            }

            // Slow selling
            List<Product> slowMoving = productDAO.findSlowMoving(b.getId(), 5);
            if (!slowMoving.isEmpty()) {
                inventoryData.append("SLOWEST MOVING PRODUCTS:\n");
                for (Product p : slowMoving) {
                    inventoryData.append("  - ").append(p.getName())
                            .append(" | Price: ").append(String.format("%.2f", p.getSellingPrice()))
                            .append(" EGP | Qty: ").append(p.getQuantity()).append("\n");
                }
            }
            inventoryData.append("\n");
        }

        String prompt = """
                أنت محلل مخزون محترف لنظام ERP متعدد الفروع. بناءً على البيانات التالية، أنشئ تقريراً أسبوعياً احترافياً وشاملاً باللغة العربية للمدير التنفيذي.

                التقرير يجب أن يحتوي على الأقسام التالية:

                1. **ملخص تنفيذي** (2-3 جمل عن الوضع العام لجميع الفروع)
                2. **تقرير كل فرع على حدة** يشمل:
                   - الوضع العام والتقييم (ممتاز / جيد / يحتاج تدخل)
                   - تفاصيل المخزون المنخفض وأولويات إعادة الطلب
                   - المنتجات منتهية الصلاحية أو المقاربة للانتهاء مع توصيات عاجلة
                   - المنتجات الأكثر والأقل مبيعاً مع تعليق تحليلي
                   - القيمة التقديرية للمخزون
                3. **التوصيات العاجلة** (مرتبة حسب الأولوية: عاجل جداً / عاجل / مقترح)
                4. **خلاصة ختامية** بخطوات العمل المقترحة للأسبوع القادم

                اجعل التقرير دقيقاً ومحددًا بالأرقام. لا تجمع بيانات الفروع ببعضها إلا في الملخص التنفيذي.

                البيانات:
                """
                + inventoryData.toString();


        try {
            HttpClient client = HttpClient.newHttpClient();
            JsonObject body = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            body.add("contents", contents);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String text = json.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();
                return text;
            } else {
                return "AI API error: " + response.statusCode() + "\n" + response.body();
            }
        } catch (Exception e) {
            return "Failed to call AI API: " + e.getMessage();
        }
    }
}
