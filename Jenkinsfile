
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
                      --json \
                      --output semgrep-report.json \
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

        stage('Docker Runtime and API Smoke Tests') {
            steps {
                sh '''
                    set -e

                    REPORT="smoke-test-report.txt"
                    echo "Docker Runtime and API Smoke Test Report" > "$REPORT"
                    echo "=========================================" >> "$REPORT"

                    log() {
                        echo "$1" | tee -a "$REPORT"
                    }

                    docker rm -f users-service-qa-app 2>/dev/null || true

                    docker run -d --rm \
                      --name users-service-qa-app \
                      -p 8081:8080 \
                      -v "$WORKSPACE/target/users-service-devsecops-0.0.1-SNAPSHOT.jar:/app/app.jar:ro" \
                      eclipse-temurin:21-jre \
                      java -jar /app/app.jar

                    cleanup() {
                        docker stop users-service-qa-app >/dev/null 2>&1 || true
                    }
                    trap cleanup EXIT

                    log "Waiting for Spring Boot application..."

                    READY=false
                    for attempt in $(seq 1 30); do
                        if curl -fsS http://localhost:8081/api/users/system-info -o /dev/null; then
                            READY=true
                            break
                        fi
                        sleep 2
                    done

                    if [ "$READY" != "true" ]; then
                        log "FAIL: Application failed to start."
                        docker logs users-service-qa-app 2>&1 | tee -a "$REPORT" || true
                        exit 1
                    fi

                    log "PASS: Spring Boot application started."

                    log "Testing system-info API..."
                    if curl -fsS http://localhost:8081/api/users/system-info -o /tmp/system-info-response.txt; then
                        log "PASS: GET /api/users/system-info"
                        cat /tmp/system-info-response.txt | tee -a "$REPORT"
                    else
                        log "FAIL: GET /api/users/system-info"
                        exit 1
                    fi

                    log "Testing get-all-users API..."
                    if curl -fsS http://localhost:8081/api/users -o /tmp/users-response.txt; then
                        log "PASS: GET /api/users"
                        cat /tmp/users-response.txt | tee -a "$REPORT"
                    else
                        log "FAIL: GET /api/users"
                        exit 1
                    fi

                    log "========================================="
                    log "RESULT: All API smoke tests passed."
                '''
            }
        }
    }

    post {
        always {
            echo 'Publishing available test and security reports...'

            junit(
                testResults: 'target/surefire-reports/*.xml',
                allowEmptyResults: true
            )

            publishHTML(target: [
                reportName: 'JaCoCo Coverage Report',
                reportDir: 'target/site/jacoco',
                reportFiles: 'index.html',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: true
            ])

            archiveArtifacts(
                artifacts: 'semgrep-report.json, smoke-test-report.txt, target/surefire-reports/*.xml, target/site/jacoco/**',
                allowEmptyArchive: true,
                fingerprint: true
            )
        }

        success {
            echo 'Security scan, build, unit tests and API smoke tests passed.'
        }

        failure {
            echo 'QA pipeline failed. Check the failed stage and available reports.'
        }
    }
}
