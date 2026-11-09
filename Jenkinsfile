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
    }

    post {
        always {
            echo 'QA pipeline execution completed.'
        }
        success {
            echo 'Build and unit tests passed.'
        }
        failure {
            echo 'Build or unit tests failed.'
        }
    }
}