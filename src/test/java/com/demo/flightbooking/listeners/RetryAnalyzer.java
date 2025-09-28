package com.demo.flightbooking.listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

// Import ConfigReader from the utils package
import com.demo.flightbooking.utils.ConfigReader;

/**
 * An implementation of TestNG's IRetryAnalyzer interface.
 * This class is used to automatically re-run a failed test a certain number of times.
 * This can be useful for handling flaky tests that might fail due to intermittent
 * environmental or network issues.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int retryCount = 0;
    
    // Read the max retry count from the config file.
    private static final int maxRetryCount = ConfigReader.getPropertyAsInt("test.retry.maxcount");

    /**
     * This method is called by TestNG when a test fails.
     * It decides whether the test should be retried.
     *
     * @param result The result of the test that has just failed.
     * @return True if the test should be retried, false otherwise.
     */
    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < maxRetryCount) {
            retryCount++;
            return true; // Return true to signal TestNG to retry the test
        }
        return false; // Return false to stop retrying
    }
}