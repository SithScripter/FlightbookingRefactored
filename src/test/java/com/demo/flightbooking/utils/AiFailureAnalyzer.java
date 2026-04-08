package com.demo.flightbooking.utils;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AI-powered failure analyzer for the Flight Booking framework (Phase 4).
 *
 * WHAT IT DOES:
 * - Reads Surefire XML report (testng-results.xml) for test results
 * - Extracts STRUCTURED failure data (test name, exception, message, stack trace)
 * - Reads automation log and filters to ERROR/WARN + failure-related lines
 * - Masks sensitive data via MaskingUtil.maskLogContent() before AI processing
 * - Sends structured, high-signal input to LLM for root cause analysis
 * - Generates a markdown report (failure-analysis-report.md)
 *
 * WHY THIS EXISTS:
 * Debugging test failures is the biggest time sink in QA. This utility converts
 * raw execution data into actionable intelligence — root cause classification,
 * flaky vs real analysis, and a business-readable executive summary.
 *
 * ARCHITECTURE:
 * - POST-EXECUTION utility — runs AFTER tests complete, never during
 * - Zero impact on test performance, stability, or parallel execution
 * - Advisory only — output is a report, not a pass/fail decision
 * - Same ChatModel pattern as AiDataGenerator (Phase 3)
 * - If LLM is unavailable, tests still work — this is a supplementary tool
 *
 * DATA FLOW (v2 — structured preprocessing):
 *   Surefire XML → extractFailureBlocks() → structured failures
 *   Automation Log → filterRelevantLogs() → error/warn signals
 *   Both → MaskingUtil → LLM (FailureAnalysisService) → Markdown Report
 *
 * USAGE:
 * - From IDE: Run FailureAnalysisDemo.main() or call AiFailureAnalyzer.analyze() directly
 * - From CLI: mvn exec:java -Dexec.mainClass="com.demo.flightbooking.utils.FailureAnalysisDemo"
 * - In CI: Phase 5 — Jenkins shared library step (analyzeFailuresWithAi.groovy)
 */
public class AiFailureAnalyzer {

    private static final Logger logger = LogManager.getLogger(AiFailureAnalyzer.class);

    /** Max lines from filtered log output — keeps LLM context focused */
    private static final int MAX_LOG_LINES = 30;

    private final FailureAnalysisService service;

    /**
     * Creates an AiFailureAnalyzer using the provider configured in config.properties.
     * Same config-driven pattern as AiDataGenerator.
     */
    public AiFailureAnalyzer() {
        this(buildModel());
    }

    /**
     * Creates an AiFailureAnalyzer with an explicitly provided ChatModel.
     * Used for testing (mock models) and when you want to override the config.
     *
     * @param model The ChatModel to use (Ollama, OpenAI, or mock).
     */
    public AiFailureAnalyzer(ChatModel model) {
        this.service = AiServices.create(FailureAnalysisService.class, model);
        logger.info("AiFailureAnalyzer initialized with model: {}", model.getClass().getSimpleName());
    }

    /**
     * Builds the appropriate ChatModel based on config.properties.
     * Same pattern as AiDataGenerator.buildModel() — reuses same config keys.
     *
     * KEY DIFFERENCE: No Capability.RESPONSE_FORMAT_JSON_SCHEMA needed here.
     * Phase 3 needs schema enforcement (structured JSON output).
     * Phase 4 returns free-form markdown (interpretive analysis).
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
                        .temperature(0.2)
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
                        .timeout(Duration.ofSeconds(300))
                        // No .supportedCapabilities() — Phase 4 returns String, not JSON schema
                        .build();
            }
            default -> throw new IllegalArgumentException(
                    "Unknown AI provider: " + provider +
                            ". Supported: 'ollama' (local, free) or 'openai' (cloud, paid)"
            );
        };
    }

    /**
     * Runs the full failure analysis pipeline (v2 — structured preprocessing):
     * 1. Read Surefire XML → extract test counts
     * 2. Extract structured failure blocks from XML (test name, exception, stack trace)
     * 3. Read automation log → filter to ERROR/WARN + failure-related lines
     * 4. Mask sensitive data → MaskingUtil.maskLogContent()
     * 5. Send structured input to LLM → FailureAnalysisService.analyzeFailures()
     * 6. Write markdown report → target/failure-analysis-report.md
     *
     * @return Path to the generated report, or null if no failures detected.
     */
    public Path analyze() {
        logger.info("=== AI Failure Analysis Started ===");

        // Step 1: Read Surefire XML
        Path surefirePath = Paths.get("target", "surefire-reports", "testng-results.xml");
        if (!Files.exists(surefirePath)) {
            logger.warn("Surefire XML not found at {}. Run tests first.", surefirePath);
            return null;
        }

        String surefireXml;
        try {
            surefireXml = Files.readString(surefirePath);
        } catch (IOException e) {
            logger.error("Failed to read Surefire XML: {}", surefirePath, e);
            return null;
        }

        // Step 2: Extract test counts from XML attributes
        int total = extractAttribute(surefireXml, "total");
        int passed = extractAttribute(surefireXml, "passed");
        int failed = extractAttribute(surefireXml, "failed");
        int skipped = extractAttribute(surefireXml, "skipped");

        logger.info("Test results — Total: {}, Passed: {}, Failed: {}, Skipped: {}",
                total, passed, failed, skipped);

        // Early exit: No failures = no analysis needed
        if (failed == 0) {
            logger.info("No failures detected. Skipping AI analysis.");
            return null;
        }

        // Step 3: Extract STRUCTURED failure blocks from XML
        // Instead of passing raw XML (noisy), we extract only the failure details
        String structuredFailures = extractFailureBlocks(surefireXml);
        logger.info("Extracted {} structured failure block(s) from XML", failed);

        // Step 4: Read automation log — filtered to ERROR/WARN + failure context
        Path logPath = Paths.get("logs", "automation.log");
        String filteredLog = filterRelevantLogs(logPath, structuredFailures);

        // Step 5: MASK sensitive data before sending to AI
        String maskedFailures = MaskingUtil.maskLogContent(structuredFailures);
        String maskedLog = MaskingUtil.maskLogContent(filteredLog);
        logger.info("Data masked. Failures: {} chars, Filtered log: {} chars",
                maskedFailures.length(), maskedLog.length());

        // Step 6: Call the LLM with structured input
        // We pass the structured failures as "surefireXml" since the prompt expects it
        // The maskedLog now contains only signal, not noise
        logger.info("Sending structured data to AI for analysis...");
        String analysis;
        try {
            analysis = service.analyzeFailures(maskedFailures, maskedLog, total, passed, failed, skipped);
        } catch (Exception e) {
            logger.error("AI analysis failed (LLM error): {}", e.getMessage(), e);
            return null;
        }

        // Step 7: Write the report
        Path reportPath = writeReport(analysis, total, passed, failed, skipped);
        logger.info("=== AI Failure Analysis Complete. Report: {} ===", reportPath);

        return reportPath;
    }

    /**
     * Extracts a numeric attribute from the testng-results.xml root element.
     * Uses simple string indexOf — no XML parser needed for 4 attributes.
     *
     * Design decision: indexOf over DOM/SAX because:
     * - Only 4 attributes needed (total, passed, failed, skipped)
     * - Zero dependency overhead
     * - Stable across TestNG versions (root element format unchanged)
     *
     * @param xml       Full XML content
     * @param attribute Attribute name (e.g., "total", "failed")
     * @return Attribute value as int, or 0 if not found
     */
    int extractAttribute(String xml, String attribute) {
        String search = attribute + "=\"";
        int start = xml.indexOf(search);
        if (start == -1) return 0;

        start += search.length();
        int end = xml.indexOf("\"", start);
        if (end == -1) return 0;

        try {
            return Integer.parseInt(xml.substring(start, end));
        } catch (NumberFormatException e) {
            logger.warn("Could not parse attribute '{}': {}", attribute, xml.substring(start, end));
            return 0;
        }
    }

    /**
     * Extracts structured failure blocks from testng-results.xml.
     *
     * Instead of sending the entire raw XML (436+ lines of noise) to the LLM,
     * we parse out only the failure-relevant data:
     * - Test method name
     * - Exception class
     * - Error message (from CDATA)
     * - Stack trace top (first 5 lines — framework frames, not JDK internals)
     *
     * Design decision: Regex over DOM/SAX because:
     * - TestNG XML structure is stable and well-defined
     * - We only need status="FAIL" blocks (not the entire DOM tree)
     * - Zero dependency overhead (no javax.xml imports)
     * - Regex is sufficient for extracting CDATA content between known tags
     *
     * @param xml Full testng-results.xml content
     * @return Structured text block like:
     *         FAILURE 1: testPurchaseWithInvalidData
     *         Exception: java.lang.AssertionError
     *         Message: Purchase should have failed...
     *         StackTrace: (top 5 lines)
     */
    String extractFailureBlocks(String xml) {
        StringBuilder result = new StringBuilder();
        result.append("STRUCTURED FAILURE DATA (extracted from TestNG XML):\n");
        result.append("=".repeat(60)).append("\n\n");

        // Pattern to find <test-method> blocks with status="FAIL" (excluding is-config methods)
        // We look for blocks that do NOT have is-config="true"
        int failureCount = 0;
        int searchFrom = 0;

        while (true) {
            // Find next status="FAIL" occurrence
            int failIndex = xml.indexOf("status=\"FAIL\"", searchFrom);
            if (failIndex == -1) break;

            // Walk back to find the <test-method that contains this status
            int methodStart = xml.lastIndexOf("<test-method", failIndex);
            if (methodStart == -1) {
                searchFrom = failIndex + 1;
                continue;
            }

            // Skip config methods (setUp, tearDown) — they have is-config="true"
            String methodTag = xml.substring(methodStart, failIndex + 20);
            if (methodTag.contains("is-config=\"true\"")) {
                searchFrom = failIndex + 1;
                continue;
            }

            failureCount++;
            result.append("FAILURE ").append(failureCount).append(":\n");

            // Extract test name
            String name = extractXmlValue(methodTag, "name=\"");
            result.append("  Test: ").append(name != null ? name : "unknown").append("\n");

            // Find the end of this test-method block
            int methodEnd = xml.indexOf("</test-method>", failIndex);
            if (methodEnd == -1) methodEnd = xml.length();
            String methodBlock = xml.substring(methodStart, methodEnd);

            // Extract exception class
            String exceptionClass = extractXmlValue(methodBlock, "exception class=\"");
            result.append("  Exception: ").append(exceptionClass != null ? exceptionClass : "unknown").append("\n");

            // Extract message from CDATA
            String message = extractCdataContent(methodBlock, "<message>");
            if (message != null) {
                // Truncate long messages to first 200 chars
                String trimmed = message.trim();
                if (trimmed.length() > 200) {
                    trimmed = trimmed.substring(0, 200) + "...";
                }
                result.append("  Message: ").append(trimmed).append("\n");
            }

            // Extract stack trace from CDATA — top 5 lines only (framework frames)
            String stackTrace = extractCdataContent(methodBlock, "<full-stacktrace>");
            if (stackTrace != null) {
                String[] lines = stackTrace.trim().split("\n");
                result.append("  StackTrace (top ").append(Math.min(lines.length, 5)).append(" lines):\n");
                for (int i = 0; i < Math.min(lines.length, 5); i++) {
                    result.append("    ").append(lines[i].trim()).append("\n");
                }
            }

            result.append("\n");
            searchFrom = failIndex + 1;
        }

        if (failureCount == 0) {
            result.append("  [No failure blocks found in XML]\n");
        }

        logger.info("Extracted {} failure block(s) from XML", failureCount);
        return result.toString();
    }

    /**
     * Filters automation log to only relevant lines: ERROR, WARN, and lines
     * containing the names of failed tests.
     *
     * WHY FILTER instead of raw tail:
     * - Raw tail of 15K chars is 90% noise (DriverManager init, BrowserOptionsFactory, etc.)
     * - ERROR/WARN lines contain the actual failure signals (RetryAnalyzer decisions)
     * - Lines mentioning failed test names provide execution context
     * - Filtered output is typically 10-50 lines vs 200+ lines of noise
     *
     * @param logPath Path to automation.log
     * @param structuredFailures The extracted failure block text (used to find test names)
     * @return Filtered log lines containing only signal
     */
    private String filterRelevantLogs(Path logPath, String structuredFailures) {
        if (!Files.exists(logPath)) {
            logger.warn("Automation log not found at {}. Analysis will use XML only.", logPath);
            return "[No log file found]";
        }

        try {
            List<String> allLines = Files.readAllLines(logPath);
            List<String> relevantLines = new ArrayList<>();

            // Extract failed test names from the structured failures block
            List<String> failedTestNames = extractFailedTestNames(structuredFailures);

            for (String line : allLines) {
                // Keep ERROR and WARN lines — these are the actual failure signals
                if (line.contains(" ERROR ") || line.contains(" WARN ")) {
                    relevantLines.add(line);
                    continue;
                }

                // Keep lines that mention any failed test name
                for (String testName : failedTestNames) {
                    if (line.contains(testName)) {
                        relevantLines.add(line);
                        break;
                    }
                }
            }

            // Cap at MAX_LOG_LINES to stay within token limits — take the tail
            if (relevantLines.size() > MAX_LOG_LINES) {
                relevantLines = relevantLines.subList(
                        relevantLines.size() - MAX_LOG_LINES, relevantLines.size()
                );
            }

            logger.info("Log filtered: {} total lines → {} relevant lines",
                    allLines.size(), relevantLines.size());

            if (relevantLines.isEmpty()) {
                return "[No ERROR/WARN log entries found]";
            }

            return String.join("\n", relevantLines);
        } catch (IOException e) {
            logger.error("Failed to read log file: {}", logPath, e);
            return "[Error reading log file: " + e.getMessage() + "]";
        }
    }

    /**
     * Extracts test names from the structured failures block.
     * Looks for lines like "  Test: testPurchaseWithInvalidData"
     */
    private List<String> extractFailedTestNames(String structuredFailures) {
        List<String> names = new ArrayList<>();
        for (String line : structuredFailures.split("\n")) {
            if (line.trim().startsWith("Test: ")) {
                String name = line.trim().substring("Test: ".length()).trim();
                if (!name.equals("unknown")) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    /**
     * Extracts an XML attribute value following the given start marker.
     * Reads until the next double-quote character.
     * Example: extractXmlValue(tag, "name=\"") extracts the name attribute value.
     */
    private String extractXmlValue(String xml, String startMarker) {
        int start = xml.indexOf(startMarker);
        if (start == -1) return null;
        start += startMarker.length();
        int end = xml.indexOf("\"", start);
        if (end == -1) return null;
        return xml.substring(start, end);
    }

    /**
     * Extracts CDATA content after a given XML tag.
     * Example: extractCdataContent(block, "<message>") extracts the message CDATA.
     */
    private String extractCdataContent(String xml, String tag) {
        int tagStart = xml.indexOf(tag);
        if (tagStart == -1) return null;

        int cdataStart = xml.indexOf("<![CDATA[", tagStart);
        if (cdataStart == -1) return null;
        cdataStart += "<![CDATA[".length();

        int cdataEnd = xml.indexOf("]]>", cdataStart);
        if (cdataEnd == -1) return null;

        return xml.substring(cdataStart, cdataEnd);
    }

    /**
     * Writes the AI analysis to a markdown report file.
     * Adds a header with timestamp and test counts for context.
     *
     * @return Path to the written report file
     */
    private Path writeReport(String analysis, int total, int passed, int failed, int skipped) {
        Path reportPath = Paths.get("target", "failure-analysis-report.md");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String report = """
            # AI Failure Analysis Report
            
            **Generated:** %s
            **Test Results:** %d total | %d passed | %d failed | %d skipped
            
            ---
            
            %s
            """.formatted(timestamp, total, passed, failed, skipped, analysis);

        try {
            Files.writeString(reportPath, report);
            logger.info("Report written to: {}", reportPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to write report: {}", reportPath, e);
        }

        return reportPath;
    }
}
