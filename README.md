# ✈️ Flight Booking Automation Framework

A robust Selenium test automation framework designed for end-to-end testing of the BlazeDemo flight booking application. Built with industry best practices for scalability, maintainability, and ease of use.

## ✨ Features

* **Page Object Model (POM)**: Clean separation of UI elements from test logic
* **Data-Driven Testing**: External test data management using JSON and CSV formats
* **Cross-Browser Testing**: Supports Chrome and Firefox in headless mode
* **Parallel Execution**: TestNG-powered parallel test execution across multiple browsers
* **Dockerized Grid**: Pre-configured Selenium Grid for consistent test environments
* **Multi-Environment Support**: Configurable testing across QA, Staging, and Production environments
* **CI/CD Ready**: Jenkins pipeline integration with parallel test execution and automated reporting
* **Rich Reporting**: ExtentReports with detailed test results, screenshots, and custom dashboards
* **Modular Design**: Clean architecture with separate layers for pages, tests, and utilities
* **Retry Mechanisms**: Built-in test retry logic with configurable retry counts
* **Comprehensive Logging**: Log4j2 integration with structured logging and configurable log levels

## 🚀 Getting Started

### Prerequisites

* Java 21+
* Apache Maven 3.8+
* Docker Desktop (for grid execution)

### How to Run Tests

The framework supports two primary modes of execution: running tests locally on your machine for development and debugging, and running them against a Dockerized Selenium Grid for CI/CD or comprehensive cross-browser testing.

#### 1. Running from the Command Line (using Maven)

This is the recommended approach for running full test suites.

**A) Local Execution (No Docker Grid)**

This mode uses WebDriverManager to automatically manage browser drivers on your local machine. It's perfect for quick feedback during development.

```bash
# Run the smoke suite on your local Chrome browser
mvn clean test -P smoke -Dselenium.grid.enabled=false

# Run the regression suite on your local Firefox browser
mvn clean test -P regression -Dbrowser=firefox -Dselenium.grid.enabled=false
```

**B) Dockerized Selenium Grid Execution**

This mode is used by the CI/CD pipeline and is ideal for running the full regression suite across multiple browsers in parallel.

```bash
# 1. Start the Selenium Grid in the background
docker-compose -f docker-compose-grid.yml up -d

# 2. Run the smoke suite against the grid (defaults to Chrome)
mvn clean test -P smoke -Dselenium.grid.enabled=true

# 3. Shut down the grid when you're finished
docker-compose -f docker-compose-grid.yml down
```

#### 2. Running from an IDE (e.g., IntelliJ, Eclipse)

For debugging a single test or a small group of tests, running directly from your IDE is most efficient.

1.  **Open the Test Class**: Navigate to the test file you want to run (e.g., `EndToEndBookingTest.java`).
2.  **Configure TestNG Defaults**:
    *   Go to your IDE's run/debug configuration settings for TestNG.
    *   In the "VM options" or "VM arguments" field, you **must** specify `-Dselenium.grid.enabled=false` to ensure the test runs locally.
    *   You can also add other parameters like `-Dbrowser=firefox` if you wish to override the default (Chrome).
3.  **Run the Test**: Right-click on the test method or class and select "Run" or "Debug".

## 📊 Test Reports

After test execution, detailed HTML reports are available in:
- `reports/{browser}/` - Individual test run reports
- `test-output/` - TestNG reports and logs

## 🔧 Configuration

Edit `src/test/resources/config/config.properties` to configure:
- Test environment (PRODUCTION/STAGING/QA)
- Browser settings and headless mode
- Timeouts and retry configurations
- Reporting preferences and tester information

### Test Data Formats

The framework supports multiple test data formats:
- **JSON**: Structured test data in `src/test/resources/testdata/` directory
- **CSV**: Tabular test data for data-driven scenarios
- **DataFaker**: Dynamic, seeded test data generation for realistic and reproducible negative testing

### Test Suites

- **Smoke Suite**: Quick validation tests (`testng-smoke.xml`)
- **Regression Suite**: Comprehensive test coverage (`testng-regression.xml`)

## Project Documentation

For a deeper look into the framework's design and CI/CD process, please see the following guides:
- **[Architecture Guide](docs/ARCHITECTURE.md)**: An overview of the framework's layers and design patterns.
- **[Pipeline Guide](docs/PIPELINE_GUIDE.md)**: A detailed explanation of the Jenkins CI/CD pipeline, its features, and operational policies.

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Happy Testing! 🧪🚀