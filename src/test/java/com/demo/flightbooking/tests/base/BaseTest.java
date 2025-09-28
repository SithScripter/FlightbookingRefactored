package com.demo.flightbooking.tests.base;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.demo.flightbooking.utils.ConfigReader;
import com.demo.flightbooking.utils.DriverManager;
import com.demo.flightbooking.utils.ExtentManager;
import com.demo.flightbooking.utils.ScreenshotUtils;

/**
 * The base class for all test classes in the framework.
 * It handles the setup and teardown of essential components like WebDriver,
 * ExtentReports, and logging, ensuring a consistent test execution lifecycle.
 */
public class BaseTest {

    protected static final Logger logger = LogManager.getLogger(BaseTest.class);

    // Thread-safe report instance for parallel execution (each browser runs in isolated thread)
    private static final ThreadLocal<ExtentReports> extentReports = new ThreadLocal<>();

    // Shared list of failure summaries (thread-safe)
    protected static final List<String> failureSummaries =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * This method runs once before the entire test suite.
     * It sets up the ExtentReports instance and configures the report's appearance.
     */
    @BeforeSuite(alwaysRun = true)
    public void setUpSuite() {
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        logger.info("✅ Logs directory ensured.");

        // ✅ Read the suite name from the system property passed by Maven
        String suiteName = System.getProperty("test.suite", "default");

        // ✅ Build the filename dynamically
        File oldSummary = new File("reports/" + suiteName + "-failure-summary.txt");
        if (oldSummary.exists()) {
            oldSummary.delete();
            logger.info("🧹 Old failure summary deleted.");
        }
    }

    /**
     * ✅ Runs once per <test> tag in testng XML.
     * Creates a unique ExtentSparkReporter per browser/stage.
     */
    @Parameters("browser")
    @BeforeClass(alwaysRun = true)
    public void setUpClass(String browser) {
        // Set browser for current thread
        DriverManager.setBrowser(browser);
        logger.info("✅ Browser set to: {} for test class: {}", browser.toUpperCase(), this.getClass().getSimpleName());

        // Determine suite and report directory (e.g., chrome or firefox)
        String reportDir = System.getProperty("report.dir", browser); // fallback to browser
        String suiteName = System.getProperty("test.suite", "default");

        String reportPath = "reports/" + reportDir + "/";
        new File(reportPath).mkdirs(); // Ensure folder exists

        // 👇 File name like regression-chrome-report.html
        String reportFileName = suiteName + "-" + reportDir + "-report.html";

        // Create and configure ExtentSparkReporter
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath + reportFileName);
        sparkReporter.config().setOfflineMode(true);
        sparkReporter.config().setDocumentTitle("Test Report: " + suiteName.toUpperCase() + " - " + reportDir.toUpperCase());

        // Create new ExtentReports and attach reporter
        ExtentReports reports = new ExtentReports();
        reports.attachReporter(sparkReporter);
        reports.setSystemInfo("Tester", ConfigReader.getProperty("tester.name"));
        reports.setSystemInfo("OS", System.getProperty("os.name"));
        reports.setSystemInfo("Java Version", System.getProperty("java.version"));
        extentReports.set(reports);

        logger.info("✅ Report will be generated at: {}/{}", reportPath, reportFileName);
    }

    /**
     * This method runs before each test method.
     * It initializes the WebDriver instance for the current thread and creates a new
     * test entry in the ExtentReport.
     *
     * @param method The test method that is about to be run.
     */
    @Parameters("browser")
    @BeforeMethod(alwaysRun = true)
    public void setUp(String browser, Method method) {
        // Set MDC context at the start of each test method to ensure correct logging
        String mdcSuite = System.getProperty("test.suite", "unknown");
        String mdcBrowser = (browser != null && !browser.isBlank()) ? browser.toUpperCase() : "UNKNOWN";
        String customThreadName = "TestNG-test-" + mdcSuite.toLowerCase() + "-" + browser.toLowerCase() + "-1";
        
        // Set actual thread name so log4j %X{thread} picks it up correctly
        Thread.currentThread().setName(customThreadName);
        
        ThreadContext.put("suite", mdcSuite.toUpperCase());
        ThreadContext.put("browser", mdcBrowser);

        DriverManager.setBrowser(browser);
        DriverManager.getDriver(); // Launch browser
        logger.info("🚀 WebDriver initialized for test: {}", method.getName());

        String browserName = DriverManager.getBrowser().toUpperCase();

        // Create a test entry in report
        ExtentTest test = extentReports.get().createTest(method.getName() + " - " + browserName);
        ExtentManager.setTest(test);
        logger.info("📝 ExtentTest created for test: {} on {}", method.getName(), browserName);
    }

    /**
     * This method runs after each test method.
     * It checks the test result, takes a screenshot on failure, logs the status
     * in the report, and then quits the WebDriver instance for the current thread.
     *
     * @param result The result of the test method that has just run.
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        WebDriver driver = DriverManager.getDriver();

        if (test != null) {
            if (result.getStatus() == ITestResult.FAILURE) {
                String failureMsg = "❌ " + result.getMethod().getMethodName()
                        + " FAILED: " + result.getThrowable().getMessage().split("\n")[0];
                failureSummaries.add(failureMsg);

                String screenshotPath = ScreenshotUtils.captureScreenshot(driver, result.getMethod().getMethodName());
                test.addScreenCaptureFromPath("./screenshots/" + new File(screenshotPath).getName());
                test.fail(result.getThrowable());
                logger.error("❌ Test failed: {} | Screenshot: {}", result.getMethod().getMethodName(), screenshotPath);
            } else {
                test.log(Status.PASS, "✅ Test passed");
            }
        }

        DriverManager.quitDriver();
        logger.info("🧹 WebDriver quit after test: {}", result.getMethod().getMethodName());
        ExtentManager.unload();
    }

    /**
     * ✅ Runs once per <test> tag completion.
     * Flushes report and copies it to index.html for Jenkins if needed.
     */
    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        if (extentReports.get() != null) {
            extentReports.get().flush();
            logger.info("✅ ExtentReports flushed to disk.");
        }

        // Optional logic to write summary and copy report
        String reportDir = System.getProperty("report.dir", "default");
        String suiteName = System.getProperty("test.suite", "default");
        String reportPath = "reports/" + reportDir + "/";
        String reportFileName = suiteName + "-" + reportDir + "-report.html";
        String mergedSummaryFile = "reports/" + suiteName + "-failure-summary.txt";

        if (!failureSummaries.isEmpty()) {
            try {
                File file = new File(mergedSummaryFile);
                file.getParentFile().mkdirs(); // Ensure reports/ exists
                try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) { // append mode
                    failureSummaries.forEach(out::println);
                    logger.info("📄 Failure summary appended to merged file: {}", mergedSummaryFile);
                }
            } catch (IOException e) {
                logger.error("❌ Failed to write to merged failure summary", e);
            }
        }


        // Copy the report to index.html if Jenkins expects it
        try {
            Path source = Paths.get(reportPath + reportFileName);
            Path target = Paths.get(reportPath + "index.html");
            if (Files.exists(source)) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                logger.info("📄 Report copied to index.html for Jenkins display.");
            }
        } catch (IOException e) {
            logger.error("❌ Failed to copy report to index.html", e);
        }
    }
}
