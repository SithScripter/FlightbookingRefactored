# CI/CD Architecture – Selenium Automation Framework

## 1. Overview

This project uses a **fully containerized, Infrastructure-as-Code CI/CD setup** to execute Selenium + TestNG automation reliably across environments and browsers.

Key principles:

* **Everything as code** (pipeline, Jenkins config, infra)
* **Reproducible builds** using Docker
* **Fast execution** via pre-warmed build agents
* **Scalable parallel testing** with Selenium Grid
* **Production-grade observability & reporting**

## 2. Quick Start

```bash
# 1. Start Jenkins (includes JCasC auto-configuration)
cd jenkinsFinalSetup-temp && docker compose up -d

# 2. Build agents
docker build -f cicd/Dockerfile -t flight-booking-agent:latest .
docker build -f cicd/Dockerfile-prewarmed -t flight-booking-agent-prewarmed:latest .

# 3. Push code and trigger pipeline
# Pipeline handles Selenium Grid, parallel execution, reporting
```

## 3. High-Level Architecture

```
Developer Commit
      ↓
Git Repository (Multibranch)
      ↓
Jenkins (Dockerized, JCasC)
      ↓
Pre-warmed Docker Agent
      ↓
Selenium Grid (Docker Compose)
      ↓
Test Execution (Chrome / Firefox in parallel)
      ↓
Reports + Dashboards + Notifications
```

## 4. Jenkins Controller (Infrastructure as Code)

### Key Characteristics

* Runs as a **Docker container**
* Fully configured using **Jenkins Configuration as Code (JCasC)**
* Zero manual UI setup after container startup

### What is configured via JCasC

* Security realm (admin user)
* Authorization strategy
* Global shared libraries
* SMTP / Email extension
* Credentials (Qase token, email lists)
* Jenkins URL & global settings

**Benefits**

* Reproducible Jenkins setup
* Easy migration to another machine/environment
* No configuration drift

---

## 4. Jenkins Pipeline (Pipeline as Code)

### Style

* Declarative pipeline
* Uses a **custom shared library**
* No logic duplicated across Jenkinsfiles

### Pipeline Responsibilities

* Determine suite to run (smoke / regression)
* Apply branch-based execution rules
* Start and validate Selenium Grid
* Run tests in **parallel browsers**
* Collect, merge, and publish reports
* Notify stakeholders
* Update test management (Qase)

---

## 5. Jenkins Shared Library

Reusable pipeline logic is extracted into shared library functions:

| Function                  | Responsibility                                  |
| ------------------------- | ----------------------------------------------- |
| `getBranchConfig()`       | Branch policies (main, feature, prod-candidate) |
| `determineTestSuite()`    | Decide smoke vs regression                      |
| `printBuildMetadata()`    | Log build metadata (suite, branch, trigger)     |
| `startDockerGrid()`       | Start + health-check Selenium Grid              |
| `stopDockerGrid()`        | Safe cleanup                                    |
| `checkTestFailures()`     | Quality gate - parse test results, enforce thresholds |
| `generateDashboard()`     | Consolidated HTML dashboard                     |
| `archiveAndPublishReports()` | Archive artifacts and publish HTML reports   |
| `sendBuildSummaryEmail()` | Email notifications                             |
| `updateQase()`            | Push results to Qase                            |

**Why Shared Library**

* Enforces standards
* Reduces Jenkinsfile complexity
* Scales to multiple projects

---

## 6. Pre-Warmed Build Agents

### Purpose

Reduce build time and eliminate flaky dependency downloads.

### Strategy

Two layered Docker images:

1. **Base agent**

   * Java 21
   * Maven
   * Docker CLI
   * Docker Compose
2. **Pre-warmed agent**

   * Project source code
   * All Maven dependencies pre-downloaded
   * Uses custom `settings.xml`

### Maven Caching Strategy

* `force-local-cache` Maven profile
* `updatePolicy=never`
* Snapshot downloads disabled

**Outcome**

* Deterministic builds
* Faster pipelines
* No surprise dependency changes

---

## 7. Selenium Grid Orchestration

### Execution Model

* Selenium Grid runs via Docker Compose
* Separate containers:

  * `selenium-hub`
  * `chrome` node
  * `firefox` node

### Design Highlights

* Unique Docker network per pipeline run
* Controlled max sessions per browser
* `/dev/shm` mounting to prevent browser crashes
* Health checks before test execution

**Result**

* Reliable parallel execution
* No cross-build interference
* Easy horizontal scaling (add nodes)

---

## 8. Test Execution Flow

1. Jenkins spins up pre-warmed agent
2. Selenium Grid is started and validated
3. Tests execute in parallel:

   * Chrome
   * Firefox
4. Each browser:

   * Uses its own workspace
   * Generates isolated reports
5. Results are stashed and merged

---

## 9. Reporting & Dashboards

### Reporting Mechanisms

* **ExtentReports** (per browser)
* Consolidated **HTML dashboard**
* Jenkins archived artifacts

### Failure Aggregation

* Failures collected across threads
* Single failure summary file
* Used for dashboards and email alerts

---

## 10. Notifications & Test Management

### Email Notifications

* HTML summary
* Links to reports
* Branch-specific recipient lists

  * Feature branches → Dev list
  * Main / prod branches → Wider audience

### Qase Integration

* Suite-based configuration (`cicd/qase_config.json`)
* Automatic publishing of results
* Optional override via Jenkins parameter

---

## 11. Environment Strategy

| Aspect          | Strategy                      |
| --------------- | ----------------------------- |
| Configuration   | Environment variables         |
| Secrets         | Externalized (not in repo)    |
| Environments    | QA / Staging / Production     |
| Branch behavior | Controlled via shared library |

---

## 12. Why This Design (Interview Summary)

* **Scalable:** Parallel browsers, isolated containers
* **Reliable:** Thread-safe execution, deterministic builds
* **Maintainable:** Shared libraries, clean separation of concerns
* **Production-ready:** Docker, JCasC, IaC principles
* **Extensible:** Easy move to Kubernetes, cloud agents, enhanced observability

---

## 13. Quality Gates

### Purpose

Automated validation of test results to prevent broken code from progressing through the pipeline.

### Implementation

* **Threshold-based enforcement**: Configurable via `QUALITY_GATE_THRESHOLD` Jenkins parameter
* **Dual-format XML parsing**: Supports both JUnit (surefire) and TestNG reports
* **Parallel result aggregation**: Combines Chrome + Firefox test results
* **Branch-specific policies**:
  * Feature branches → UNSTABLE (allows continued development)
  * Protected branches → FAILURE (prevents merges)

### Key Features

* Type-safe parameter conversion
* Handles "no tests found" scenarios
* Clear console logging with emojis for visibility
* Smart email notifications (only on status changes)

---

## 14. Future Evolution (Roadmap)

* Kubernetes-based Selenium Grid
* Jenkins agent autoscaling
* Prometheus + Grafana monitoring
* Artifact storage in S3 / Nexus
* Quality gate enhancements (percentage thresholds, trend analysis)

---

✅ **This CI/CD setup is designed to demonstrate senior-level QA + DevOps maturity, not just test execution.**
