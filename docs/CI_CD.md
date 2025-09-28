# CI/CD Pipeline Documentation

## Overview
This document describes the continuous integration and deployment pipeline for the Flight Booking Test Automation framework.

## Pipeline Stages

### 1. Build
- **Trigger**: On push to any branch
- **Actions**:
  - Checkout code
  - Set up JDK
  - Cache Maven dependencies
  - Build the project
  ```yaml
  - name: Build with Maven
    run: mvn -B clean compile test-compile
  ```

### 2. Test
- **Trigger**: After successful build
- **Actions**:
  - Run unit tests
  - Run integration tests
  - Generate test reports
  ```yaml
  - name: Run Tests
    run: mvn test -B
    
  - name: Generate Allure Report
    if: always()
    run: mvn allure:report
  ```

### 3. Deploy (Release)
- **Trigger**: On tag push (v*)
- **Actions**:
  - Build JAR with dependencies
  - Publish to package registry
  - Create GitHub release
  ```yaml
  - name: Deploy to Maven Central
    if: startsWith(github.ref, 'refs/tags/v')
    run: mvn -B deploy -DskipTests
  ```

## Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `BROWSER` | Browser for tests | No | `chrome` |
| `HEADLESS` | Run in headless mode | No | `false` |
| `ENV` | Environment (dev/stage/prod) | Yes | `dev` |
| `GRID_URL` | Selenium Grid URL | No | Local Grid |

## Local Development

### Running Tests Locally
```bash
# Run all tests
mvn test

# Run specific test group
mvn test -Dgroups=smoke

# Run with specific browser
mvn test -Dbrowser=firefox
```

### Starting Selenium Grid
```bash
docker-compose -f docker-compose-grid.yml up -d
```

## Monitoring
- **Test Results**: View Allure reports in CI artifacts
- **Build History**: Check GitHub Actions tab
- **Code Coverage**: Integrated with SonarCloud

## Troubleshooting

### Common Issues
1. **Dependency Resolution**
   - Delete `.m2/repository` and rebuild
   - Check network connectivity to Maven Central

2. **Test Failures**
   - Check test logs in CI
   - Run tests locally with debug logging:
     ```bash
     mvn test -Dmaven.surefire.debug
     ```

3. **Grid Connection Issues**
   - Verify grid is running: `docker ps`
   - Check grid logs: `docker logs <container_id>`
   - Verify network connectivity between test runner and grid

## Security
- Never commit sensitive data to version control
- Use GitHub Secrets for credentials
- Regularly update dependencies for security patches
