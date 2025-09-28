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
- **Location**: `src/test/java/com/demo/flightbooking/data/`
- **Pattern**: Factory and Builder patterns
- **Features**:
  - Test data generation
  - Data-driven testing support
  - Environment-specific data

### 4. Test Execution
- **Framework**: TestNG
- **Features**:
  - Parallel test execution
  - Test grouping
  - Data providers
  - Listeners for test lifecycle

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
