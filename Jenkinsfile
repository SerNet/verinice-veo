// Jenkinsfile to build the project, run tests and generate build reports
pipeline {
    agent {
        docker {
            image 'openjdk:11-jdk'
            args '-v $HOME/.gradle:/root/.gradle'
        }
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '5'))
    }

    environment {
        // In case the build server exports a custom JAVA_HOME, we fix the JAVA_HOME
        // to the one used by the docker image.
        JAVA_HOME='/usr/local/openjdk-11'
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
            recordIssues(enabledForFailure: true, tools: [spotBugs(pattern: '**/build/reports/spotbugs/main.xml', useRankAsPriority: true)])
            recordIssues(enabledForFailure: true, tools: [pmdParser(pattern: '**/build/reports/pmd/main.xml')])
            recordIssues(enabledForFailure: true, tools: [java()])
            recordIssues(enabledForFailure: true, tools: [javaDoc()])
            recordIssues(enabledForFailure: true, tools: [taskScanner(highTags: 'FIXME', ignoreCase: true, normalTags: 'TODO', excludePattern: 'Jenkinsfile, **/.gradle/**')])
            jacoco classPattern: '**/build/classes/java/main'
            junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
        }
        success {
            archiveArtifacts artifacts: 'veo-rest/build/libs/*.jar, veo-vna-import/build/libs/*.jar', fingerprint: true
        }
    }
}
