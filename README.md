# ✈️ Flight Booking Automation Framework

This is a hybrid Selenium + TestNG automation framework for the BlazeDemo website, designed with best practices for CI/CD, reporting, and parallel execution.

---
This is a robust Selenium test automation framework designed to perform end-to-end tests for the BlazeDemo flight booking application. It is built with industry-best practices to be scalable, maintainable, and easy for any team member to use.

## ✨ Features

* **Page Object Model (POM)**: Clean separation of UI elements from test logic for easy maintenance.
* **Data-Driven Testing**: Test data is managed externally in CSV and JSON files, no hard-coded data in tests.
* **Cross-Browser Execution**: Supports Chrome, Firefox, and Edge, configurable from a single file.
* **Parallel Test Execution**: Configured with TestNG to run tests in parallel, significantly reducing execution time.
* **Docker Integration**: Comes with a pre-configured Selenium Grid, allowing for consistent test runs in a containerized environment with a single command.
* **CI/CD Ready**: Includes a `Jenkinsfile` for easy integration into a CI/CD pipeline.
* **Rich Reporting**: Generates detailed HTML reports using ExtentReports, including screenshots for failed tests.

---

## 🚀 Getting Started

### Prerequisites

* Java 21+
* Apache Maven
* Docker Desktop

### How to Run Tests

There are two ways to run the tests:

**1. Run Locally**

This command runs the full regression suite on your local Chrome browser.

```bash
# Set selenium.grid.enabled=false in src/test/resources/config/config.properties
mvn clean test -Dsuite.xml.file=testng-regression.xml

**2. Run on Dockerized Selenium Grid**

# 1. Start the Selenium Grid
docker-compose up -d

# 2. Run the tests
# Set selenium.grid.enabled=true in src/test/resources/config/config.properties
mvn clean test -Dsuite.xml.file=testng-regression.xml

# 3. Shut down the Grid when finished
docker-compose down

📊 Reporting
After a test run, detailed HTML reports can be found in the `reports/` directory, organized by browser and test suite (e.g., `reports/chrome/regression-chrome-report.html`).

This is the recommended way to run tests for consistency.

Happy Testing! 🧪🐞🚀