package com.demo.flightbooking.ai;

import com.demo.flightbooking.utils.ConfigReader;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

/**
 * AI-powered test data generator for the Flight Booking framework.
 *
 * WHAT IT DOES:
 * - Calls an LLM (Ollama local or OpenAI cloud) via LangChain4j
 * - Generates structured passenger data matching our Passenger record schema
 * - Writes output as CSV to src/test/resources/testdata/
 * - The generated CSV is consumed by CsvDataProvider — zero framework changes
 *
 * WHY THIS EXISTS:
 * DataFaker generates random data. AI generates INTELLIGENT data — edge cases,
 * boundary values, and scenario-specific combinations that humans miss under
 * time pressure. AI is not replacing DataFaker; it's a complementary data source
 * for test design acceleration.
 *
 * ARCHITECTURE:
 * - This is a UTILITY class, not a test class. It runs BEFORE tests, not during.
 * - It is decoupled from test execution — if the LLM is down, existing test data
 *   (passenger-data.csv, passengers.json) still works. Tests never depend on AI.
 * - Uses ConfigReader for settings — same pattern as all existing utilities.
 *
 * NOTE: AI generates schema-compliant edge-case data. Application behavior is
 * still validated by the test assertions — not by the data generator.
 *
 * USAGE:
 * - From IDE: Run AiDataRunner.main() or call AiDataGenerator.generate() directly
 * - From CLI: mvn exec:java -Dexec.mainClass="com.demo.flightbooking.ai.AiDataRunner"
 * - In CI: Run as a pre-test data preparation step (optional, never blocking)
 */
public class AiDataGenerator {

    private static final Logger logger = LogManager.getLogger(AiDataGenerator.class);

    // CSV header matching passenger-data.csv exactly
    private static final String CSV_HEADER =
            "origin,destination,firstName,lastName,address,city,state,zipCode," +
                    "cardType,cardNumber,month,year,cardName,age,gender";

    private final PassengerDataService service;

    /**
     * Creates an AiDataGenerator using the provider configured in config.properties.
     * Reads ai.provider to determine Ollama (local) or OpenAI (cloud).
     */
    public AiDataGenerator() {
        this(buildModel());
    }

    /**
     * Creates an AiDataGenerator with an explicitly provided ChatLanguageModel.
     * Used for testing (mock models) and when you want to override the config.
     *
     * @param model The ChatModel to use (Ollama, OpenAI, or mock).
     */
    public AiDataGenerator(ChatModel model) {
        this.service = AiServices.create(PassengerDataService.class, model);
        logger.info("AiDataGenerator initialized with model: {}", model.getClass().getSimpleName());
    }

    /**
     * Builds the appropriate ChatLanguageModel based on config.properties.
     * Pattern mirrors ConfigReader.getApplicationUrl() — config-driven behavior.
     */
    private static ChatModel buildModel() {
        String provider = ConfigReader.getProperty("ai.provider", "ollama");

        return switch (provider.toLowerCase()) {
            case "openai" -> {
                String apiKey = System.getenv("OPENAI_API_KEY");
                if (apiKey == null || apiKey.isBlank()) {
                    throw new IllegalStateException(
                            "OPENAI_API_KEY environment variable is not set. " +
                                    "Set it or switch to ai.provider=ollama in config.properties"
                    );
                }
                String model = ConfigReader.getProperty("ai.openai.model", "gpt-4o");
                logger.info("Using OpenAI provider with model: {}", model);
                yield OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .temperature(0.2) // Low temperature for consistent, factual data
                        .build();
            }
            case "ollama" -> {
                String baseUrl = ConfigReader.getProperty("ai.ollama.baseUrl", "http://localhost:11434");
                String model = ConfigReader.getProperty("ai.ollama.model", "llama3.2");
                logger.info("Using Ollama provider at {} with model: {}", baseUrl, model);
                yield OllamaChatModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(model)
                        .temperature(0.2)
                        .timeout(Duration.ofSeconds(300)) // Local LLM needs more time for structured output
                        .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                        .build();
            }
            default -> throw new IllegalArgumentException(
                    "Unknown AI provider: " + provider +
                            ". Supported: 'ollama' (local, free) or 'openai' (cloud, paid)"
            );
        };
    }

    /**
     * Generates passenger test data and writes it to a CSV file.
     *
     * @param count    Number of passenger records to generate.
     * @param scenario Description of what kind of data to generate.
     *                 Examples:
     *                 - "edge-case payment data with boundary card numbers"
     *                 - "international passengers with diverse names"
     *                 - "negative test data with invalid months and expired years"
     * @param fileName Output CSV filename (e.g., "ai-generated-passengers.csv").
     * @return Path to the generated CSV file.
     */
    public Path generate(int count, String scenario, String fileName) {
        logger.info("Generating {} passenger records for scenario: '{}'", count, scenario);

        // Step 1: Call the LLM via the AI Service
        List<PassengerDataService.PassengerData> passengers = service.generatePassengers(count, scenario);
        logger.info("LLM returned {} passenger records", passengers.size());

        // Step 2: Write to CSV in the testdata directory
        Path outputPath = resolveOutputPath(fileName);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath.toFile()))) {
            writer.println(CSV_HEADER);

            for (PassengerDataService.PassengerData p : passengers) {
                // Defensive: LLM may return null objects in rare failure cases
                if (p == null) {
                    logger.warn("Skipping null passenger record from LLM response");
                    continue;
                }
                String csvRow = String.join(",",
                        sanitize(p.origin()),
                        sanitize(p.destination()),
                        sanitize(p.firstName()),
                        sanitize(p.lastName()),
                        sanitize(p.address()),
                        sanitize(p.city()),
                        sanitize(p.state()),
                        sanitize(p.zipCode()),
                        sanitize(p.cardType()),
                        sanitize(p.cardNumber()),
                        sanitize(p.month()),
                        sanitize(p.year()),
                        sanitize(p.cardName()),
                        String.valueOf(p.age()),
                        sanitize(p.gender())
                );
                writer.println(csvRow);
            }

            logger.info("CSV written to: {}", outputPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to write CSV file: {}", outputPath, e);
            throw new RuntimeException("Failed to write AI-generated CSV", e);
        }

        return outputPath;
    }

    /**
     * Sanitizes a field value for CSV output.
     * - Replaces nulls with empty string
     * - Replaces commas with spaces (CsvDataProvider uses simple comma split,
     *   not RFC 4180 quote-aware parsing, so commas in values would break field count)
     * - Trims whitespace
     */
    private String sanitize(String value) {
        if (value == null) return "";
        // Replace commas — CsvDataProvider.line.split(",", -1) can't handle quoted CSV
        return value.trim().replace(",", " ");
    }

    /**
     * Resolves the output path for the generated CSV.
     * Reads output directory from config.properties (ai.data.outputDir).
     * Defaults to src/test/resources/testdata/ — same directory as existing data files.
     */
    private Path resolveOutputPath(String fileName) {
        String outputDir = ConfigReader.getProperty("ai.data.outputDir", "testdata/");
        Path testdataDir = Paths.get("src", "test", "resources", outputDir);

        // Create directory if it doesn't exist (defensive)
        try {
            Files.createDirectories(testdataDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create testdata directory", e);
        }

        return testdataDir.resolve(fileName);
    }
}
