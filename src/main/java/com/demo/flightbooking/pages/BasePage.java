package com.demo.flightbooking.pages;

import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.demo.flightbooking.utils.ConfigReader;

/**
 * Represents the base class for all Page Objects in the framework.
 * It initializes the WebDriver, WebDriverWait, and logger, and provides common
 * page functionalities that can be inherited by all specific page classes.
 */
public abstract class BasePage {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected final Logger logger; // Logger for each page object

    private static final int DEFAULT_TIMEOUT = 10; // Default timeout if not specified in config

    /**
     * Constructor for the BasePage.
     * It initializes the WebDriver and WebDriverWait for the page, and it also
     * initializes all WebElements annotated with @FindBy using the PageFactory.
     *
     * @param driver The WebDriver instance to be used by the page.
     */

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        // Initialize logger with the specific class name of the concrete page object
        this.logger = LogManager.getLogger(this.getClass());
        // Initialize WebDriverWait using the timeout from config.properties
        // If "test.timeout" is not found or invalid, it will default to 10 seconds.
        int timeoutSeconds = ConfigReader.getPropertyAsInt("test.timeout");
        if (timeoutSeconds <= 0) {
            timeoutSeconds = DEFAULT_TIMEOUT; // Use default if config value is invalid or not found
            logger.warn("Invalid or missing 'test.timeout' in config.properties. Using default timeout: {} seconds.",
                    DEFAULT_TIMEOUT);
        }
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

        // Initialize PageFactory elements (if using @FindBy annotations)
        // PageFactory.initElements(driver, this); // Uncomment if you plan to use
        // @FindBy
    }

    /**
     * Gets the title of the current page.
     *
     * @return A string representing the page title.
     */
    public String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Get the current URL.
     *
     * @return The current URL.
     */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

}