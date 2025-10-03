package com.demo.flightbooking.listeners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.demo.flightbooking.utils.DriverManager;
import com.demo.flightbooking.utils.ScreenshotUtils;
import org.openqa.selenium.WebDriver;
import org.testng.IAnnotationTransformer;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.demo.flightbooking.utils.ExtentManager;

/**
 * A TestNG listener class that implements ITestListener.
 * Listeners allow you to execute custom code in response to TestNG events,
 * such as when a test starts, passes, or fails. This is useful for custom
 * logging, reporting, or integrating with other tools.
 */
public class TestListener implements ITestListener, IAnnotationTransformer {

	@Override
	@SuppressWarnings({ "rawtypes"})
	public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
	    annotation.setRetryAnalyzer(RetryAnalyzer.class);
	}


	// ... other onTestStart, onTestSuccess methods remain the same

	@Override
	public void onTestStart(ITestResult result) {
		ExtentTest test = ExtentManager.getTest();
		if (test != null) {
			test.log(Status.INFO, "Test Started: " + result.getMethod().getMethodName());
		}
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		ExtentTest test = ExtentManager.getTest();
		if (test != null) {
			test.log(Status.PASS, "Test Passed: " + result.getMethod().getMethodName());
		}
	}

	@Override
	public void onTestFailure(ITestResult result) {
		// Check if this test has a retry analyzer and if it will retry
		if (result.getMethod().getRetryAnalyzer(result) != null) {
			try {
				// Get the retry analyzer from our RetryAnalyzer class
				com.demo.flightbooking.listeners.RetryAnalyzer retryAnalyzer =
					(com.demo.flightbooking.listeners.RetryAnalyzer) result.getMethod().getRetryAnalyzer(result);

				// If the analyzer will retry, suppress this failure report
				if (retryAnalyzer.retry(result)) {
					// Don't log to ExtentReports for intermediate failures
					return; // Exit early - don't process this failure
				}
			} catch (ClassCastException e) {
				// If it's not our retry analyzer, continue with normal processing
			}
		}

		// Only process final failures (after all retries exhausted)
		ExtentTest test = ExtentManager.getTest();
		if (test != null) {
			test.log(Status.FAIL, "Test failed: " + result.getThrowable());
		}
		try {
			WebDriver driver = DriverManager.getDriver(); // use thread-local driver
			if (driver != null) {
				String screenshotPath =
						ScreenshotUtils.captureScreenshot(driver, result.getMethod().getMethodName());
				if (test != null) {
					test.addScreenCaptureFromPath("../screenshots/" + new java.io.File(screenshotPath).getName());
				}
			} else if (test != null) {
				test.log(Status.WARNING, "Driver was null; skipping screenshot.");
			}
		} catch (Exception e) {
			if (test != null) {
				test.log(Status.WARNING, "Screenshot capture failed: " + e.getMessage());
			}
		}
//		}
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		ExtentTest test = ExtentManager.getTest();
		if (test != null) {
			if (result.wasRetried()) {
				test.log(Status.WARNING, "Test Retried: " + result.getMethod().getMethodName());
			} else {
				test.log(Status.SKIP, "Test Skipped: " + result.getMethod().getMethodName());
			}
		}
	}
}