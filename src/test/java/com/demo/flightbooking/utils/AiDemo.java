package com.demo.flightbooking.utils;

import java.nio.file.Path;

/**
 * CLI entry point for AI test data generation.
 *
 * PURPOSE:
 * - Quick generation from IDE (right-click → Run)
 * - Live demo in interviews ("let me show you")
 * - CI pre-test data preparation
 *
 * USAGE:
 *   # Using Maven exec plugin (from project root):
 *   mvn compile exec:java \
 *     -Dexec.mainClass="com.demo.flightbooking.utils.AiDemo" \
 *     -Dexec.classpathScope=test
 *
 *   # With custom scenario:
 *   mvn compile exec:java \
 *     -Dexec.mainClass="com.demo.flightbooking.utils.AiDemo" \
 *     -Dexec.classpathScope=test \
 *     -Dscenario="boundary value data — shortest names, longest addresses"
 *
 *   # Using different provider:
 *   mvn compile exec:java \
 *     -Dexec.mainClass="com.demo.flightbooking.utils.AiDemo" \
 *     -Dexec.classpathScope=test \
 *     -Dai.provider=openai
 *
 * NOTE: This class is NOT a test. It's a standalone utility.
 * It exists in the test source tree because it uses test-scoped dependencies.
 */
public class AiDemo {

    public static void main(String[] args) {
        System.out.println("=== AI Test Data Generator ===");
        System.out.println("Provider: " + ConfigReader.getProperty("ai.provider", "ollama"));

        // Read scenario from system property or use default
        String scenario = System.getProperty("scenario",
            "edge-case passenger data covering boundary values, international names, " +
            "and payment validation scenarios");
        if (scenario.isBlank()) {
            scenario = "edge-case passenger data covering boundary values, international names, " +
                "and payment validation scenarios";
        }

        // Read count from system property or use config default
        int count = Integer.parseInt(System.getProperty("count",
            ConfigReader.getProperty("ai.data.defaultCount", "10")));

        String fileName = System.getProperty("output", "ai-generated-passengers.csv");

        System.out.println("Scenario: " + scenario);
        System.out.println("Count: " + count);
        System.out.println("Output: " + fileName);
        System.out.println();

        AiDataGenerator generator = new AiDataGenerator();
        Path outputPath = generator.generate(count, scenario, fileName);

        System.out.println();
        System.out.println("Generated " + count + " records -> " + outputPath.toAbsolutePath());
        System.out.println();
        System.out.println("To use this data in tests, update config.properties:");
        System.out.println("  data.file.passengers.csv=testdata/" + fileName);
    }
}
