package com.smartstock.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EnvHelper {
    private static final Map<String, String> envVars = new HashMap<>();

    static {
        // Try multiple candidate locations for the .env file
        String[] candidates = {
            ".env",                                                                    // current working directory
            System.getProperty("user.dir") + "/.env",                                 // explicit working dir
            System.getProperty("user.dir") + "/ERP-main/.env",                        // nested project root
            getJarDirectory() + "/.env",                                               // next to the JAR
            getJarDirectory() + "/../.env",                                            // one level up from JAR
            getJarDirectory() + "/../../.env",                                         // two levels up (from target/classes)
            "c:/Users/Bios/Documents/JAVA Avanced/M5zany/.env",                       // Absolute fallback (this machine)
            "c:/Users/kirols/Desktop/ERP-main/ERP-main/.env"                          // Absolute fallback (old machine)
        };

        boolean loaded = false;
        StringBuilder triedPaths = new StringBuilder();
        for (String path : candidates) {
            java.io.File f = new java.io.File(path);
            triedPaths.append(f.getAbsolutePath()).append("\n");
            if (f.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
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
                    loaded = true;
                    break;
                } catch (IOException ignored) {}
            }
        }
        if (!loaded) {
            System.err.println("Could not load .env file. Tried:\n" + triedPaths.toString());
        }
    }

    private static String getJarDirectory() {
        try {
            java.net.URL url = EnvHelper.class.getProtectionDomain().getCodeSource().getLocation();
            java.io.File jarFile = new java.io.File(url.toURI());
            return jarFile.isDirectory() ? jarFile.getAbsolutePath() : jarFile.getParent();
        } catch (Exception e) {
            return ".";
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
