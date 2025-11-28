package com.demo.flightbooking.tests.base;

import com.demo.flightbooking.utils.ExtentManager;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

/**
 * A specialized base class for API tests.
 * It extends the standard BaseTest but overrides the tearDown method
 * to prevent WebDriver-specific cleanup for non-UI tests.
 */
public class ApiBaseTest extends BaseTest {

    @BeforeMethod(alwaysRun = true)
    @Override
    public void setUp(Method method) {
        // This override is intentionally left empty.
        // API tests do not require the WebDriver setup from the parent BaseTest.
        // We still need the method to be present to satisfy the TestNG lifecycle.
    }

    @AfterMethod(alwaysRun = true)
    @Override
    public void tearDown(ITestResult result) {
        // For API tests, we only unload the ExtentTest context.
        // We do not call DriverManager.quitDriver() because no WebDriver was used.
        if (result.getStatus() == ITestResult.FAILURE) {
            String failureMsg = "❌ " + result.getMethod().getMethodName()
                    + " FAILED: " + result.getThrowable().getMessage().split("\n")[0];
            failureSummaries.add(failureMsg);
            logger.error("❌ API Test failed: {}", result.getMethod().getMethodName());
        }

        ExtentManager.unload();
    }
}
