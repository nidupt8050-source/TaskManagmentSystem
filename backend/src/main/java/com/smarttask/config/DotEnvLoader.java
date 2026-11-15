package com.smarttask.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads key=value pairs from a local .env file into system properties
 * when the matching environment variable is not already set.
 */
public final class DotEnvLoader {

    private DotEnvLoader() {
    }

    public static void load() {
        Path envFile = Paths.get(".env");
        if (!Files.exists(envFile)) {
            envFile = Paths.get(System.getProperty("user.dir"), ".env");
        }
        if (!Files.exists(envFile)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(envFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }

                int separator = line.indexOf('=');
                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();

                if (key.isEmpty()) {
                    continue;
                }

                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException ignored) {
            // Fall back to defaults / OS environment variables.
        }
    }
}
