@Library('my-automation-library') _

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
            when { branch 'enhancements' }
            agent {
                docker {
                    image 'flight-booking-agent:latest'
                    args '-u root -v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""'
                }
            }
            steps {
				echo "🚀 Starting Selenium Grid for suite: '${env.SUITE_TO_RUN}'"
                cleanWs()
                checkout scm
                printBuildMetadata(env.SUITE_TO_RUN)

                // Retry starting the grid to handle any temporary network flakiness.
                retry(2) {
                    startDockerGrid('docker-compose-grid.yml', 20)
                }
            }
        }

        // This stage is only active for regression runs when the user explicitly toggles it on.
        stage('Approval Gate (Regression Only)') {
            when {
                allOf {
                    branch 'enhancements'
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

        // This stage executes the tests in parallel across Chrome and Firefox.
        stage('Build & Run Parallel Tests') {
            when { branch 'enhancements' }
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
            }
        }

        stage('Post-Build Actions') {
            when { branch 'enhancements' }
            agent {
                docker {
                    image 'flight-booking-agent:latest'
                    args '-u root -v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""'
                }
            }
            steps {
                script {
                    echo "DEBUG: Suite name at start of post-build is '${env.SUITE_TO_RUN}'"

                    if (env.BRANCH_NAME == 'enhancements') {
                        echo '🧹 Shutting down Selenium Grid...'
                        stopDockerGrid('docker-compose-grid.yml')
                    }

                    // Qase integration (only for specific branches)
                    if (env.BRANCH_NAME in ['enhancements', 'main']) {
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

                    echo "📦 Generating dashboard for suite: ${env.SUITE_TO_RUN}"
                    generateDashboard(env.SUITE_TO_RUN, "${env.BUILD_NUMBER}")
                    archiveAndPublishReports()
                    
                    // Archive screenshots separately since shared library doesn't include them
                    archiveArtifacts artifacts: 'screenshots/**', allowEmptyArchive: true
                    
                    // 🆕 QUALITY GATE: Check test results and set build status
                    def testFailures = checkTestFailures()
                    
                    if (testFailures > 0) {
                        currentBuild.result = 'UNSTABLE'
                        echo "🚨 Quality Gate: FAILED - ${testFailures} test failures detected. Build marked as UNSTABLE."
                    } else {
                        echo "✅ Quality Gate: PASSED - All tests successful."
                    }
                }
            }
        }
    }
}