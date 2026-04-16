package com.demo.flightbooking.ai;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class TestSummaryRunner {

    private static final Logger logger = LogManager.getLogger(TestSummaryRunner.class);

    public static void main(String[] args) {

        logger.info("=== AI Test Summary Runner ===");

        try {
            AiTestSummaryGenerator generator = new AiTestSummaryGenerator();
            Path report = generator.generateSummary();

            if (report != null) {
                logger.info("Summary generated: {}", report.toAbsolutePath());
            }

        } catch (Exception e) {
            logger.error("Summary generation failed", e);
        }
    }
}