# ✈️ Flight Booking Automation Framework

A robust Selenium test automation framework designed for end-to-end testing of the BlazeDemo flight booking application. Built with **industry-standard patterns** for scalability, maintainability, and ease of use.

## ✨ Features

### Core Framework
- Page Object Model (POM) with composition pattern
- Modular layered architecture (Infrastructure → Domain → Test)
- Centralized WebDriver utilities with instance-based interaction model
- Configuration-driven wait and timeout strategy (no hardcoded waits in Page Objects)
- Comprehensive Log4j2 logging with MDC context

### Testing Capabilities
- Data-driven testing (JSON, CSV, DataFaker)
- Cross-browser execution with parallelization where applicable
- Intelligent retry mechanism for transient failures

### Execution & Infrastructure
- Dockerized Selenium Grid with isolated networks
- Multi-environment execution (QA/Staging/Production)
- Jenkins CI/CD ready with shared library patterns

### Reporting & Quality Gates
- ExtentReports with screenshots and timelines
- Consolidated HTML dashboards in CI/CD
- Automated quality gates with configurable thresholds
- Failure aggregation and smart notifications

## 🚀 Getting Started

This section covers **local and CI-style execution**. For CI/CD internals and architecture, see the documentation links below.

### Prerequisites

- Java 21+
- Apache Maven 3.8+
- Docker Desktop (for grid execution)

### How to Run Tests

The framework supports two primary modes of execution: running tests locally on your machine for development and debugging, and running them against a Dockerized Selenium Grid for CI/CD or comprehensive cross-browser testing.

> These commands mirror how the framework executes inside Jenkins CI.

#### 1. Running from the Command Line (using Maven)

This is the recommended approach for running full test suites. These commands reflect **how the framework is executed in CI**, with local overrides for development.

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

For debugging, tests can also be run directly from the IDE with:
```
-Dselenium.grid.enabled=false
```

## 📊 Test Reports

After test execution, detailed HTML reports are available in:
- `reports/{browser}/` - Individual test run reports
- `test-output/` - TestNG reports and logs

Reports are generated **per browser execution** and aggregated in CI.

## 🔧 Configuration

Edit `src/test/resources/config/config.properties` to configure:
- Test environment (PRODUCTION/STAGING/QA)
- Browser settings and headless mode
- Timeouts and retry configurations
- Reporting preferences and tester information

Configuration is externalized to support non-code environment changes in CI/CD.

### Test Data Formats

The framework supports multiple test data formats:
- **JSON**: Structured test data in `src/test/resources/testdata/` directory
- **CSV**: Tabular test data for data-driven scenarios
- **DataFaker**: Dynamic, seeded test data generation for realistic and reproducible negative testing

### Test Suites

- **Smoke Suite**: Quick validation tests (`testng-smoke.xml`)
- **Regression Suite**: Comprehensive test coverage (`testng-regression.xml`)

## 📚 Architecture & CI/CD Documentation

This repository intentionally separates **architectural intent** from **execution behavior**.

> Start with **CI-CD-architecture.md** for the big picture, then refer to **PIPELINE_GUIDE.md** for execution details.

- **[CI-CD-architecture.md](docs/CI-CD-architecture.md)**  
  High-level CI/CD design — infrastructure, Docker strategy, JCasC, Selenium Grid, and integrations.

- **[PIPELINE_GUIDE.md](docs/PIPELINE_GUIDE.md)**  
  Operational truth — Jenkins pipeline behavior, branch policies, parameters, and execution flow.

- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)**  
  Framework-level design — layers, patterns, and test architecture.


## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
