pipeline {
    agent {
        docker {
            image 'openjdk:11-jdk'
            args '-v $HOME/.gradle:/root/.gradle'
        }
    }

    environment {
        GRADLE_OPTS='-Dhttp.proxyHost=cache.sernet.private -Dhttp.proxyPort=3128 -Dhttps.proxyHost=cache.sernet.private -Dhttps.proxyPort=3128'
        // pass -Pci=true to gradle, https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
        ORG_GRADLE_PROJECT_ci=true
    }

    stages {
        stage('Setup') {
            steps {
                sh 'env'
                buildDescription "${env.GIT_BRANCH} ${env.GIT_COMMIT[0..8]}"
            }
        }
        stage('Build') {
            steps {
                sh './gradlew --no-daemon classes'
            }
        }
        stage('Test') {
            steps {
                sh './gradlew --no-daemon test'
                sh './gradlew --no-daemon jenkinsTestFix'
            }
        }
        stage('Analyze') {
            steps {
                sh './gradlew --no-daemon check'
            }
        }
        stage('Artifacts') {
            steps {
                sh './gradlew --no-daemon build'
            }
        }
    }
    post {
        always {
            recordIssues(tools: [spotBugs(pattern: '**/build/reports/spotbugs/main.xml', useRankAsPriority: true)])
            recordIssues(tools: [pmdParser(pattern: '**/build/reports/pmd/main.xml')])
            recordIssues(tools: [java()])
            recordIssues(tools: [javaDoc()])
            recordIssues(tools: [taskScanner(highTags: 'FIXME', ignoreCase: true, normalTags: 'TODO', excludePattern: 'Jenkinsfile')])
            jacoco classPattern: '**/build/classes/java/main'
            junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
        }
        success {
            archiveArtifacts artifacts: 'veo-rest/build/libs/*.jar, veo-vna-import/build/libs/*.jar', fingerprint: true
        }
    }
}
