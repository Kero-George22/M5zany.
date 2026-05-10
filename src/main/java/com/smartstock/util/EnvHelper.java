package com.smartstock.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EnvHelper {
    private static final Map<String, String> envVars = new HashMap<>();

    static {
        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String value = line.substring(eqIdx + 1).trim();
                    envVars.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load .env file. Make sure it exists in the root directory.");
        }
    }

    public static String get(String key, String defaultValue) {
        if (envVars.containsKey(key)) {
            return envVars.get(key);
        }
        // Fallback to system environment variables
        String sysEnv = System.getenv(key);
        return sysEnv != null ? sysEnv : defaultValue;
    }

    public static String get(String key) {
        return get(key, null);
    }
}
