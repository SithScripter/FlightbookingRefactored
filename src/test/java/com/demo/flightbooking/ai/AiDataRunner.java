package com.demo.flightbooking.ai;

import com.demo.flightbooking.utils.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * CLI entry point for AI test data generation.
 *
 * PURPOSE:
 * - Quick generation from IDE (right-click → Run)
 * - Quick validation and exploratory data generation
 * - CI pre-test data preparation
 *
 * USAGE:
 * # Using Maven exec plugin (from project root):
 * mvn compile exec:java \
 * -Dexec.mainClass="com.demo.flightbooking.ai.AiDataRunner" \
 * -Dexec.classpathScope=test
 *
 * # With custom scenario:
 * mvn compile exec:java \
 * -Dexec.mainClass="com.demo.flightbooking.ai.AiDataRunner" \
 * -Dexec.classpathScope=test \
 * -Dscenario="boundary value data — shortest names, longest addresses"
 *
 * # Using different provider:
 * mvn compile exec:java \
 * -Dexec.mainClass="com.demo.flightbooking.ai.AiDataRunner" \
 * -Dexec.classpathScope=test \
 * -Dai.provider=openai
 *
 * NOTE: This class is NOT a test. It's a standalone utility.
 * It exists in the test source tree because it uses test-scoped dependencies.
 */
public class AiDataRunner {

    private static final Logger logger = LogManager.getLogger(AiDataRunner.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("  AI Test Data Runner");
        logger.info("========================================");

        logger.info("Provider: {}", ConfigReader.getProperty("ai.provider", "ollama"));

        // Read scenario from system property or use default
        String defaultScenario = "edge-case passenger data covering boundary values, " +
                "international names, and payment validation scenarios";
        String scenario = System.getProperty("scenario", defaultScenario);
        if (scenario.isBlank()) {
            scenario = defaultScenario;
        }

        // Read count from system property or use config default
        int count;
        try {
            count = Integer.parseInt(System.getProperty("count",
                    ConfigReader.getProperty("ai.data.defaultCount", "10")));
        } catch (NumberFormatException e) {
            count = 10;
            logger.warn("Invalid 'count' value. Falling back to {}", count);
        }

        String fileName = System.getProperty("output", "ai-generated-passengers.csv");

        logger.info("Scenario: {}", scenario);
        logger.info("Count: {}", count);
        logger.info("Output: {}", fileName);

        AiDataGenerator generator = new AiDataGenerator();
        Path outputPath = generator.generate(count, scenario, fileName);

        logger.info("========================================");
        logger.info("  Generated {} records -> {}", count, outputPath.toAbsolutePath());
        logger.info("  To use in tests, update config.properties:");
        logger.info("  data.file.passengers.csv=testdata/{}", fileName);
        logger.info("========================================");
    }
}
