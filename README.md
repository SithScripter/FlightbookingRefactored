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

#### 1. Local Execution

```bash
# Run smoke tests on local Chrome
mvn clean test -P smoke -Dselenium.grid.enabled=false

# Run regression tests on local Firefox
mvn clean test -P regression -Dbrowser=firefox -Dselenium.grid.enabled=false
```

#### 2. Dockerized Selenium Grid

```bash
# Start the Selenium Grid
docker-compose -f docker-compose-grid.yml up -d

# Run tests against the grid
mvn clean test -P smoke -Dselenium.grid.enabled=true

# Run regression tests on the grid with parallel execution
mvn clean test -P regression -Dselenium.grid.enabled=true

# Shut down the grid when finished
docker-compose -f docker-compose-grid.yml down
```

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