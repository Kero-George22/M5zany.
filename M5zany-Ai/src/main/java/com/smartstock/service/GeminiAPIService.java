package com.smartstock.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartstock.util.EnvHelper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service for calling Gemini API via HttpClient 5 (Module 1 - Dynamic Pricing).
 */
public class GeminiAPIService {
    private static final String API_KEY = EnvHelper.get("GEMINI_API_KEY", "YOUR_API_KEY");
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + API_KEY;

    public PricingSuggestion suggestRetailPrice(String productName, int quantity, double wholesalePrice) {
        String prompt = String.format(
                "You are a pricing expert for a retail ERP system. " +
                "Product: %s, Quantity: %d, Wholesale Price: %.2f. " +
                "Suggest an appropriate retail price with brief reasoning. " +
                "Respond ONLY in valid JSON format with keys: suggested_price (number), reasoning (string). Do not include markdown formatting like ```json.",
                productName, quantity, wholesalePrice
        );

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
                String textResponse = json.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();
                
                if (textResponse.startsWith("```json")) textResponse = textResponse.substring(7);
                else if (textResponse.startsWith("```")) textResponse = textResponse.substring(3);
                if (textResponse.endsWith("```")) textResponse = textResponse.substring(0, textResponse.length() - 3);
                textResponse = textResponse.trim();
                
                JsonObject parsed = JsonParser.parseString(textResponse).getAsJsonObject();
                double suggestedPrice = parsed.get("suggested_price").getAsDouble();
                String reasoning = parsed.get("reasoning").getAsString();
                
                return new PricingSuggestion(suggestedPrice, reasoning);
            } else {
                return new PricingSuggestion(wholesalePrice * 1.2, "API Error: " + response.statusCode());
            }
        } catch (Exception e) {
            return new PricingSuggestion(wholesalePrice * 1.2, "Failed to call AI API: " + e.getMessage());
        }
    }

    public static class PricingSuggestion {
        private final double suggestedPrice;
        private final String reasoning;

        public PricingSuggestion(double suggestedPrice, String reasoning) {
            this.suggestedPrice = suggestedPrice;
            this.reasoning = reasoning;
        }

        public double getSuggestedPrice() { return suggestedPrice; }
        public String getReasoning() { return reasoning; }
    }
}
