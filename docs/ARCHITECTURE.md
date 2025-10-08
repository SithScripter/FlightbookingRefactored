# Architecture Overview

## Framework Architecture

```
┌─────────────────────────────────────────┐
│              Test Layer                │  (Test Classes)
├─────────────────────────────────────────┤
│              Page Layer                │  (Page Objects)
├─────────────────────────────────────────┤
│           Component Layer              │  (Reusable Components)
├─────────────────────────────────────────┤
│             Core Layer                 │  (Framework Core)
│  ┌─────────────┴─────────────┐         │
│  │    Driver Management      │         │
│  │    Wait Strategies        │         │
│  │    Listeners              │         │
│  │    Retry Mechanism        │         │
│  └─────────────┬─────────────┘         │
└─────────────────────────────────────────┘
```

## Core Components

### 1. Driver Management
- **Location**: `src/main/java/com/demo/flightbooking/factory/`
- **Responsibility**: Manages WebDriver lifecycle
- **Key Features**:
  - Thread-safe WebDriver instances
  - Support for multiple browsers
  - Remote WebDriver support

### 2. Page Objects
- **Location**: `src/main/java/com/demo/flightbooking/pages/`
- **Pattern**: Page Object Model (POM)
- **Structure**:
  - BasePage: Common page functionality
  - Component: Reusable UI components
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
- **Features**:
  - TestNG data provider integration
  - Environment-specific data support
  - Programmatic data generation capabilities

### 4. Test Execution & Lifecycle
- **Framework**: TestNG
- **Location**: `src/test/java/com/demo/flightbooking/tests/`
- **Features**:
  - Parallel test execution across browsers
  - Test grouping and categorization
  - Data providers for data-driven testing
  - Custom listeners for test lifecycle management
  - Retry mechanisms with configurable retry counts

### 5. Test Listeners & Reporting
- **Location**: `src/test/java/com/demo/flightbooking/listeners/`
- **Components**:
  - TestListener: Custom TestNG listener for enhanced reporting
  - TestReporter: Custom reporting integration
  - RetryAnalyzer: Configurable test retry logic
- **Features**:
  - Screenshot capture on failure
  - Custom reporting hooks
  - Test execution tracking

### 6. Utilities & Helpers
- **Location**: `src/main/java/com/demo/flightbooking/utils/`
- **Components**:
  - ConfigReader: Configuration file parsing and management
  - DriverManager: WebDriver lifecycle and thread-safe management
  - ExtentManager: ExtentReports integration and management
  - MaskingUtil: Data masking utilities for sensitive information
  - ScreenshotUtils: Screenshot capture and management
  - WebDriverUtils: WebDriver helper methods and utilities

## Design Patterns

### 1. Page Object Model (POM)
- Encapsulates page details
- Reduces code duplication
- Improves maintainability

### 2. Factory Pattern
- Creates WebDriver instances
- Manages test data generation

### 3. Builder Pattern
- Constructs complex objects
- Improves test readability

### 4. Singleton Pattern
- Manages configuration
- Handles WebDriver instances

## Best Practices

### 1. Test Structure
- One test class per page/feature
- Descriptive test method names
- Arrange-Act-Assert pattern

### 2. Error Handling
- Custom exceptions
- Meaningful error messages
- Screenshot on failure

### 3. Logging
- Log4j2
- Appropriate log levels
- Structured logging

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
