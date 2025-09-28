package com.demo.flightbooking.tests.base;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.demo.flightbooking.utils.ConfigReader;
import com.demo.flightbooking.utils.DriverManager;
import com.demo.flightbooking.utils.ExtentManager;
import com.demo.flightbooking.utils.ScreenshotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The base class for all test classes in the framework.
 * It handles the setup and teardown of essential components like WebDriver,
 * ExtentReports, and logging, ensuring a consistent test execution lifecycle.
 */
public class BaseTest {

    protected static final Logger logger = LogManager.getLogger(BaseTest.class);

    // Thread-safe report instance for parallel execution
    private static final ThreadLocal<ExtentReports> extentReports = new ThreadLocal<>();

    // Shared list of failure summaries (thread-safe)
    protected static final List<String> failureSummaries =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * This method runs once before the entire test suite.
     * It cleans up old failure summary files.
     */
    @BeforeSuite(alwaysRun = true)
    public void setUpSuite() {
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        logger.info("✅ Logs directory ensured.");

        String suiteName = System.getProperty("test.suite", "default");
        File oldSummary = new File("reports/" + suiteName + "-failure-summary.txt");
        if (oldSummary.exists()) {
            oldSummary.delete();
            logger.info("🧹 Old failure summary for suite '{}' deleted.", suiteName);
        }
    }

    /**
     * ✅ Runs once per test class.
     * It retrieves the browser parameter from the TestNG context to ensure thread safety
     * and sets up the ExtentReports instance for the current browser.
     *
     * @param context The ITestContext provided by TestNG for the current test run.
     */
    @BeforeClass(alwaysRun = true)
    public void setUpClass(ITestContext context) {
        String browser = context.getCurrentXmlTest().getParameter("browser");
        DriverManager.setBrowser(browser);
        logger.info("✅ Browser set to: {} for test class: {}", browser.toUpperCase(), this.getClass().getSimpleName());

        String reportDir = browser.toLowerCase();
        String suiteName = System.getProperty("test.suite", "default");

        String reportPath = "reports/" + reportDir + "/";
        new File(reportPath).mkdirs();

        String reportFileName = suiteName + "-" + reportDir + "-report.html";

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath + reportFileName);
        sparkReporter.config().setOfflineMode(true);
        sparkReporter.config().setDocumentTitle("Test Report: " + suiteName.toUpperCase() + " - " + browser.toUpperCase());

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(sparkReporter);
        reports.setSystemInfo("Tester", ConfigReader.getProperty("tester.name"));
        reports.setSystemInfo("OS", System.getProperty("os.name"));
        reports.setSystemInfo("Java Version", System.getProperty("java.version"));
        extentReports.set(reports);

        logger.info("✅ Report for {} will be generated at: {}/{}", browser.toUpperCase(), reportPath, reportFileName);
    }

    /**
     * This method runs before each @Test method.
     * It initializes the WebDriver and creates a new test entry in the ExtentReport.
     *
     * @param method The test method that is about to be run.
     */
    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        DriverManager.getDriver();
        String browserName = DriverManager.getBrowser().toUpperCase();
        logger.info("🚀 WebDriver initialized for test: {} on {}", method.getName(), browserName);

        ExtentTest test = extentReports.get().createTest(method.getName() + " - " + browserName);
        ExtentManager.setTest(test);
        logger.info("📝 ExtentTest created for: {} on {}", method.getName(), browserName);
    }

    /**
     * This method runs after each @Test method.
     * It handles test result logging, takes screenshots on failure, and quits the WebDriver.
     *
     * @param result The result of the test method that has just run.
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        WebDriver driver = DriverManager.getDriver(); // Get driver before quitting

        if (test != null) {
            if (result.getStatus() == ITestResult.FAILURE) {
                String failureMsg = "❌ " + result.getMethod().getMethodName()
                        + " FAILED on " + DriverManager.getBrowser().toUpperCase()
                        + ": " + result.getThrowable().getMessage().split("\n")[0];
                failureSummaries.add(failureMsg);

                String screenshotPath = ScreenshotUtils.captureScreenshot(driver, result.getMethod().getMethodName());
                test.addScreenCaptureFromPath("../screenshots/" + new File(screenshotPath).getName());
                test.fail(result.getThrowable());
                logger.error("❌ Test failed: {} | Screenshot: {}", result.getMethod().getMethodName(), screenshotPath);
            } else if (result.getStatus() == ITestResult.SUCCESS) {
                test.log(Status.PASS, "✅ Test passed");
            } else if (result.getStatus() == ITestResult.SKIP) {
                test.log(Status.SKIP, "🚫 Test skipped");
            }
        }

        DriverManager.quitDriver();
        logger.info("🧹 WebDriver quit for test: {}", result.getMethod().getMethodName());
        ExtentManager.unload();
    }

    /**
     * ✅ Runs once after all methods in a class have run.
     * Flushes the report and copies it for Jenkins display.
     */
    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        if (extentReports.get() != null) {
            extentReports.get().flush();
            logger.info("✅ ExtentReports flushed for browser: {}", DriverManager.getBrowser().toUpperCase());
        }

        String browser = DriverManager.getBrowser().toLowerCase();
        String suiteName = System.getProperty("test.suite", "default");
        String reportPath = "reports/" + browser + "/";
        String reportFileName = suiteName + "-" + browser + "-report.html";

        try {
            Path source = Paths.get(reportPath + reportFileName);
            Path target = Paths.get(reportPath + "index.html");
            if (Files.exists(source)) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                logger.info("📄 Report copied to index.html for {} display.", browser.toUpperCase());
            }
        } catch (IOException e) {
            logger.error("❌ Failed to copy report to index.html for {}", browser.toUpperCase(), e);
        }
    }

    /**
     * This method runs once after the entire test suite has finished.
     * It writes the consolidated failure summary to a file.
     */
    @AfterSuite(alwaysRun = true)
    public void tearDownSuite() {
        String suiteName = System.getProperty("test.suite", "default");
        String mergedSummaryFile = "reports/" + suiteName + "-failure-summary.txt";

        if (!failureSummaries.isEmpty()) {
            try (PrintWriter out = new PrintWriter(new FileWriter(mergedSummaryFile, false))) {
                failureSummaries.forEach(out::println);
                logger.info("📄 Consolidated failure summary written to: {}", mergedSummaryFile);
            } catch (IOException e) {
                logger.error("❌ Failed to write consolidated failure summary.", e);
            }
        }
    }
}