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
					// Use path relative to workspace root for both Jenkins HTML Publisher and artifacts
					test.addScreenCaptureFromPath("reports/screenshots/" + new java.io.File(screenshotPath).getName());
				}
			} else if (test != null) {
				test.log(Status.WARNING, "Driver was null; skipping screenshot.");
			}
		} catch (Exception e) {
			if (test != null) {
				test.log(Status.WARNING, "Screenshot capture failed: " + e.getMessage());
			}
		}

//		ExtentTest test = ExtentManager.getTest();
//		if (test != null) {
//			test.log(Status.FAIL, "Test Failed: " + result.getMethod().getMethodName());
//			test.log(Status.FAIL, result.getThrowable());
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