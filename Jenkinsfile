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
                    apt-get install -y chromium && \
                    rm -rf /var/lib/apt/lists/*
                '''
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile -q'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn test -Dtest=ContributionServiceTest,EligibilityCheckerTest,MemberServiceTest,MembershipServiceTest,RoundServiceTest,BidServiceTest'
            }
        }

        stage('Selenium Tests') {
            steps {
                sh 'mvn test -Dtest=IqubSeleniumTest'
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
