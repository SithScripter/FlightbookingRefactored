@Library('my-automation-library') _

def branchConfig = getBranchConfig()  // ✅ Load centralized config

pipeline {
    // ✅ We define no top-level agent. This enables a flexible multi-agent
    // strategy where each stage can use its own specialized environment.
    agent none 

    options {
        // ✅ Prevents Jenkins from doing an initial checkout on the controller.
        skipDefaultCheckout()
    }

    triggers {
        cron('H 2 * * *')
    }

    parameters {
        choice(name: 'SUITE_NAME', choices: ['smoke', 'regression'], description: 'Select suite (only applies to manual runs).')
        choice(name: 'TARGET_ENVIRONMENT', choices: ['PRODUCTION', 'STAGING', 'QA'], description: 'Select test environment.')
        booleanParam(name: 'MANUAL_APPROVAL', defaultValue: false, description: '🛑 Only relevant if "regression" is selected. Ignored for "smoke".')
        string(name: 'QASE_TEST_CASE_IDS', defaultValue: '', description: 'Optional: Override default Qase IDs.')
    }

    stages {
        stage('Determine Suite') {
            agent any
            steps {
                script {
                    env.SUITE_TO_RUN = determineTestSuite()
                }
            }
        }

        stage('Initialize & Start Grid') {
            when {
                expression { return env.BRANCH_NAME in branchConfig.pipelineBranches }
            }
            agent {
                docker {
                    image 'flight-booking-agent-prewarmed:latest'
                    args '-u root -v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""'
                }
            }
            steps {
                retry(2) {
                    // Create the Docker network explicitly before starting containers
                    sh 'docker network create selenium_grid_network || true'
                    initializeTestEnvironment(env.SUITE_TO_RUN)
                }
            }
        }

        stage('Approval Gate (Regression Only)') {
            when {
                allOf {
                    expression { return env.BRANCH_NAME in branchConfig.productionCandidateBranches }
                    expression { return env.SUITE_TO_RUN == 'regression' }
                    expression { return params.MANUAL_APPROVAL == true }
                }
            }
            agent any
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input message: "🛑 Proceed with full regression for branch '${env.BRANCH_NAME}'?"
                }
            }
        }
        // Force multi branch pipeline to auto trigger
        // Force multi branch pipeline to auto trigger
        stage('Build & Run Parallel Tests') {
            when {
                expression { return env.BRANCH_NAME in branchConfig.pipelineBranches }
            }
            agent {
                docker {
                    image 'flight-booking-agent-prewarmed:latest'
                    args '-u root -v /var/run/docker.sock:/var/run/docker.sock --entrypoint="" --network=selenium_grid_network'
                }
            }
            steps {
                // DIAGNOSTIC STEP 1: VERIFY THE IMAGE ID
                echo "Verifying Docker Image ID..."
                sh 'docker images flight-booking-agent-prewarmed'

                // DIAGNOSTIC STEP 2: VERIFY THE CACHE CONTENTS
                echo "Verifying contents of the pre-warmed cache..."
                sh 'ls -la /root/.m2/repository/org/apache/maven/surefire'

                // DIAGNOSTIC STEP 3: VERIFY MAVEN'S REPOSITORY PATH
                echo "Verifying Maven's configured local repository path..."
                sh 'mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout'

                echo "🧪 Running parallel tests for: ${env.SUITE_TO_RUN}"
                retry(2) {
                    timeout(time: 2, unit: 'HOURS') {
                        script {
                            def mvnBase = "mvn clean test -P ${env.SUITE_TO_RUN} -Denv=${params.TARGET_ENVIRONMENT} -Dtest.suite=${env.SUITE_TO_RUN} -Dbrowser.headless=true"
                            parallel(
                                Chrome: {
                                    sh "${mvnBase} -Dbrowser=chrome -Dreport.dir=chrome"
                                },
                                Firefox: {
                                    sh "${mvnBase} -Dbrowser=firefox -Dreport.dir=firefox"
                                }
                            )
                        }
                    }
                }
                // Stash all artifacts needed for post-processing
                echo "Stashing build artifacts (reports, screenshots, test results)..."
                stash name: 'build-artifacts', includes: 'reports/**, target/surefire-reports/**', allowEmpty: true
            }
        }
    }

    post {
        always {
            script {
                // Re-introduce the functional 'inside' wrapper
                docker.image('flight-booking-agent-prewarmed:latest').inside('-u root -v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""') {
                    echo "--- Starting Guaranteed Post-Build Processing ---"

                    try {
                        unstash 'build-artifacts'
                    } catch (e) {
                        echo "⚠️ Build artifacts not found to unstash. This is expected if the build failed before stashing."
                    }

                    // 1. Publish Test Results (sets the final build status)
                    junit testResults: 'target/surefire-reports/**/*.xml', allowEmptyResults: true

                    // 2. Generate and Publish HTML Reports (from shared library)
                    generateDashboard(env.SUITE_TO_RUN, "${env.BUILD_NUMBER}")
                    archiveAndPublishReports()

                    // 3. Handle Notifications (Qase, Email)
                    if (env.BRANCH_NAME in branchConfig.productionCandidateBranches) {
                        echo "🚀 Running notifications for production-candidate branch..."
                        try {
                            def qaseConfig = readJSON file: 'cicd/qase_config.json'
                            def suiteSettings = qaseConfig[env.SUITE_TO_RUN]
                            if (suiteSettings) {
                                def qaseIds = (params.QASE_TEST_CASE_IDS?.trim()) ? params.QASE_TEST_CASE_IDS : suiteSettings.testCaseIds
                                updateQase(
                                    projectCode: 'FB',
                                    credentialsId: 'qase-api-token',
                                    testCaseIds: qaseIds
                                )
                                sendBuildSummaryEmail(
                                    suiteName: env.SUITE_TO_RUN,
                                    emailCredsId: 'recipient-email-list'
                                )
                            }
                        } catch (err) {
                            echo "⚠️ Notification step failed: ${err.getMessage()}"
                        }
                    } else {
                        echo "ℹ️ Skipping notifications for branch: ${env.BRANCH_NAME}"
                    }

                    // Conditional notifications based on build result
                    if (currentBuild.result == 'UNSTABLE') {
                        echo "📧 Notifying QA team for UNSTABLE build on ${env.BRANCH_NAME}"
                        try {
                            sendBuildSummaryEmail(
                                suiteName: env.SUITE_TO_RUN,
                                emailCredsId: 'recipient-email-list'
                            )
                        } catch (err) {
                            echo "⚠️ Email notification failed: ${err.getMessage()}"
                        }
                    } else if (currentBuild.result == 'FAILURE') {
                        echo "📧 Notifying DevOps team for FAILURE build on ${env.BRANCH_NAME}"
                        try {
                            sendBuildSummaryEmail(
                                suiteName: env.SUITE_TO_RUN,
                                emailCredsId: 'recipient-email-list'
                            )
                        } catch (err) {
                            echo "⚠️ Email notification failed: ${err.getMessage()}"
                        }
                    }
                }
            }
        }
        success {
            echo "✅ Build SUCCESS. All tests passed."
            script {
                echo "⏱️ Build duration: ${currentBuild.durationString}"
            }
        }
        unstable {
            echo "⚠️ Build UNSTABLE. Tests failed. Check the 'Test Dashboard' for detailed results."
            script {
                echo "⏱️ Build duration: ${currentBuild.durationString}"
            }
        }
        failure {
            echo "❌ Build FAILED. A critical error occurred in one of the stages."
            script {
                echo "⏱️ Build duration: ${currentBuild.durationString}"
            }
        }
        cleanup {
            script {
                // Re-introduce the functional 'inside' wrapper
                docker.image('flight-booking-agent-prewarmed:latest').inside('-u root -v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""') {
                    if (env.BRANCH_NAME in branchConfig.pipelineBranches) {
                        echo '🧹 GUARANTEED CLEANUP: Shutting down Selenium Grid...'
                        stopDockerGrid('docker-compose-grid.yml')
                    }
                }
            }
        }
    }
}
