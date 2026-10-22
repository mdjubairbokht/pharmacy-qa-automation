// Declarative pipeline for a Jenkins agent with Docker, JDK 17 and Maven 3.9.
// Required plugins: Pipeline, Docker Pipeline (or docker CLI on the agent), JUnit, Allure Jenkins Plugin.
// Global tools expected (Manage Jenkins > Tools): JDK named "jdk17", Maven named "maven3",
// Allure Commandline named "allure".
pipeline {
    agent any

    tools {
        jdk 'jdk17'
        maven 'maven3'
    }

    parameters {
        choice(name: 'SUITE', choices: ['regression', 'smoke', 'api', 'db', 'ui'],
               description: 'TestNG suite to run after the smoke gate')
    }

    triggers {
        cron('H 6 * * 1-5')   // nightly regression on weekdays
    }

    options {
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    environment {
        COMPOSE_PROJECT_NAME = 'rxcare-ci'
        MVN = 'mvn -B -ntp'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & unit test app') {
            steps {
                sh "${MVN} -f pharmacy-app/pom.xml verify"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'pharmacy-app/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Start test environment') {
            steps {
                // PostgreSQL + app + Selenium standalone Chrome (see docker-compose.yml)
                sh 'docker compose --profile grid up -d --build'
                sh '''
                    for i in $(seq 1 60); do
                      if curl -fs http://localhost:8080/api/health; then echo; echo "App is up"; exit 0; fi
                      sleep 2
                    done
                    docker compose logs app; exit 1
                '''
            }
        }

        stage('Smoke gate') {
            steps {
                sh "${MVN} -f qa-automation/pom.xml test -Dsuite=smoke -Denv=docker"
            }
        }

        stage('Regression') {
            when {
                expression { params.SUITE != 'smoke' }
            }
            steps {
                sh "${MVN} -f qa-automation/pom.xml test -Dsuite=${params.SUITE} -Denv=docker"
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'qa-automation/target/surefire-reports/TEST-*.xml'
            allure includeProperties: false, jdk: '', results: [[path: 'qa-automation/target/allure-results']]
            sh 'docker compose logs app > app.log 2>&1 || true'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'app.log, qa-automation/target/screenshots/**'
            sh 'docker compose --profile grid down -v || true'
        }
        failure {
            echo 'Tests failed - see the Allure report and failure screenshots for details.'
        }
    }
}
