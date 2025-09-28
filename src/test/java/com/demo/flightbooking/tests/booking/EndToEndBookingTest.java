package com.demo.flightbooking.tests.booking;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.aventstack.extentreports.ExtentTest;
import com.demo.flightbooking.model.Passenger;
import com.demo.flightbooking.pages.FlightSelectionPage;
import com.demo.flightbooking.pages.HomePage;
import com.demo.flightbooking.pages.PurchasePage;
import com.demo.flightbooking.tests.base.BaseTest;
import com.demo.flightbooking.utils.ConfigReader;
import com.demo.flightbooking.utils.CsvDataProvider;
import com.demo.flightbooking.utils.DriverManager;
import com.demo.flightbooking.utils.ExtentManager;
import com.demo.flightbooking.utils.JsonDataProvider;
import com.demo.flightbooking.utils.WebDriverUtils;

/**
 * Contains the end-to-end test case for successfully booking a flight.
 * This test class demonstrates the complete user flow from searching for a flight
 * to receiving a booking confirmation.
 */
public class EndToEndBookingTest extends BaseTest {

    /**
     * Verifies the successful end-to-end booking of a flight using data from a JSON file.
     * The test is data-driven, meaning it will run once for each passenger object
     * provided by the JsonDataProvider.
     *
     * @param passenger A Passenger object containing all necessary data for one test run.
     */
    @Test(
            dataProvider = "passengerData", 
            dataProviderClass = JsonDataProvider.class,
            groups = {"regression", "smoke", "passenger_booking"},
            testName = "Verify successful end-to-end booking using data from JSON"
        )
    public void testEndToEndBookingFromJson(Passenger passenger) {
        WebDriver driver = DriverManager.getDriver();
        WebDriverUtils webDriverUtils = new WebDriverUtils(driver, ConfigReader.getPropertyAsInt("test.timeout"));
        driver.get(ConfigReader.getApplicationUrl());
        ExtentTest test = ExtentManager.getTest();

        if (test != null) {
            // --- CHANGE: Using record accessors passenger.firstName() instead of passenger.getFirstName() ---
            test.info("Navigated to: " + ConfigReader.getApplicationUrl());
            test.info("Attempting booking for passenger (JSON): " + passenger.firstName() + " " + passenger.lastName() +
                      " from " + passenger.origin() + " to " + passenger.destination());
        }
        logger.info("Starting flight booking (JSON) for passenger: {} {} from {} to {}",
                    passenger.firstName(), passenger.lastName(), passenger.origin(), passenger.destination());

        HomePage homePage = new HomePage(driver);
        homePage.findFlights(passenger.origin(), passenger.destination());

        boolean urlContainsReserve = webDriverUtils.waitUntilUrlContains("/reserve.php");
        Assert.assertTrue(urlContainsReserve, "Did not navigate to reserve page!");

        FlightSelectionPage flightSelectionPage = new FlightSelectionPage(driver);
        flightSelectionPage.clickChooseFlightButton();

        boolean urlContainsPurchase = webDriverUtils.waitUntilUrlContains("/purchase.php");
        Assert.assertTrue(urlContainsPurchase, "Did not navigate to purchase page!");

        PurchasePage purchasePage = new PurchasePage(driver);
        purchasePage.fillPurchaseForm(passenger);
        purchasePage.clickPurchaseFlightButton();

        boolean urlContainsConfirmation = webDriverUtils.waitUntilUrlContains("/confirmation.php");
        Assert.assertTrue(urlContainsConfirmation, "Did not navigate to confirmation page after purchase.");
        
        // 🔴 INTENTIONAL FAILURE FOR TESTING - Always fails
        Assert.assertTrue(false, "Intentional failure to test framework's failure handling, screenshots, and reporting");
        
        // Additional verification: Check that the confirmation page has loaded correctly
        Assert.assertTrue(driver.getTitle().contains("BlazeDemo"), "Confirmation page title does not match expected.");
        
        if (test != null) {
            test.pass("Flight booking (JSON) successful for: " + passenger.firstName() + " " + passenger.lastName());
        }
        logger.info("Flight booking (JSON) completed for passenger: {} {}", passenger.firstName(), passenger.lastName());
    }
}