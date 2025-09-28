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
        stage('Determine Trigger Type & Suite') {
            agent any
            steps {
//                script {
//                    def cause = currentBuild.getBuildCauses()[0]
//                    echo "🔍 Build was triggered by: ${cause.shortDescription}"
//
//                    if (cause instanceof hudson.triggers.TimerTrigger$TimerTriggerCause) {
//                        // If triggered by the cron timer, it's a regression run.
//                        env.SUITE_TO_RUN = 'regression'
//                    } else if (cause instanceof hudson.model.Cause$UserIdCause) {
//                        // If a user started it manually, respect their parameter choice.
//                        env.SUITE_TO_RUN = params.SUITE_NAME
//                    } else {
//                        // For all other triggers (like a git push), default to a quick smoke test.
//                        env.SUITE_TO_RUN = 'smoke'
//                    }
//
//                    echo "✅ Pipeline will run the '${env.SUITE_TO_RUN}' suite."
//                }
//				script {
//					def causes = currentBuild.getBuildCauses()
//					// This will now print all trigger descriptions, e.g., "Branch indexing, Started by user..."
//					echo "🔍 Build was triggered by: ${causes*.shortDescription.join(', ')}"
//				
//					// Use .any{} to search the entire list of causes
//					def isManualTrigger = causes.any { it instanceof hudson.model.Cause$UserIdCause }
//					def isTimerTrigger = causes.any { it instanceof hudson.triggers.TimerTrigger$TimerTriggerCause }
//				
//					def suiteToRun
//					if (isTimerTrigger) {
//						suiteToRun = 'regression'
//					} else if (isManualTrigger) {
//						suiteToRun = params.SUITE_NAME
//					} else {
//						// Default for all other triggers (like a git push)
//						suiteToRun = 'smoke'
//					}
//				
//					env.SUITE_TO_RUN = suiteToRun
//					// This will now correctly reflect the user's choice
//					echo "✅ Pipeline will run the '${env.SUITE_TO_RUN}' suite."
//					echo "DEBUG: params.SUITE_NAME='${params.SUITE_NAME}', env.SUITE_TO_RUN='${env.SUITE_TO_RUN}'"
//				}
				script {
					def causes = currentBuild.getBuildCauses()
					def descs  = causes*.shortDescription.join(', ')
					echo "🔍 Build was triggered by: ${descs}"
				  
					def isTimerTrigger  = descs.toLowerCase().contains('timer') || descs.toLowerCase().contains('cron')
					def isManualTrigger = descs.toLowerCase().contains('started by user')
				  
					def suiteToRun = isTimerTrigger ? 'regression' : (params.SUITE_NAME ?: 'smoke')
					env.SUITE_TO_RUN = suiteToRun
				  
					echo "✅ Pipeline will run the '${env.SUITE_TO_RUN}' suite."
					echo "DEBUG: params.SUITE_NAME='${params.SUITE_NAME}', env.SUITE_TO_RUN='${env.SUITE_TO_RUN}'"
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

                    echo "📦 Generating dashboard for suite: ${env.SUITE_TO_RUN}"
                    generateDashboard(env.SUITE_TO_RUN, "${env.BUILD_NUMBER}")
                    archiveAndPublishReports()

					// ✅ This condition is now more flexible. As your project grows, you can add
					 // other important branches like 'main' or 'release' to this list.

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
}