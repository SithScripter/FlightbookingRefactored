# Architecture Overview

## Framework Architecture

```
┌─────────────────────────────────────────┐
│              Test Layer                 │  (Test Classes)
├─────────────────────────────────────────┤
│            Domain Layer                 │  (Page Objects)
├─────────────────────────────────────────┤
│         Infrastructure Layer            │  (Framework Core)
│  ┌─────────────┴─────────────┐          │
│  │    Driver Management      │          │
│  │    Wait Strategies        │          │
│  │    Listeners              │          │
│  │    Retry Mechanism        │          │
│  └─────────────┬─────────────┘          │
└─────────────────────────────────────────┘
```

## Core Components

### 1. Driver Management
- **Location**: `src/main/java/com/demo/flightbooking/utils/DriverManager.java`, `src/main/java/com/demo/flightbooking/factory/BrowserOptionsFactory.java`
- **Responsibility**: Manages WebDriver lifecycle
- **Key Features**:
  - Thread-safe WebDriver instances
  - Support for multiple browsers
  - Remote WebDriver support

### 2. Page Objects
- **Location**: `src/main/java/com/demo/flightbooking/pages/`
- **Pattern**: Page Object Model (POM) with composition
- **Structure**:
  - BasePage: Common page functionality
  - Pages: Page-specific classes

### 3. Test Data Management
- **Location**: `src/test/java/com/demo/flightbooking/utils/`
- **Components**:
  - CsvDataProvider: CSV file parsing and data provision
  - JsonDataProvider: JSON file parsing and data provision
- **Test Data Location**: `src/test/resources/testdata/`
- **Supported Formats**:
  - JSON files for structured test data
  - CSV files for tabular test data
  - DataFaker library for dynamic, seeded test data generation
- **Features**:
  - TestNG data provider integration
  - Environment-specific data support
  - Deterministic randomization with fixed seeds for reproducible negative tests

### 4. Test Execution & Lifecycle
- **Framework**: TestNG
- **Location**: `src/test/java/com/demo/flightbooking/tests/`
- **Features**:
  - Parallel test execution across browsers
  - Data providers for data-driven testing
  - Custom listeners for test lifecycle management
  - Retry mechanisms with configurable retry counts

### 5. Test Listeners & Reporting
  - **Location**: `src/test/java/com/demo/flightbooking/listeners/`
  - **Components**:
    - TestListener: Custom TestNG listener for enhanced reporting
    - RetryAnalyzer: Configurable test retry logic
  - **Features**:
    - Screenshot capture on failure
    - Custom reporting hooks
    - Test execution tracking
    - **CI/CD Integration**: Jenkins quality gates evaluate TestNG/Surefire XML results for pass/fail decisions; HTML reports are for human-readable diagnostics.
- **Location**: `src/main/java/com/demo/flightbooking/utils/`
- **Components**:
  - ConfigReader: Configuration file parsing and management
  - DriverManager: WebDriver lifecycle and thread-safe management
  - ExtentManager: ExtentReports integration and management
  - MaskingUtil: Data masking utilities for sensitive information
  - ScreenshotUtils: Screenshot capture and management
  - WebDriverUtils: WebDriver helper methods and utilities

WebDriverUtils follows an instance-based interaction model.
Page Objects delegate all waits and interactions to WebDriverUtils,
ensuring centralized timeout control via configuration.

## Design Patterns

### 1. Page Object Model (POM) with Composition
- Encapsulates page details
- Delegates interactions to utilities
- Improves maintainability

### 2. Factory Pattern
- Creates WebDriver instances via BrowserOptionsFactory

### 3. Singleton Pattern
- Manages configuration via ConfigReader
- Handles ExtentManager for reports

## Best Practices

### 1. Test Structure
- One test class per page/feature
- Descriptive test method names
- Arrange-Act-Assert pattern

### 2. Error Handling
- Meaningful error messages
- Screenshot on failure

### 3. Logging
- Log4j2
- Appropriate log levels
- Structured logging

## Additional Components

### Enums
- **Location**: `src/main/java/com/demo/flightbooking/enums/`
- **Components**:
  - BrowserType: Defines supported browsers (CHROME, FIREFOX, EDGE)
  - EnvironmentType: Defines environments (QA, STAGING, PRODUCTION)

### Model
- **Location**: `src/main/java/com/demo/flightbooking/model/`
- **Components**:
  - Passenger: Data model for test data using record pattern

## Dependencies

### Core Dependencies
- Selenium WebDriver
- TestNG
- WebDriverManager
- ExtentReports
- Log4j2
- Commons IO
- OpenCSV
- Jackson Databind
- DataFaker
- Apache Commons Lang
