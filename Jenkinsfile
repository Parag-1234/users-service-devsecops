
pipeline {
    agent any

    options {
        timestamps()
        skipDefaultCheckout(true)
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Semgrep Security Scan') {
            steps {
                sh '''
                    /opt/semgrep-venv/bin/semgrep scan \
                      --config auto \
                      --error .
                '''
            }
        }

        stage('Build and Unit Tests') {
            steps {
                sh '''
                    docker run --rm \
                      -v "$WORKSPACE:/workspace" \
                      -w /workspace \
                      maven:3.9.9-eclipse-temurin-21 \
                      mvn clean verify
                '''
            }
        }

        stage('Publish JaCoCo Report') {
            steps {
                publishHTML(target: [
                    reportName: 'JaCoCo Coverage Report',
                    reportDir: 'target/site/jacoco',
                    reportFiles: 'index.html',
                    keepAll: true,
                    alwaysLinkToLastBuild: true,
                    allowMissing: false
                ])
            }
        }
    }

    post {
        always {
            echo 'QA pipeline execution completed.'
        }
        success {
            echo 'Security scan, build and unit tests passed.'
        }
        failure {
            echo 'QA pipeline failed. Check the failed stage.'
        }
    }
}
