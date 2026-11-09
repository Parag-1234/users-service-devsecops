
pipeline {
    agent any

    options {
        timestamps()
        skipDefaultCheckout(true)
    }

    environment {
        IMAGE_NAME = 'users-service'
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
                    set +e

                    /opt/semgrep-venv/bin/semgrep scan \
                      --config auto \
                      --json \
                      --output semgrep-report.json \
                      --error .

                    echo $? > semgrep-exit-code.txt
                    exit 0
                '''
            }
        }

        stage('Build and Unit Tests') {
            steps {
                sh '''
                    set -e

                    mkdir -p "$HOME/.m2/repository"

                    docker run --rm \
                      -v "$WORKSPACE:/workspace" \
                      -v "$HOME/.m2:/root/.m2" \
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

        stage('Hadolint Dockerfile Check') {
            steps {
                sh '''
                    set +e

                    /usr/local/bin/hadolint \
                      --format json Dockerfile \
                      > hadolint-report.json

                    echo $? > hadolint-exit-code.txt
                    exit 0
                '''
            }
        }

        stage('Trivy Filesystem Vulnerability Scan') {
            steps {
                sh '''
                    set +e

                    trivy fs \
                      --scanners vuln \
                      --severity HIGH,CRITICAL \
                      --exit-code 1 \
                      --format json \
                      --output trivy-fs-report.json \
                      .

                    echo $? > trivy-fs-exit-code.txt
                    exit 0
                '''
            }
        }

        stage('Docker Image Build') {
            steps {
                sh '''
                    set -e

                    IMAGE_TAG="qa-$(git rev-parse --short HEAD)"

                    echo "Building ${IMAGE_NAME}:$IMAGE_TAG"

                    docker build \
                      -t "${IMAGE_NAME}:$IMAGE_TAG" \
                      .

                    docker image inspect "${IMAGE_NAME}:$IMAGE_TAG" \
                      --format='Image ID: {{.Id}}'
                '''
            }
        }

        stage('Syft SBOM Generation') {
            steps {
                sh '''
                    set -e

                    IMAGE_TAG="qa-$(git rev-parse --short HEAD)"

                    syft "${IMAGE_NAME}:$IMAGE_TAG" \
                      -o cyclonedx-json \
                      > syft-sbom.cdx.json

                    test -s syft-sbom.cdx.json

                    echo "SBOM generated successfully."
                    ls -lh syft-sbom.cdx.json
                '''
            }
        }

        stage('Trivy Docker Image Vulnerability Scan') {
            steps {
                sh '''
                    set +e

                    IMAGE_TAG="qa-$(git rev-parse --short HEAD)"

                    trivy image \
                      --severity HIGH,CRITICAL \
                      --exit-code 1 \
                      --format json \
                      --output trivy-image-report.json \
                      "${IMAGE_NAME}:$IMAGE_TAG"

                    echo $? > trivy-image-exit-code.txt
                    exit 0
                '''
            }
        }

        stage('Security Policy Gate') {
            steps {
                sh '''
                    set -e

                    FAILED=0

                    for scan in \
                      semgrep \
                      hadolint \
                      trivy-fs \
                      trivy-image
                    do
                        FILE="${scan}-exit-code.txt"

                        if [ ! -f "$FILE" ]; then
                            echo "FAIL: Missing scan result: $FILE"
                            FAILED=1
                            continue
                        fi

                        CODE=$(cat "$FILE")

                        if [ "$CODE" -ne 0 ]; then
                            echo "FAIL: $scan returned exit code $CODE"
                            FAILED=1
                        else
                            echo "PASS: $scan"
                        fi
                    done

                    if [ "$FAILED" -ne 0 ]; then
                        echo "Security policy gate FAILED."
                        exit 1
                    fi

                    echo "All security checks passed."
                '''
            }
        }
    }

    post {
        always {
            echo 'Preparing and publishing QA and security reports...'

            script {
                sh '''
                    python3 - <<'PY'
import json
from html import escape

STYLE = """
<style>
body { font-family: Arial, sans-serif; margin: 30px; }
table { border-collapse: collapse; width: 100%; }
th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
th { background: #eee; }
</style>
"""

def load_json(path):
    try:
        with open(path) as f:
            return json.load(f)
    except Exception:
        return None

def write_table(path, title, headers, rows, message=None):
    with open(path, "w") as f:
        f.write("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
        f.write("<title>" + escape(title) + "</title>")
        f.write(STYLE)
        f.write("</head><body><h1>" + escape(title) + "</h1>")

        if message:
            f.write("<p>" + escape(message) + "</p>")

        f.write("<p>Total findings: " + str(len(rows)) + "</p>")
        f.write("<table><tr>")

        for header in headers:
            f.write("<th>" + escape(header) + "</th>")

        f.write("</tr>")

        for row in rows:
            f.write("<tr>")
            for value in row:
                f.write("<td>" + escape(str(value)) + "</td>")
            f.write("</tr>")

        f.write("</table></body></html>")

# Semgrep
data = load_json("semgrep-report.json")
rows = []

if isinstance(data, dict):
    for item in data.get("results", []):
        extra = item.get("extra", {})
        metadata = extra.get("metadata", {})
        rows.append((
            metadata.get("severity", "INFO"),
            item.get("check_id", ""),
            item.get("path", ""),
            item.get("start", {}).get("line", ""),
            extra.get("message", "")
        ))

write_table(
    "semgrep-report.html",
    "Semgrep Security Report",
    ["Severity", "Rule", "File", "Line", "Message"],
    rows,
    None if data is not None else "Report unavailable or invalid."
)

# Hadolint
data = load_json("hadolint-report.json")
rows = []

if isinstance(data, list):
    for item in data:
        rows.append((
            item.get("code", ""),
            item.get("level", ""),
            item.get("line", ""),
            item.get("message", "")
        ))

write_table(
    "hadolint-report.html",
    "Hadolint Dockerfile Report",
    ["Rule", "Severity", "Line", "Message"],
    rows,
    None if data is not None else "Report unavailable or invalid."
)

# Trivy filesystem and image reports
def make_trivy_report(source, destination, title):
    data = load_json(source)
    rows = []

    if isinstance(data, dict):
        for result in data.get("Results", []):
            target = result.get("Target", "")

            for vuln in result.get("Vulnerabilities") or []:
                rows.append((
                    vuln.get("VulnerabilityID", ""),
                    vuln.get("PkgName", ""),
                    vuln.get("InstalledVersion", ""),
                    vuln.get("FixedVersion", ""),
                    vuln.get("Severity", ""),
                    target
                ))

    write_table(
        destination,
        title,
        ["ID", "Package", "Installed", "Fixed Version", "Severity", "Target"],
        rows,
        None if data is not None else "Report unavailable or invalid."
    )

make_trivy_report(
    "trivy-fs-report.json",
    "trivy-fs-report.html",
    "Trivy Filesystem Vulnerability Report"
)

make_trivy_report(
    "trivy-image-report.json",
    "trivy-image-report.html",
    "Trivy Docker Image Vulnerability Report"
)

print("HTML report generation completed.")
PY
                '''

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

                publishHTML(target: [
                    reportName: 'Semgrep Security Report',
                    reportDir: '.',
                    reportFiles: 'semgrep-report.html',
                    keepAll: true,
                    alwaysLinkToLastBuild: true,
                    allowMissing: true
                ])

                publishHTML(target: [
                    reportName: 'Hadolint Dockerfile Report',
                    reportDir: '.',
                    reportFiles: 'hadolint-report.html',
                    keepAll: true,
                    alwaysLinkToLastBuild: true,
                    allowMissing: true
                ])

                publishHTML(target: [
                    reportName: 'Trivy Filesystem Report',
                    reportDir: '.',
                    reportFiles: 'trivy-fs-report.html',
                    keepAll: true,
                    alwaysLinkToLastBuild: true,
                    allowMissing: true
                ])

                publishHTML(target: [
                    reportName: 'Trivy Image Report',
                    reportDir: '.',
                    reportFiles: 'trivy-image-report.html',
                    keepAll: true,
                    alwaysLinkToLastBuild: true,
                    allowMissing: true
                ])

                archiveArtifacts(
                    artifacts: 'semgrep-report.json, semgrep-report.html, semgrep-exit-code.txt, hadolint-report.json, hadolint-report.html, hadolint-exit-code.txt, trivy-fs-report.json, trivy-fs-report.html, trivy-fs-exit-code.txt, trivy-image-report.json, trivy-image-report.html, trivy-image-exit-code.txt, syft-sbom.cdx.json, smoke-test-report.txt, target/surefire-reports/*.xml, target/site/jacoco/**',
                    allowEmptyArchive: true,
                    fingerprint: true
                )
            }
        }

        success {
            echo 'QA, security scans, image build and SBOM generation passed.'
        }

        failure {
            echo 'Pipeline failed. Check the failed stage and published reports.'
        }
    }
}
