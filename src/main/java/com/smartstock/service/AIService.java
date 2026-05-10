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
    private static final String API_KEY = EnvHelper.get("GEMINI_API_KEY", "YOUR_API_KEY");
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key="
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

        for (Branch b : branches) {
            inventoryData.append("Branch: ").append(b.getName()).append("\n");
            inventoryData.append("  Products count: ").append(b.getProductCount()).append("\n");
            inventoryData.append("  Total inventory: ").append(b.getTotalQuantity()).append(" units\n");
            inventoryData.append("  Low stock items: ").append(b.getLowStockCount()).append("\n");

            List<Product> lowStock = productDAO.findLowStock(b.getId());
            if (!lowStock.isEmpty()) {
                inventoryData.append("  Low stock products:\n");
                for (Product p : lowStock) {
                    inventoryData.append("    - ").append(p.getName()).append(" (only ").append(p.getQuantity())
                            .append(" left, min: ").append(p.getMinStock()).append(")\n");
                }
            }

            List<Product> fastMoving = productDAO.findFastMoving(b.getId(), 0);
            if (!fastMoving.isEmpty()) {
                inventoryData.append("  Most selling products:\n");
                // just top 5
                for (int i = 0; i < Math.min(5, fastMoving.size()); i++) {
                    Product p = fastMoving.get(i);
                    inventoryData.append("    - ").append(p.getName()).append(" (").append(p.getSellingPrice())
                            .append(" EGP)\n");
                }
            }
            
            List<Product> slowMoving = productDAO.findSlowMoving(b.getId(), 5);
            if (!slowMoving.isEmpty()) {
                inventoryData.append("  Lowest selling products:\n");
                for (Product p : slowMoving) {
                    inventoryData.append("    - ").append(p.getName()).append(" (").append(p.getSellingPrice())
                            .append(" EGP)\n");
                }
            }
            inventoryData.append("\n");
        }

        String prompt = "You are a retail inventory analyst. Given this multi-branch inventory data, generate a concise weekly summary in Arabic. Include: overall status of each branch, most selling products, and lowest selling products for EACH branch separately. Do not aggregate the top products globally, display them per branch. Keep it accurate and under 300 words.\n\n"
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
