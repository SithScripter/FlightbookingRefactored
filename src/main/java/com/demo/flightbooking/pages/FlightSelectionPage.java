package com.demo.flightbooking.pages;

import java.util.Optional;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.demo.flightbooking.utils.ConfigReader;
import com.demo.flightbooking.utils.WebDriverUtils;

/**
 * Represents the Flight Selection Page of the BlazeDemo application.
 * This page appears after a user has searched for flights, and it lists
 * the available options.
 */
public class FlightSelectionPage extends BasePage {

    // --- Locators ---
    private final By chooseFlightButton = By.cssSelector("input[type='submit']");
    // Locator for all the rows in the flight table
    private final By flightRows = By.xpath("//table[@class='table']/tbody/tr");
    // Locator for the price cell within a flight row (it's the 6th column: <td>)
    private final By priceCell = By.xpath("./td[6]");

    // WebDriverUtils instance for robust interactions
    private final WebDriverUtils webDriverUtils;

    /**
     * Constructor for the FlightSelectionPage.
     * 
     * @param driver The WebDriver instance.
     */
    public FlightSelectionPage(WebDriver driver) {
        super(driver); // Call BasePage constructor
        this.webDriverUtils = new WebDriverUtils(driver, ConfigReader.getPropertyAsInt("test.timeout"));
        logger.info("FlightSelectionPage initialized.");
    }

    // --- Action Methods ---

    /**
     * Verifies that the Flight Selection page is displayed by checking
     * if the flight table is visible.
     *
     * @return true if the flight table is displayed.
     */
    public boolean isFlightSelectionPageDisplayed() {
        boolean isDisplayed = webDriverUtils.isElementDisplayed(flightRows);
        logger.info("Flight Selection page displayed: {}", isDisplayed);
        return isDisplayed;
    }

    /**
     * Clicks the "Choose This Flight" button to select the first available flight.
     */
    public void clickChooseFlightButton() {
        logger.info("Clicking Choose This Flight button.");
        webDriverUtils.waitForPageLoad();
        webDriverUtils.click(chooseFlightButton);
        logger.info("Choose Flight button clicked.");
    }

    // --- NEW STREAM-BASED DATA PROCESSING METHOD ---

    /**
     * Gets the lowest flight price from the results table using Java Streams.
     * This method demonstrates a clean, declarative way to process a collection of
     * WebElements.
     *
     * @return An Optional<Double> containing the lowest price, or an empty Optional
     *         if no prices are found.
     */
    public Optional<Double> getLowestFlightPrice() {
        logger.info("Finding the lowest flight price on the page using Streams.");
        return driver.findElements(flightRows).stream() // 1. Get a stream of all flight row <tr> WebElements.
                .map(row -> row.findElement(priceCell).getText()) // 2. For each row, find its price cell and get the
                                                                  // text (e.g., "$472.56").
                .map(priceText -> priceText.replace("$", "")) // 3. For each price string, remove the '$' character.
                .map(Double::parseDouble) // 4. Convert the clean string (e.g., "472.56") into a Double.
                .min(Double::compare); // 5. Use the min() terminal operation to find the smallest Double in the
                                       // stream.
    }
}
