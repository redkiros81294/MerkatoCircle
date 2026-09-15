pipeline {
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-21'
            args '-u root' // allow Chrome install
        }
    }

    options {
        timeout(time: 15, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Install Chrome') {
            steps {
                sh '''
                    apt-get update && \
                    apt-get install -y curl unzip jq && \
                    rm -rf /var/lib/apt/lists/*

                    # Download a version-matched Chrome + ChromeDriver pair straight from
                    # Google's "Chrome for Testing" API. Ubuntu no longer ships real,
                    # non-snap chromium/chromium-driver apt packages (they're empty
                    # transitional packages that wrap the Snap Store build, which fails
                    # with no snapd), so this is the reliable path for both CI systems.
                    JSON_URL="https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json"
                    CHROME_URL=$(curl -sL "$JSON_URL" | jq -r '.channels.Stable.downloads.chrome[] | select(.platform=="linux64") | .url')
                    DRIVER_URL=$(curl -sL "$JSON_URL" | jq -r '.channels.Stable.downloads.chromedriver[] | select(.platform=="linux64") | .url')

                    mkdir -p /opt/chrome-for-testing
                    curl -sL "$CHROME_URL" -o /tmp/chrome.zip
                    curl -sL "$DRIVER_URL" -o /tmp/chromedriver.zip
                    unzip -q -o /tmp/chrome.zip -d /opt/chrome-for-testing
                    unzip -q -o /tmp/chromedriver.zip -d /opt/chrome-for-testing
                '''
            }
        }

        stage('Build') {
            steps {
                sh 'mkdir -p data && mvn clean compile -q'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn test -Dtest=ContributionServiceTest,EligibilityCheckerTest,MemberServiceTest,MembershipServiceTest,RoundServiceTest,BidServiceTest'
            }
        }

        stage('Selenium Tests') {
            steps {
                withEnv(["CHROME_BIN=/opt/chrome-for-testing/chrome-linux64/chrome", "CHROMEDRIVER_BIN=/opt/chrome-for-testing/chromedriver-linux64/chromedriver"]) {
                    sh 'mvn test -Dtest=IqubSeleniumTest'
                }
            }
        }

        stage('Coverage Report') {
            steps {
                sh 'mvn jacoco:report'
            }
        }
    }

    post {
        always {
            publishHTML([
                reportDir: 'target/site/jacoco',
                reportFiles: 'index.html',
                reportName: 'JaCoCo Coverage Report'
            ])
            junit 'target/surefire-reports/*.xml'
        }
    }
}
