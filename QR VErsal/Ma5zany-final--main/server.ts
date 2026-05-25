import express from "express";
import path from "path";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI } from "@google/genai";
import dotenv from "dotenv";

dotenv.config();

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(express.json());

  // Initialize Gemini if key is provided (Lazy initialization handled by skill guidance)
  const getGeminiClient = () => {
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) return null;
    return new GoogleGenAI({
      apiKey,
      httpOptions: {
        headers: {
          'User-Agent': 'aistudio-build',
        }
      }
    });
  };

  // API Routes
  app.get("/api/health", (req, res) => {
    res.json({ status: "ok", timestamp: new Date().toISOString() });
  });

  // Proxy for Open Food Facts to avoid CORS issues in browser
  app.get("/api/products/:barcode", async (req, res) => {
    const { barcode } = req.params;
    try {
      console.log(`[Proxy] Fetching product: ${barcode}`);
      const response = await fetch(`https://world.openfoodfacts.org/api/v2/product/${barcode}.json`);
      if (!response.ok) {
        return res.status(response.status).json({ error: "Failed to fetch from Open Food Facts" });
      }
      const data = await response.json();
      res.json(data);
    } catch (error) {
      console.error("[Proxy Error]:", error);
      res.status(500).json({ error: "Internal Server Error during proxy fetch" });
    }
  });

  // Gemini Smart Inventory Helper (Future proofing/fixing potential API needs)
  app.post("/api/inventory/suggest", async (req, res) => {
    const ai = getGeminiClient();
    if (!ai) {
      return res.status(500).json({ error: "GEMINI_API_KEY not configured" });
    }

    const { productInfo } = req.body;
    try {
      const response = await ai.models.generateContent({
        model: "gemini-3-flash-preview",
        contents: `Suggest inventory optimization for: ${JSON.stringify(productInfo)}`,
      });
      res.json({ suggestion: response.text });
    } catch (error) {
      console.error("Gemini Error:", error);
      res.status(500).json({ error: "Gemini API call failed" });
    }
  });

  // Vite middleware for development
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`[Server] Running at http://localhost:${PORT}`);
  });
}

startServer().catch(err => {
  console.error("Failed to start server:", err);
  process.exit(1);
});
