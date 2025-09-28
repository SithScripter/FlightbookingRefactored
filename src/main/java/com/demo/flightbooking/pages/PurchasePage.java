package com.demo.flightbooking.pages;

import com.demo.flightbooking.model.Passenger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

/**
 * Represents the Purchase Page of the BlazeDemo application.
 * This is the final step where the user enters their personal and payment
 * information to complete the booking.Encapsulates all elements and actions available on this page.
 */
public class PurchasePage extends BasePage {

    // Locators for the purchase form elements
    private final By firstNameInput = By.id("inputName");
    private final By addressInput = By.id("address");
    private final By cityInput = By.id("city");
    private final By stateInput = By.id("state");
    private final By zipCodeInput = By.id("zipCode");
    private final By cardTypeSelect = By.id("cardType");
    private final By creditCardNumberInput = By.id("creditCardNumber");
    private final By creditCardMonthInput = By.id("creditCardMonth");
    private final By creditCardYearInput = By.id("creditCardYear");
    private final By nameOnCardInput = By.id("nameOnCard");
    private final By rememberMeCheckbox = By.id("rememberMe");
    private final By purchaseFlightButton = By.xpath("//input[@value='Purchase Flight']");

    /**
     * Constructor for the PurchasePage.
     * @param driver The WebDriver instance.
     */
    public PurchasePage(WebDriver driver) {
        super(driver);
    }

    // --- High-Level Service Method ---

    /**
     * Fills the entire purchase form using data from a Passenger record.
     * This encapsulates the low-level details of filling each field.
     *
     * @param passenger The Passenger record containing all necessary data.
     */
    public void fillPurchaseForm(Passenger passenger) {
        // --- CHANGE: From Getters to Record Accessors ---
        // We now use the direct accessor methods provided by the record.
        logger.info("Filling purchase form for passenger: {}", passenger.firstName());
        enterFirstName(passenger.firstName());
        enterAddress(passenger.address());
        enterCity(passenger.city());
        enterState(passenger.state());
        enterZipCode(passenger.zipCode());
        selectCardType(passenger.cardType());
        enterCardNumber(passenger.cardNumber());
        enterMonth(passenger.month());
        enterYear(passenger.year());
        enterNameOnCard(passenger.cardName());
        tickRememberMeCheckbox();
    }


    // --- Low-Level Action Methods ---

    public void enterFirstName(String firstName) {
        driver.findElement(firstNameInput).sendKeys(firstName);
    }

    public void enterAddress(String address) {
        driver.findElement(addressInput).sendKeys(address);
    }

    public void enterCity(String city) {
        driver.findElement(cityInput).sendKeys(city);
    }

    public void enterState(String state) {
        driver.findElement(stateInput).sendKeys(state);
    }

    public void enterZipCode(String zipCode) {
        driver.findElement(zipCodeInput).sendKeys(zipCode);
    }

    public void selectCardType(String cardType) {
        Select select = new Select(driver.findElement(cardTypeSelect));
        select.selectByVisibleText(cardType);
    }

    public void enterCardNumber(String cardNumber) {
        driver.findElement(creditCardNumberInput).sendKeys(cardNumber);
    }

    public void enterMonth(String month) {
        driver.findElement(creditCardMonthInput).clear();
        driver.findElement(creditCardMonthInput).sendKeys(month);
    }

    public void enterYear(String year) {
        driver.findElement(creditCardYearInput).clear();
        driver.findElement(creditCardYearInput).sendKeys(year);
    }

    public void enterNameOnCard(String name) {
        driver.findElement(nameOnCardInput).sendKeys(name);
    }

    public void tickRememberMeCheckbox() {
        driver.findElement(rememberMeCheckbox).click();
    }

    /**
     * Clicks the "Purchase Flight" button to submit the form and complete the booking.
     */
    public void clickPurchaseFlightButton() {
        logger.info("Clicking on 'Purchase Flight' button");
        driver.findElement(purchaseFlightButton).click();
    }
}