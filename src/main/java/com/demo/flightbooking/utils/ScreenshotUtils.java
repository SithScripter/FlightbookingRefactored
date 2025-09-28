package com.demo.flightbooking.utils;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A utility class for capturing screenshots of the browser.
 * This is crucial for debugging test failures, as it provides a visual
 * record of the application's state at the moment of failure.
 */
public class ScreenshotUtils {

    private static final String SCREENSHOT_DIR = "reports/screenshots/";

    /**
     * Captures a screenshot of the current browser window and saves it to a file.
     * The screenshot is saved in the 'reports/screenshots' directory with a
     * unique name based on the test name and a timestamp.
     *
     * @param driver   The WebDriver instance.
     * @param testName The name of the test for which the screenshot is being taken.
     * @return The absolute path to the saved screenshot file, or null if an error occurred.
     */
    public static String captureScreenshot(WebDriver driver, String testName) {
        // Create folder if it doesn’t exist
        File dir = new File(SCREENSHOT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = testName + "_" + timestamp + ".png";
        String relativePath = "screenshots/" + fileName;
        String fullPath = SCREENSHOT_DIR + fileName;

        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(srcFile, new File(fullPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Only return the relative path
        return relativePath.replace("\\", "/"); // important for Windows
    }
}
