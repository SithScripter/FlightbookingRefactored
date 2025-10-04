@Library('my-automation-library') _

def branchConfig = getBranchConfig()  // ✅ Load centralized config

pipeline {
    // ✅ We define no top-level agent. This enables a flexible multi-agent
    // strategy where each stage can use its own specialized environment.
    agent none 

    options {
        // ✅ Prevents Jenkins from doing an initial checkout on the controller.
        // We will handle the checkout manually inside the correct agent.
        skipDefaultCheckout()
    }

    triggers {
        // ✅ Automatically schedules a build to run every night at approximately 2 AM.
        // This trigger will be identified by the pipeline to run the 'regression' suite.
        cron('H 2 * * *')
    }

    parameters {
        // ✅ This parameter is now ONLY for manual builds. Automated runs will ignore it.
        choice(name: 'SUITE_NAME', choices: ['smoke', 'regression'], description: 'Select suite (only applies to manual runs).')

        // ✅ The target environment for test execution.
        choice(name: 'TARGET_ENVIRONMENT', choices: ['PRODUCTION', 'STAGING', 'QA'], description: 'Select test environment.')

        // ✅ An optional manual approval gate, used only by the regression suite.
        booleanParam(name: 'MANUAL_APPROVAL', defaultValue: false, description: '🛑 Only relevant if "regression" is selected. Ignored for "smoke".')

        // ✅ An optional override for Qase test case IDs.
        string(name: 'QASE_TEST_CASE_IDS', defaultValue: '', description: 'Optional: Override default Qase IDs.')
    }

    stages {

        // This initial stage runs on a lightweight agent to determine which suite to run.
        // This is a safe and robust pattern that avoids running complex logic in unsupported places.
        stage('Determine Suite') {
            agent any
            steps {
				script {
				    env.SUITE_TO_RUN = determineTestSuite()
				  }
            }
        }

        // This stage prepares the environment by starting the Selenium Grid.
        stage('Initialize & Start Grid') {
            when {
                expression { return env.BRANCH_NAME in branchConfig.pipelineBranches }
            }
            agent {
                docker {
                    image 'flight-booking-agent:latest'
                    args '-u root -v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""'
                }
            }
            steps {
                initializeTestEnvironment(env.SUITE_TO_RUN)  // Clean orchestration!
            }
        }

        // This stage is only active for regression runs when the user explicitly toggles it on.
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

        stage('Build & Run Parallel Tests') {
            when {
                expression { 
                    def branchName = env.BRANCH_NAME ?: 'unknown'
                    echo "DEBUG: Branch name for test execution: ${branchName}"
                    return branchName in branchConfig.pipelineBranches
                }
            }
            agent {
                docker {
                    image 'flight-booking-agent:latest'
                    args '-u root -v /var/run/docker.sock:/var/run/docker.sock --entrypoint="" --network=selenium_grid_network'
                }
            }
            steps {
                echo "🧪 Running parallel tests for: ${env.SUITE_TO_RUN}"
                timeout(time: 2, unit: 'HOURS') {
                    script {
                        def mvnBase = "mvn clean test -P ${env.SUITE_TO_RUN} -Denv=${params.TARGET_ENVIRONMENT} -Dtest.suite=${env.SUITE_TO_RUN} -Dbrowser.headless=true"
                        parallel(
                            Chrome: {
                                sh "${mvnBase} -Dbrowser=CHROME -Dreport.dir=chrome -Dmaven.repo.local=.m2-chrome"
                            },
                            Firefox: {
                                sh "${mvnBase} -Dbrowser=FIREFOX -Dreport.dir=firefox -Dmaven.repo.local=.m2-firefox"
                            }
                        )
                    }
                }
                // ✅ Stash screenshots for cross-container transfer to post block
                echo "Stashing screenshots for archiving..."
                stash name: 'test-screenshots', includes: 'reports/screenshots/**', allowEmpty: true
            }
        }

        stage('Post-Build Actions') {
            when {
                expression { return env.BRANCH_NAME in branchConfig.pipelineBranches }
            }
            //Force agent to run on the same machine as the build
            agent {
                docker {
                    image 'flight-booking-agent:latest'
                    args '-u root -v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""'
                }
            }

            steps {
                script {
                    echo "DEBUG: Suite name at start of post-build is '${env.SUITE_TO_RUN}'"

                    // Generate HTML dashboard using shared library
                    generateDashboard(env.SUITE_TO_RUN, "${env.BUILD_NUMBER}")

                    // Archive and publish reports using shared library
                    archiveAndPublishReports()

                    // ✅ Use centralized config for Qase integration
                    if (env.BRANCH_NAME in branchConfig.productionCandidateBranches) {
                        echo "🚀 Running notifications for production-candidate branch: ${env.BRANCH_NAME}"
                        try {
                            def qaseConfig = readJSON file: 'cicd/qase_config.json'
                            def suiteSettings = qaseConfig[env.SUITE_TO_RUN]
                            if (!suiteSettings) {
                                error "❌ Qase config missing for suite: ${env.SUITE_TO_RUN}"
                            }

                            // Use parameter override if provided, otherwise use config file.
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
                        } catch (err) {
                            echo "⚠️ Post-build notification failed: ${err.getMessage()}"
                        }
                    } else {
                        echo "ℹ️ Skipping post-build notifications for branch: ${env.BRANCH_NAME}"
                    }
                }
            }
        }
    }

    post {
        // 'always' ensures these steps run regardless of the build's success or failure.
        always {
            script {
                // Use the 'inside' step to run all cleanup, reporting, and notifications inside our container.
                // This is needed because we're using 'agent none' at the top level.
                docker.image('flight-booking-agent:latest').inside('-u root -v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""') {

                    echo "Publishing test results and handling screenshots..."

                    // ✅ ADD: Native Jenkins junit step - automatically sets build to UNSTABLE on failures
                    junit testResults: 'target/surefire-reports/**/*.xml', allowEmptyResults: true

                    // ✅ Unstash screenshots from test stage for archiving
                    echo "Unstashing screenshots for archiving..."
                    script {
                        try {
                            unstash 'test-screenshots'
                        } catch (Exception e) {
                            echo "⚠️ Screenshot stash not found (test stage may have failed before stashing): ${e.getMessage()}"
                            // Continue with archiving - no screenshots to archive
                        }
                    }
                    // Archive screenshots separately since shared library doesn't include them
                    archiveArtifacts artifacts: 'reports/screenshots/**', allowEmptyArchive: true
                }
            }
        }
        unstable {
            script {
                echo "⚠️ Build is UNSTABLE due to test failures. Check the 'Test Automation Dashboard' for detailed results."
                // Future: Add notification logic here
            }
        }
        failure {
            script {
                echo "❌ Build FAILED. There was an error during the build or test execution."
            }
        }
        success {
            script {
                echo "✅ Build SUCCESS. All tests passed."
            }
        }
        cleanup {
            // This is GUARANTEED to run last, making it the perfect place for resource cleanup
            node {
                agent {
                    docker {
                        image 'flight-booking-agent:latest'
                        args '-u root -v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""'
                    }
                }
                steps {
                    echo '🧹 GUARANTEED CLEANUP: Shutting down Selenium Grid...'
                    // NOTE: It is safe to call stopDockerGrid even if the grid is already down.
                    stopDockerGrid('docker-compose-grid.yml')
                }
            }
        }
    }
}