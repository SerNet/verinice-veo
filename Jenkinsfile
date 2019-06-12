pipeline {
    agent any

    environment {
        GRADLE_OPTS='-Dhttp.proxyHost=cache.sernet.private -Dhttp.proxyPort=3128 -Dhttps.proxyHost=cache.sernet.private -Dhttps.proxyPort=3128'
        // pass -Pci=true to gradle, https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
        ORG_GRADLE_PROJECT_ci=true
    }

    stages {
        stage('Setup') {
            steps {
                sh 'env'
                notifyBB()
                buildDescription "${env.GIT_BRANCH} ${env.GIT_COMMIT[0..8]}"
            }
        }
        stage('Build') {
            steps {
                sh './gradlew classes'
            }
        }
        stage('Test') {
            steps {
                sh './gradlew test'
                sh './gradlew jenkinsTestFix'
            }
        }
        stage('Analyze') {
            steps {
                sh './gradlew check'
            }
        }
        stage('Artifacts') {
            steps {
                sh './gradlew build'
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
            notifyBB()
        }
        success {
            archiveArtifacts artifacts: 'veo-rest/build/libs/*.jar, veo-vna-import/build/libs/*.jar', fingerprint: true
        }
    }
}

def notifyBB() {
    notifyBitbucket commitSha1: '', considerUnstableAsSuccess: false, credentialsId: 'bitbucket', disableInprogressNotification: false, ignoreUnverifiedSSLPeer: false, includeBuildNumberInKey: false, prependParentProjectKey: false, projectKey: '', stashServerBaseUrl: 'https://git.verinice.org/bb'
}

