package com.demo.flightbooking.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * CLI entry point for demonstrating the AI Failure Analyzer (Phase 4).
 *
 * Same pattern as AiDemo (Phase 3):
 * - Simple main() method for running from IDE or CLI
 * - Shows the full pipeline: XML + logs → mask → LLM → report
 * - NOT a test class — this is a demonstration tool
 *
 * PREREQUISITES:
 * 1. Run tests first to generate surefire XML and logs:
 *    mvn test (or run from IDE — some tests are designed to fail)
 * 2. Ensure Ollama is running: ollama serve
 * 3. Ensure model is available: ollama pull llama3.2
 *
 * USAGE:
 * - From IDE: Right-click → Run FailureAnalysisDemo.main()
 * - From CLI: mvn exec:java -Dexec.mainClass="com.demo.flightbooking.utils.FailureAnalysisDemo"
 *
 * OUTPUT:
 * - Generates target/failure-analysis-report.md
 * - Console shows pipeline progress and summary
 */
public class FailureAnalysisDemo {

    private static final Logger logger = LogManager.getLogger(FailureAnalysisDemo.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("  AI Failure Analysis Demo (Phase 4)");
        logger.info("========================================");

        try {
            AiFailureAnalyzer analyzer = new AiFailureAnalyzer();
            Path reportPath = analyzer.analyze();

            if (reportPath != null) {
                logger.info("========================================");
                logger.info("  Report generated: {}", reportPath.toAbsolutePath());
                logger.info("  Open in any markdown viewer to read.");
                logger.info("========================================");
            } else {
                logger.info("========================================");
                logger.info("  No report generated.");
                logger.info("  Either no failures found, or prerequisites missing.");
                logger.info("  Run 'mvn test' first to generate test results.");
                logger.info("========================================");
            }
        } catch (Exception e) {
            logger.error("Demo failed: {}", e.getMessage(), e);
            logger.info("========================================");
            logger.info("  Troubleshooting:");
            logger.info("  1. Is Ollama running? → ollama serve");
            logger.info("  2. Is model available? → ollama pull llama3.2");
            logger.info("  3. Have tests been run? → mvn test");
            logger.info("========================================");
        }
    }
}
