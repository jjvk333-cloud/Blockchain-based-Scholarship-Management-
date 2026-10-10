package com.scholarship.scholartrust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
public class ScholarTrustApplication {

    public static void main(String[] args) {
        loadDotenvIfPresent();
        SpringApplication.run(ScholarTrustApplication.class, args);
        System.out.println("\n========================================================");
        System.out.println(">>> ScholarTrust Backend Application Started Successfully! <<<");
        System.out.println("========================================================\n");
    }

    private static void loadDotenvIfPresent() {
        File envFile = new File(".env");
        if (!envFile.exists() || !envFile.isFile()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envFile.toPath());
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String val = line.substring(eqIdx + 1).trim();
                    if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                        val = val.substring(1, val.length() - 1);
                    }
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, val);
                    }
                }
            }
            System.out.println(">>> Loaded environment configurations from .env");
        } catch (Exception e) {
            System.err.println("Notice: Could not load .env file: " + e.getMessage());
        }
    }
}
