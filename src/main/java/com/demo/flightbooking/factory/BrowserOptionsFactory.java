package com.demo.flightbooking.factory;

import com.demo.flightbooking.enums.BrowserType;
import com.demo.flightbooking.utils.ConfigReader;

import io.github.bonigarcia.wdm.WebDriverManager; // Import WebDriverManager
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * A factory class for creating browser-specific configurations (Options).
 * It centralizes the logic for setting up browser capabilities, such as
 * headless mode,
 * and uses WebDriverManager to automatically handle browser driver executables.
 */

public class BrowserOptionsFactory {

    private static final Logger logger = LogManager.getLogger(BrowserOptionsFactory.class);

    /**
     * Gets the browser-specific capabilities.
     * It sets up the appropriate driver using WebDriverManager and configures
     * options like headless mode.
     *
     * @param browserType The type of browser (e.g., CHROME, FIREFOX).
     * @param isHeadless  A boolean flag to indicate if the browser should run in
     *                    headless mode.
     * @return A MutableCapabilities object with the browser-specific settings.
     */

    public static MutableCapabilities getOptions(BrowserType browserType, boolean isHeadless) {

        isHeadless = Boolean.parseBoolean(ConfigReader.getProperty("browser.headless", "false"));
        logger.info("Headless mode for {}: {}", browserType, isHeadless);

        boolean useGrid = Boolean.parseBoolean(ConfigReader.getProperty("selenium.grid.enabled", "true"));

        switch (browserType) {
            case CHROME:
                if (!useGrid) {
                    WebDriverManager.chromedriver().setup();
                }
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--start-maximized");
                chromeOptions.addArguments("--disable-gpu");
                chromeOptions.addArguments("--remote-allow-origins=*");

                if (isHeadless) {
                    logger.info("✅ Enabling headless mode for CHROME");
                    chromeOptions.addArguments("--headless=new");
                    chromeOptions.addArguments("--window-size=1920,1080");
                }

                return chromeOptions;

            case FIREFOX:
                if (!useGrid) {
                    WebDriverManager.firefoxdriver().setup();
                }
                FirefoxOptions firefoxOptions = new FirefoxOptions();

                if (isHeadless) {
                    logger.info("✅ Enabling headless mode for FIREFOX");
                    firefoxOptions.addArguments("--headless");
                    firefoxOptions.addArguments("--width=1920");
                    firefoxOptions.addArguments("--height=1080");
                }
                return firefoxOptions;

            case EDGE:
                if (!useGrid) {
                    WebDriverManager.edgedriver().setup();
                }
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--start-maximized");
                edgeOptions.addArguments("--inprivate");

                if (isHeadless) {
                    logger.info("✅ Enabling headless mode for EDGE");
                    edgeOptions.addArguments("--headless=new");
                    edgeOptions.addArguments("--window-size=1920,1080");
                }

                return edgeOptions;

            default:
                throw new IllegalArgumentException("Unsupported browser type provided: " + browserType);
        }
    }
}