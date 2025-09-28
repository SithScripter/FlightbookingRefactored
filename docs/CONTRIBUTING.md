# Contributing Guide

Thank you for considering contributing to the Flight Booking Test Automation project! This guide will help you get started with contributing.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Code Style](#code-style)
- [Pull Request Process](#pull-request-process)
- [Reporting Issues](#reporting-issues)

## Code of Conduct
This project adheres to the [Contributor Covenant](https://www.contributor-covenant.org/). By participating, you are expected to uphold this code.

## Getting Started

### Prerequisites
- Java 21+
- Apache Maven 3.8+
- Git
- Docker Desktop (for grid execution)
- IDE (IntelliJ IDEA or Eclipse)

### Setting Up
1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/your-username/flight-booking-automation.git
   cd flight-booking-automation
   ```
3. Set up the upstream remote:
   ```bash
   git remote add upstream https://github.com/original-owner/flight-booking-automation.git
   ```
4. Install dependencies:
   ```bash
   mvn clean install -DskipTests
   ```

## Development Workflow

### Branch Naming
```
type/ticket-number-short-description
```

**Types**:
- `feature/` - New features
- `bugfix/` - Bug fixes
- `hotfix/` - Critical production fixes
- `docs/` - Documentation changes

### Commit Messages
Follow [Conventional Commits](https://www.conventionalcommits.org/):
```
<type>[optional scope]: <description>

[optional body]

[optional footer]
```

## Code Style
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Document all public methods with Javadoc
- Write unit tests for new code
- **Project-specific standards**:
  - Use Page Object Model (POM) for UI interactions
  - Implement data-driven testing with JSON or CSV data providers
  - Ensure tests support parallel execution
  - Use ExtentReports for test reporting
  - Log important actions with Log4j2
  - Follow Arrange-Act-Assert pattern in tests

## Pull Request Process
1. Create a feature branch from `develop`
2. Ensure all tests pass
3. Update documentation as needed
4. Submit a pull request to `develop`
5. Request review from at least one maintainer

## Reporting Issues
When creating an issue, please include:
- Steps to reproduce
- Expected vs actual behavior
- Environment details
- Screenshots if applicable
- Any relevant logs

## License
By contributing, you agree that your contributions will be licensed under the project's [MIT License](LICENSE).
