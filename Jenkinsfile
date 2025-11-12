@Library('my-automation-library') _

def branchConfig = getBranchConfig()  // ✅ Load centralized config

pipeline {
    agent none 

    environment {
        NETWORK_NAME = "selenium-grid-${env.BRANCH_NAME}".replaceAll('[^a-zA-Z0-9_.-]', '_')
    }

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
        // force jenkins to run test automatically
        

        stage('Initialize & Start Grid') {
            when {
                expression { return env.BRANCH_NAME in branchConfig.pipelineBranches }
            }
            agent {
                docker {
                    image 'flight-booking-agent-prewarmed:latest'
                    alwaysPull false  // Prevent unnecessary image pulls
                    // --- HANG FIX: Added --entrypoint="" ---
                    args "-v /var/run/docker.sock:/var/run/docker.sock --entrypoint=\"\""
                }
            }
            steps {
                retry(2) {
                    echo " Starting Docker Grid..."

                    // --- LIBRARY FIX: Pass correct Hub URL and Network Name ---
                    startDockerGrid(
                        'docker-compose-grid.yml',         // 1. composeFile
                        120,                               // 2. maxWaitSeconds (default)
                        5,                                 // 3. checkIntervalSeconds (default)
                        'http://selenium-hub:4444/wd/hub'  // 4. hubUrl (our override)
                    )
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

        stage('Build & Run Parallel Tests') {
            when {
                expression { return env.BRANCH_NAME in branchConfig.pipelineBranches }
            }
            agent none
            steps {
                echo "🧪 Running parallel tests for: ${env.SUITE_TO_RUN}"
                retry(2) {
                    timeout(time: 2, unit: 'HOURS') {
                        script {
                            def mvnBase = "mvn -P force-local-cache clean test -P ${env.SUITE_TO_RUN} -Denv=${params.TARGET_ENVIRONMENT} -Dtest.suite=${env.SUITE_TO_RUN} -Dbrowser.headless=true"
                            parallel(
                                Chrome: {
                                    docker.image('flight-booking-agent-prewarmed:latest').inside("-v /var/run/docker.sock:/var/run/docker.sock --network=${env.NETWORK_NAME} --entrypoint=\"\"") {
                                        cleanWs()
                                        checkout scm
                                        sh script: "${mvnBase} -Dbrowser=chrome -Dreport.dir=chrome -Dproject.build.directory=target-chrome", returnStatus: true
                                        // ✅ --- STASH FIX: Stash artifacts from this workspace ---
                                        stash name: 'chrome-artifacts', includes: 'reports/**, **/surefire-reports/**, **/regression-failure-summary.txt', allowEmpty: true
                                    }
                                },
                                Firefox: {
                                    docker.image('flight-booking-agent-prewarmed:latest').inside("-v /var/run/docker.sock:/var/run/docker.sock --network=${env.NETWORK_NAME} --entrypoint=\"\"") {
                                        cleanWs()
                                        checkout scm
                                        sh script: "${mvnBase} -Dbrowser=firefox -Dreport.dir=firefox -Dproject.build.directory=target-firefox", returnStatus: true
                                        // ✅ --- STASH FIX: Stash artifacts from this workspace ---
                                        stash name: 'firefox-artifacts', includes: 'reports/**, **/surefire-reports/**, **/regression-failure-summary.txt', allowEmpty: true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

post {
        always {
            script {

                // === STEP 1: Process artifacts in an agent ===
                docker.image('flight-booking-agent-prewarmed:latest').inside('-v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""') {
                    echo "--- Starting Guaranteed Post-Build Processing (on agent) ---"
                    try {
                        unstash 'chrome-artifacts'
                    } catch (e) {
                        echo "⚠️ Chrome artifacts not found to unstash."
                    }
                    try {
                        unstash 'firefox-artifacts'
                    } catch (e) {
                        echo "⚠️ Firefox artifacts not found to unstash."
                    }

                    sh "cp reports/chrome/${env.SUITE_TO_RUN}-chrome-report.html reports/chrome/index.html || echo 'Chrome report not found'"
                    sh "cp reports/firefox/${env.SUITE_TO_RUN}-firefox-report.html reports/firefox/index.html || echo 'Firefox report not found'"
                    junit testResults: '**/surefire-reports/**/*.xml', allowEmptyResults: true
                    generateDashboard(env.SUITE_TO_RUN, "${env.BUILD_NUMBER}")
                    archiveAndPublishReports()

                    echo "Stashing reports for notification step."
                    stash name: 'email-reports', includes: 'reports/**'
                    stash name: 'qase-results', includes: '**/testng-results.xml'
                }

                // === STEP 2: Unified Notifications (on Controller) ===
                node {
                    echo "--- Preparing notifications on Jenkins Controller (in a node context) ---"
                    try {
                        unstash 'email-reports'
                        unstash 'qase-results'
                    } catch (e) {
                        echo "⚠️ Could not unstash reports for notification: ${e.getMessage()}"
                    }

                    checkout scm

                    // --- 2a. Qase Update Logic (Prod Branches Only) ---
                    if (env.BRANCH_NAME in branchConfig.productionCandidateBranches) {
                        echo "🚀 Running Qase update for production-candidate branch..."
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
                            }
                        } catch (err) {
                            echo "⚠️ Qase update failed: ${err.getMessage()}"
                        }
                    }

                    // --- 2b. Unified Email Logic ---
                    def currentResult = currentBuild.result ?: 'SUCCESS'
                    def previousResult = currentBuild.previousBuild?.result ?: 'SUCCESS'

                    // Only send notifications for failed or unstable builds
                    if (currentResult == 'UNSTABLE' || currentResult == 'FAILURE') {

                        // POLISH 1: Only email if the status has CHANGED (e.g., SUCCESS -> UNSTABLE)
                        if (currentResult != previousResult) {
                            echo "✅ Build status changed to ${currentResult}. Sending notification."
                            try {
                                // POLISH 2: Pass branchName to the library for branch-aware recipients
                                sendBuildSummaryEmail(
                                    suiteName: env.SUITE_TO_RUN,
                                    branchName: env.BRANCH_NAME
                                )
                            } catch (err) {
                                echo "⚠️ Email notification failed: ${err.getMessage()}"
                            }
                        } else {
                            echo "ℹ️ Skipping email: Build status (${currentResult}) is unchanged from previous build."
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
            script {
                echo "⚠️ Build UNSTABLE. Tests failed. Check the 'Test Dashboard' for detailed results."
                echo "⏱️ Build duration: ${currentBuild.durationString}"
                if (env.BRANCH_NAME in branchConfig.productionCandidateBranches) {
                    error("❌ Failing build due to test failures in protected branch '${env.BRANCH_NAME}'.")
                }
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
                docker.image('flight-booking-agent-prewarmed:latest').inside('-v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""') {
                    echo '🧹 GUARANTEED CLEANUP: Shutting down Selenium Grid...'
                    stopDockerGrid('docker-compose-grid.yml')
                }
            }
        }
    }
}