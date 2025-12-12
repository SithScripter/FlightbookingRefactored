# Flight Booking Pipeline Guide

## 1. Overview

This document outlines the policies, behaviors, and operational procedures for the Flight Booking test automation pipeline. It serves as the single source of truth for understanding and working with the CI/CD process effectively.

## 2. Core Concepts & Policies

### Branching Strategy

- **main**: Production branch. All code must pass review and regression tests before merging.
- **enhancements**: A pre-production branch for integrating major features.
- **feature/***: Standard branches for developing new features.

### Build Failure Conditions

- **FAILURE (Red)**: The build is immediately stopped and marked as failed. This occurs when:
  - A critical infrastructure or script error occurs in any stage.
  - Any test fails when run on a protected branch (main, enhancements).
- **UNSTABLE (Yellow)**: The build completes all stages, but one or more tests have failed. This is the expected outcome for test failures on feature branches.

### Test Execution Strategy

- **smoke suite**: A small set of critical-path tests. Runs automatically on pushes to all branches.
- **regression suite**: A comprehensive suite of all tests. Intended to be run on protected branches before a release.
- **Parallel Execution**: Tests are automatically run in parallel across Chrome and Firefox to reduce execution time.

## 3. Pipeline Features

### Manual Triggers & Parameters

The following parameters are available when running a build manually in Jenkins:

| Parameter | Options | Description |
|-----------|---------|-------------|
| SUITE_NAME | smoke, regression | Selects the TestNG suite to execute. |
| TARGET_ENVIRONMENT | PRODUCTION, STAGING, QA | Specifies the test environment configuration to use. |
| MANUAL_APPROVAL | true, false | If true, pauses the pipeline for manual approval before running regression tests on protected branches. |
| QASE_TEST_CASE_IDS | String of IDs | (Optional) A comma-separated list of Qase Test Case IDs to update, overriding the defaults. |
| QUALITY_GATE_THRESHOLD | 0, 1, 2, 5 | Maximum number of test failures allowed before the quality gate fails. Default is 0 (zero tolerance). |
| FAIL_ON_NO_TESTS | true, false | If true, marks the build as UNSTABLE when no test results are found. Default is true. |

### Quality Gate Enforcement

The pipeline includes an automated quality gate that validates test results after every run:

- **Threshold-Based Enforcement**: Configurable via `QUALITY_GATE_THRESHOLD` parameter (0, 1, 2, or 5 failures)
- **Dual-Format Support**: Parses both JUnit (surefire) and TestNG XML reports accurately
- **Parallel Aggregation**: Combines results from Chrome + Firefox executions
- **Branch-Specific Policies**:
  - **Feature branches**: Exceeding threshold marks build as `UNSTABLE` (allows continued development)
  - **Protected branches** (main, enhancements): Exceeding threshold marks build as `FAILURE` (prevents bad merges)

**Quality Gate Output Example:**
```
📊 Quality Gate: 4/39 failures (threshold: 0)
⚠️ Quality Gate: Build marked UNSTABLE due to 4 failures
```

### Notifications

- **Email**: Sent when build status changes (e.g., SUCCESS → UNSTABLE). Includes quality gate summary and test failure details.
- **Qase**: Test run results are automatically published to your Qase project.

### Guaranteed Cleanup

The pipeline ensures that all resources are properly shut down after every run, regardless of the build's outcome. This includes:

- Selenium Grid shutdown
- Docker container removal
- Jenkins workspace cleanup

## 4. The Build Environment

### Docker Agent & Caching

The pipeline executes inside a custom Docker container (`flight-booking-agent-prewarmed:latest`) to ensure a consistent and clean build environment. This image contains all necessary tools (Java, Maven, Docker CLI) and a pre-warmed cache of all Maven dependencies.

Dependency caching is enforced by a custom `settings.xml` file with an `<updatePolicy>never</updatePolicy>`, which forces Maven to use the local cache and prevents any downloads during the build.

### When to Rebuild the Pre-warmed Image

The pre-warmed Docker image must be rebuilt to keep the cache up to date. Rebuild the image after:

1. Adding, removing, or updating any dependencies in the `pom.xml`.
2. Changing system-level tools in the base Dockerfile.
3. Updating the base Maven/Java image version.

## 5. Local Development

### Running Tests Locally

```bash
# Run the default (smoke) test suite
mvn clean test

# Run the regression suite by activating its profile
mvn clean test -P regression

# Run a specific TestNG group (e.g., 'smoke')
mvn clean test -Dgroups=smoke

# Run with a specific browser
mvn clean test -Dbrowser=firefox
```

### Starting the Selenium Grid

```bash
# Start the Selenium Grid in the background
docker-compose -f docker-compose-grid.yml up -d

# Shut down the Grid when finished
docker-compose -f docker-compose-grid.yml down
```

## 6. Dependencies & Prerequisites

### Shared Library

The pipeline relies on a Jenkins Shared Library for reusable functions.
- **Name**: `my-automation-library`

### Required Jenkins Credentials

The following credential IDs must be configured in Jenkins for the pipeline to function correctly:

- `qase-api-token`: API token for updating test results in Qase.
- `recipient-email-list`: The distribution list for email notifications.

## 7. Troubleshooting

### Common Issues

#### 1. Maven 'BUILD SUCCESS' vs. Jenkins 'UNSTABLE' Status

- **Symptom**: The Maven console output shows `BUILD SUCCESS`, but the Jenkins build is marked as `UNSTABLE`.
- **Explanation**: This is intended. Maven is configured with `<testFailureIgnore>true</testFailureIgnore>` to ensure it always finishes and generates reports. Jenkins then reads these reports and correctly marks the build `UNSTABLE` if it finds test failures.
- **Action**: No action needed. This indicates the pipeline worked, but tests failed.

#### 2. Dependency Resolution Issues

- **Symptom**: The build fails with a "dependency not found" error.
- **Explanation**: The `pom.xml` was likely updated with a new dependency, but the pre-warmed Docker image was not rebuilt.
- **Solution**: Rebuild the `flight-booking-agent-prewarmed:latest` image and re-run the build.

#### 3. Test Failures on Protected Branches

- **Symptom**: The build is marked as `FAILURE` when tests fail on `main` or `enhancements`.
- **Explanation**: This is by design to enforce quality on protected branches.
- **Action**: Fix the failing tests in your feature branch before merging.

#### 4. Random Docker/Network Errors

- **Symptom**: The build fails with a random connection error or a Docker daemon error.
- **Explanation**: This is likely a transient infrastructure issue.
- **Action**: Use the "Replay" button in the Jenkins build to re-run the pipeline.

#### 5. Grid Connection Issues

- **Symptom**: Tests fail to connect to Selenium Grid.
- **Check**: Verify Grid containers are running using `docker ps`.
- **Action**: Check Docker logs (`docker-compose logs`) and restart the Grid if needed.

### Monitoring

- **Test Results**: View ExtentReports in build artifacts.
- **Build History**: Check Jenkins build history.
- **Logs**: Access detailed execution logs in Jenkins.

## 8. Best Practices

1. **Keep the Pre-warmed Image Up to Date**: Always rebuild the image after `pom.xml` dependency changes.
2. **Run Tests Locally First**: Catch issues early before pushing code.
3. **Rebase Feature Branches Regularly**: Keep branches updated to avoid large merge conflicts.
4. **Monitor Test Flakiness**: Investigate and fix tests that fail intermittently.
5. **Use Meaningful Commit Messages**: Clearly explain the "what" and the "why" of your changes.
6. **Document Significant Changes**: Update this guide when making major changes to the pipeline or framework.
