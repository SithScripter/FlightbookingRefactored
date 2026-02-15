package com.demo.flightbooking.tests.booking;

import com.demo.flightbooking.pages.ConfirmationPage;
import com.demo.flightbooking.utils.*;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.aventstack.extentreports.ExtentTest;
import com.demo.flightbooking.model.Passenger;
import com.demo.flightbooking.pages.FlightSelectionPage;
import com.demo.flightbooking.pages.HomePage;
import com.demo.flightbooking.pages.PurchasePage;
import com.demo.flightbooking.tests.base.BaseTest;

/**
 * Contains the end-to-end test case for successfully booking a flight.
 * This test class demonstrates the complete user flow from searching for a
 * flight
 * to receiving a booking confirmation.
 */
public class EndToEndBookingTest extends BaseTest {

    /**
     * Verifies the successful end-to-end booking of a flight using data from a JSON
     * file.
     * The test is data-driven, meaning it will run once for each passenger object
     * provided by the JsonDataProvider.
     *
     * @param passenger A Passenger object containing all necessary data for one
     *                  test run.
     */
    @Test(dataProvider = "passengerData", dataProviderClass = JsonDataProvider.class, groups = { "regression", "smoke"},
            testName = "Verify successful end-to-end booking using data from JSON")
    public void testEndToEndBookingFromJson(Passenger passenger) {
        WebDriver driver = DriverManager.getDriver();
        driver.get(ConfigReader.getApplicationUrl());
        ExtentTest test = ExtentManager.getTest();

        if (test != null) {
            test.info("Navigated to: " + ConfigReader.getApplicationUrl());
            test.info("Attempting booking for passenger (JSON): " + passenger.firstName() + " " + passenger.lastName() +
                    " from " + passenger.origin() + " to " + passenger.destination());
        }
        logger.info("Starting flight booking (JSON) for passenger: {} {} from {} to {}",
                passenger.firstName(), passenger.lastName(), passenger.origin(), passenger.destination());

        HomePage homePage = new HomePage(driver);
        Assert.assertTrue(homePage.isHomePageDisplayed(), "Home page is not displayed!");
        homePage.findFlights(passenger.origin(), passenger.destination());

        FlightSelectionPage flightSelectionPage = new FlightSelectionPage(driver);
        Assert.assertTrue(flightSelectionPage.isFlightSelectionPageDisplayed(),"Flight Selection Page is not displayed!");
        flightSelectionPage.clickChooseFlightButton();

        PurchasePage purchasePage = new PurchasePage(driver);
        Assert.assertTrue(purchasePage.isPurchasePageDisplayed(), "Purchase Page is not displayed!");
        purchasePage.fillPurchaseForm(passenger);
        purchasePage.clickPurchaseFlightButton();

        ConfirmationPage confirmationPage = new ConfirmationPage(driver);
        Assert.assertTrue(confirmationPage.isConfirmationPageDisplayed(), "Confirmation page is not displayed!");

        String thankYouMessage = confirmationPage.getThankYouMessage();
        Assert.assertEquals(thankYouMessage, "Thank you for your purchase today!","Thank you message mismatch!");

        String confirmationId = confirmationPage.getConfirmationId();
        Assert.assertNotNull(confirmationId, "Confirmation ID should not be null!");
        Assert.assertFalse(confirmationId.isEmpty(), "Confirmation ID should not be empty!");

        String totalAmount = confirmationPage.getTotalAmount();
        Assert.assertNotNull(totalAmount, "Total amount should not be null!");
        Assert.assertFalse(totalAmount.isEmpty(), "Total amount should not be empty!");

        // Capture SUCCESS screenshot with confirmation ID for audit trail
        String screenshotPath = ScreenshotUtils.captureScreenshot(driver, "booking_success_" + confirmationId);

        if (test != null) {
            test.pass("Flight booking (JSON) successful for: " + passenger.firstName() + " " + passenger.lastName());
            test.info("Confirmation ID: " + confirmationId);
            test.info("Total Amount: " + totalAmount);
            test.addScreenCaptureFromPath("../screenshots/" + new java.io.File(screenshotPath).getName());
        }
        logger.info("Flight booking (JSON) completed for passenger: {} {}", passenger.firstName(),
                passenger.lastName());
    }

    /**
     * Verifies flight search for all valid routes from CSV data.
     * This test is data-driven, running for each route pair in routes.csv.
     *
     * @param departureCity   The departure city.
     * @param destinationCity The destination city.
     */
    @Test(dataProvider = "routesData", dataProviderClass = CsvDataProvider.class, groups = { "regression",
            "smoke" }, testName = "Verify flight search for all valid routes from CSV")
    public void testAllValidRoutesFromCsv(String departureCity, String destinationCity) {
        WebDriver driver = DriverManager.getDriver();
        driver.get(ConfigReader.getApplicationUrl());
        ExtentTest test = ExtentManager.getTest();

        if (test != null) {
            test.info("Testing route: " + departureCity + " to " + destinationCity);
        }
        logger.info("Starting flight search for route: {} to {}", departureCity, destinationCity);

        HomePage homePage = new HomePage(driver);
        Assert.assertTrue(homePage.isHomePageDisplayed(), "Home page is not displayed!");
        homePage.findFlights(departureCity, destinationCity);

        FlightSelectionPage flightSelectionPage = new FlightSelectionPage(driver);
        Assert.assertTrue(flightSelectionPage.isFlightSelectionPageDisplayed(),
                "Flight Selection Page is not displayed for route " + departureCity + " to " + destinationCity);

        if (test != null) {
            test.pass("Flight search (CSV) successful for route: " + departureCity + " to " + destinationCity);
        }
        logger.info("Flight search (CSV) completed for route: {} to {}", departureCity, destinationCity);
    }
}