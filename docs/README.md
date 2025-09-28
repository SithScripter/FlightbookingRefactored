# Flight Booking Test Automation

## Overview
This project contains automated tests for the Flight Booking application, built with Selenium WebDriver, TestNG, and Java. The framework follows the Page Object Model (POM) design pattern and supports parallel test execution.

## Quick Start

### Prerequisites
- Java 11+
- Maven 3.8+
- Chrome/Firefox browser
- Allure (for reporting)

### Running Tests
```bash
# Run all tests
mvn clean test

# Run specific test group
mvn test -Dgroups=smoke

# Generate Allure report
mvn allure:serve
```

### CI/CD
- **Main Branch**: Runs on every push
- **Pull Requests**: Runs smoke tests
- **Nightly**: Full regression suite

## Documentation
- [Architecture](ARCHITECTURE.md) - Framework design and components
- [Contributing](CONTRIBUTING.md) - How to contribute to the project
- [CI/CD](CI_CD.md) - Continuous Integration/Deployment setup

## License
This project is licensed under the MIT License.
