package com.smartstock.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartstock.util.EnvHelper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service for calling Anthropic Claude API via HttpClient 5 (Module 1 - Dynamic Pricing).
 */
public class ClaudeAPIService {
    private static final String API_KEY = EnvHelper.get("CLAUDE_API_KEY", "YOUR_CLAUDE_API_KEY");
    private static final String API_URL = "https://api.anthropic.com/v1/messages";

    public PricingSuggestion suggestRetailPrice(String productName, int quantity, double wholesalePrice) {
        String prompt = String.format(
                "You are a pricing expert for a retail ERP system. " +
                "Product: %s, Quantity: %d, Wholesale Price: %.2f. " +
                "Suggest an appropriate retail price with brief reasoning. " +
                "Respond in JSON format with keys: suggested_price (number), reasoning (string).",
                productName, quantity, wholesalePrice
        );

        try {
            HttpClient client = HttpClient.newHttpClient();
            JsonObject body = new JsonObject();
            body.addProperty("model", "claude-3-5-sonnet-20241022");
            body.addProperty("max_tokens", 1024);
            
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            
            body.add("messages", new com.google.gson.JsonArray());
            body.getAsJsonArray("messages").add(message);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", API_KEY)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String content = json.getAsJsonArray("content")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();
                
                // Parse the JSON response from Claude
                JsonObject parsed = JsonParser.parseString(content).getAsJsonObject();
                double suggestedPrice = parsed.get("suggested_price").getAsDouble();
                String reasoning = parsed.get("reasoning").getAsString();
                
                return new PricingSuggestion(suggestedPrice, reasoning);
            } else {
                return new PricingSuggestion(wholesalePrice * 1.2, "API Error: " + response.statusCode());
            }
        } catch (Exception e) {
            return new PricingSuggestion(wholesalePrice * 1.2, "Failed to call Claude API: " + e.getMessage());
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
