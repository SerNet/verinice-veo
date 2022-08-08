// required plugins:
// - OAuth Credentials plugin, org.jenkins-ci.plugins:oauth-credentials:0.4
// - Google Container Registry Auth0, google-container-registry-auth:0.3

def imageForGradleStages = 'openjdk:17-jdk-bullseye'
def dockerArgsForGradleStages = '-v /data/gradle-homes/executor-$EXECUTOR_NUMBER:/gradle-home -e GRADLE_USER_HOME=/gradle-home'
def projectVersion

def withDockerNetwork(Closure inner) {
    def networkId = UUID.randomUUID().toString()
    try {
        sh "docker network create ${networkId}"
        inner.call(networkId)
    } finally {
        sh "docker network rm ${networkId}"
    }
}


pipeline {
    agent none

    options {
        buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '5'))
        timeout(time: 2, unit: 'HOURS')
    }

    environment {
        // In case the build server exports a custom JAVA_HOME, we fix the JAVA_HOME
        // to the one used by the docker image.
        JAVA_HOME='/usr/local/openjdk-17'
        GRADLE_OPTS='-Dhttp.proxyHost=cache.int.sernet.de -Dhttp.proxyPort=3128 -Dhttps.proxyHost=cache.int.sernet.de -Dhttps.proxyPort=3128'
        // pass -Pci=true to gradle, https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
        ORG_GRADLE_PROJECT_ci=true
    }

    stages {
        stage('Setup') {
            agent {
                docker {
                    image imageForGradleStages
                    alwaysPull true
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh 'env'
                buildDescription "${env.GIT_BRANCH} ${env.GIT_COMMIT[0..8]}"
                script {
                    projectVersion = sh(returnStdout: true, script: '''./gradlew -PciBuildNumber=$BUILD_NUMBER -PciJobName=$JOB_NAME properties -q | awk '/^version:/ {print $2}' ''').trim()
                }
            }
        }
        stage('Build') {
            agent {
                docker {
                    image imageForGradleStages
                    alwaysPull true
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh './gradlew -PciBuildNumber=$BUILD_NUMBER -PciJobName=$JOB_NAME classes generateLicenseReport'
                stash name: "classes", includes: "*/build/classes/*/main/**,*/build/resources/main/**,*/build/lombok/**"
            }
            post {
                always {
                    archiveArtifacts artifacts: 'build/reports/dependency-license/*.*', allowEmptyArchive: true
                }
            }
        }
        stage('Artifacts') {
            agent {
                docker {
                    image imageForGradleStages
                    alwaysPull true
                    args dockerArgsForGradleStages
                }
            }
            steps {
                unstash "classes"
                sh './gradlew -PciBuildNumber=$BUILD_NUMBER -PciJobName=$JOB_NAME build -x check'
                archiveArtifacts artifacts: 'veo-rest/build/libs/*.jar', fingerprint: true
            }
            post {
                always{
                    sh 'rm veo-rest/build/libs/*.jar'
                }
            }
        }
        stage('Dockerimage') {
            agent any
            steps {
                unarchive mapping: ["veo-rest/build/libs/veo-rest-${projectVersion}.jar": "veo-rest/build/libs/veo-rest-${projectVersion}.jar"]
                script {
                    def dockerImage = docker.build("eu.gcr.io/veo-projekt/veo:git-${env.GIT_COMMIT}", "--build-arg VEO_VERSION='$projectVersion' --label org.opencontainers.image.version='$projectVersion' --label org.opencontainers.image.revision='$env.GIT_COMMIT' .")
                    // Finally, we'll push the image with several tags:
                    // Pushing multiple tags is cheap, as all the layers are reused.
                    withDockerRegistry(credentialsId: 'gcr:verinice-projekt@gcr', url: 'https://eu.gcr.io') {
                        dockerImage.push("git-${env.GIT_COMMIT}")
                        if (env.GIT_BRANCH == 'master') {
                            dockerImage.push(projectVersion)
                            dockerImage.push("latest")
                            dockerImage.push("master-build-${env.BUILD_NUMBER}")
                        } else if (env.GIT_BRANCH == 'develop') {
                            dockerImage.push("develop")
                            dockerImage.push("develop-build-${env.BUILD_NUMBER}")
                        }
                    }
                }
            }
            post {
                always{
                    sh 'rm veo-rest/build/libs/*.jar'
                }
            }

        }
        stage('veo-rest test classes') {
            agent {
                docker {
                    image imageForGradleStages
                    alwaysPull true
                    args dockerArgsForGradleStages
                }
            }
            steps {
                unstash "classes"
                sh './gradlew -PciBuildNumber=$BUILD_NUMBER -PciJobName=$JOB_NAME veo-rest:testClasses'
                stash name: "test-classes", includes: "veo-rest/build/classes/*/test/**,veo-rest/build/resources/test/**"
            }
        }
        stage('Post-build steps') {
            parallel {

                stage('Analyze') {
                    agent {
                        docker {
                            image imageForGradleStages
                            alwaysPull true
                            args dockerArgsForGradleStages
                        }
                    }
                    steps {
                        timeout(time: 5, unit: 'MINUTES'){
                            unarchive mapping: ['build/reports/dependency-license/LICENSE-3RD-PARTY.txt': 'build/reports/dependency-license/LICENSE-3RD-PARTY.txt']
                            script {
                                def repositoryFileContent = readFile('LICENSE-3RD-PARTY.txt')
                                def generatedFileContent = readFile('build/reports/dependency-license/LICENSE-3RD-PARTY.txt')
                                def repositoryFileContentWithoutDate = repositoryFileContent.replaceAll(/\RThis report was generated at .+\R/, '')
                                def generatedFileContentWithoutDate = generatedFileContent.replaceAll(/\RThis report was generated at .+\R/, '')
                                if (repositoryFileContentWithoutDate != generatedFileContentWithoutDate){
                                    error 'LICENSE-3RD-PARTY.txt is not up to date, please re-run ./gradlew generateLicenseReport'
                                }
                            }
                            unstash "classes"
                            // work around https://github.com/spotbugs/spotbugs-gradle-plugin/issues/391
                            sh './gradlew -PciBuildNumber=$BUILD_NUMBER -PciJobName=$JOB_NAME check -x test -x spotbugsTest'
                        }
                    }
                    post {
                        failure {
                            recordIssues(enabledForFailure: true, tools: [
                                spotBugs(pattern: '**/build/reports/spotbugs/main.xml', useRankAsPriority: true, trendChartType: 'NONE')
                            ])
                            recordIssues(enabledForFailure: true, tools: [
                                pmdParser(pattern: '**/build/reports/pmd/main.xml', trendChartType: 'NONE')
                            ])
                        }
                    }
                }
                stage('Test') {
                    environment {
                        def tag = "${env.BUILD_TAG}".replaceAll("[^A-Za-z0-9]", "_")
                        RABBITMQ_CREDS = credentials('veo_rabbit_credentials')
                        VEO_MESSAGE_DISPATCH_ROUTINGKEYPREFIX =  "VEO_TEST_${tag}."
                        VEO_MESSAGE_CONSUME_QUEUE = "VEO_TEST_${tag}"
                    }
                    agent any
                    steps {
                        timeout(time: 20, unit: 'MINUTES'){
                            script {
                                withDockerNetwork{ n ->
                                    docker.image('postgres:13.4-alpine').withRun("--network ${n} --name database-${n} -e POSTGRES_USER=test -e POSTGRES_PASSWORD=test") { db ->
                                        docker.image(imageForGradleStages).inside("${dockerArgsForGradleStages} --network ${n} -e SPRING_DATASOURCE_URL=jdbc:postgresql://database-${n}:5432/postgres -e SPRING_DATASOURCE_DRIVERCLASSNAME=org.postgresql.Driver") {
                                            unstash "classes"
                                            unstash "test-classes"
                                            sh '''export SPRING_RABBITMQ_USERNAME=$RABBITMQ_CREDS_USR && \
                                          export SPRING_RABBITMQ_PASSWORD=$RABBITMQ_CREDS_PSW && \
                                          ./gradlew -PciBuildNumber=$BUILD_NUMBER -PciJobName=$JOB_NAME test'''
                                            jacoco classPattern: '**/build/classes/java/main'
                                            junit allowEmptyResults: true,
                                            testResults: '**/build/test-results/test/*.xml',
                                            testDataPublishers: [
                                                [$class: 'StabilityTestDataPublisher']
                                            ]
                                            [
                                                'veo-adapter',
                                                'veo-core-entity',
                                                'veo-core-usecase',
                                                'veo-persistence',
                                                'veo-rest',
                                                'veo-test'
                                            ].each {
                                                publishHTML([
                                                    allowMissing: false,
                                                    alwaysLinkToLastBuild: false,
                                                    keepAll: true,
                                                    reportDir: "${it}/build/reports/tests/test/",
                                                    reportFiles: 'index.html',
                                                    reportName: "Test report: ${it}"
                                                ])
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                stage('HTTP REST Test') {
                    environment {
                        def tag = "${env.BUILD_TAG}".replaceAll("[^A-Za-z0-9]", "_")
                        KEYCLOAK_DEFAULT_CREDS = credentials('veo_authentication_credentials')
                        KEYCLOAK_ADMIN_CREDS = credentials('veo_admin_authentication_credentials')
                        KEYCLOAK_CONTENT_CREATOR_CREDS = credentials('veo_content_creator_authentication_credentials')
                        RABBITMQ_CREDS = credentials('veo_rabbit_credentials')
                        VEO_MESSAGE_DISPATCH_ROUTINGKEYPREFIX =  "VEO_REST_TEST_${tag}."
                        VEO_MESSAGE_CONSUME_QUEUE = "VEO_REST_TEST_${tag}"
                        VEO_RESTTEST_OIDCURL = "${env.OIDC_URL_DEV}"
                        VEO_RESTTEST_REALM = "${env.OIDC_REALM_DEV}"
                        VEO_RESTTEST_CLIENTID = "${env.OIDC_CLIENT_DEV}"
                        VEO_RESTTEST_PROXYHOST = "cache.int.sernet.de"
                        VEO_RESTTEST_PROXYPORT = 3128
                    }
                    agent any
                    steps {
                        unstash "classes"
                        unstash "test-classes"
                        timeout(time: 20, unit: 'MINUTES'){
                            script {
                                withDockerNetwork{ n ->
                                    docker.image('postgres:13.4-alpine').withRun("--network ${n} --name database-${n} -e POSTGRES_USER=test -e POSTGRES_PASSWORD=test") { db ->
                                        docker.image("eu.gcr.io/veo-projekt/veo:git-${env.GIT_COMMIT}").withRun("\
                                --network ${n}\
                                --name veo-${n}\
                                -e SPRING_DATASOURCE_URL=jdbc:postgresql://database-${n}:5432/postgres\
                                -e SPRING_DATASOURCE_USERNAME=test\
                                -e SPRING_DATASOURCE_PASSWORD=test\
                                -e SPRING_DATASOURCE_DRIVERCLASSNAME=org.postgresql.Driver\
                                -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=${env.VEO_AUTH_URL}\
                                -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=${env.VEO_AUTH_URL}/protocol/openid-connect/certs\
                                -e 'VEO_CORS_ORIGINS=https://*.verinice.example, https://frontend.somewhereelse.example'\
                                -e VEO_DEFAULT_DOMAINTEMPLATE_NAMES=DS-GVO,test-domain\
                                -e VEO_ETAG_SALT=zuL4Q8JKdy\
                                -e 'JDK_JAVA_OPTIONS=-Dhttp.proxyHost=cache.int.sernet.de -Dhttp.proxyPort=3128 -Dhttps.proxyHost=cache.int.sernet.de -Dhttps.proxyPort=3128 -Dhttps.proxySet=true -Dhttp.proxySet=true'") { veo ->
                                                    docker.image(imageForGradleStages).inside("${dockerArgsForGradleStages}\
                                    --network ${n}\
                                    -e SPRING_DATASOURCE_URL=jdbc:postgresql://database-${n}:5432/postgres\
                                    -e SPRING_DATASOURCE_DRIVERCLASSNAME=org.postgresql.Driver\
                                    -e VEO_RESTTEST_BASEURL=http://veo-${n}:8070") {
                                                                echo 'Waiting for container startup'
                                                                timeout(2) {
                                                                    waitUntil {
                                                                        script {
                                                                            def r = sh returnStatus:true, script: "wget --no-proxy -q http://veo-${n}:8070 -O /dev/null"
                                                                            return (r == 0);
                                                                        }
                                                                    }
                                                                }
                                                                unstash "classes"
                                                                unstash "test-classes"
                                                                sh """export SPRING_RABBITMQ_USERNAME=\$RABBITMQ_CREDS_USR && \
                                                   export SPRING_RABBITMQ_PASSWORD=\$RABBITMQ_CREDS_PSW && \
                                                   export VEO_RESTTEST_USERS_DEFAULT_NAME=\$KEYCLOAK_DEFAULT_CREDS_USR && \
                                                   export VEO_RESTTEST_USERS_DEFAULT_PASS=\$KEYCLOAK_DEFAULT_CREDS_PSW && \
                                                   export VEO_RESTTEST_USERS_ADMIN_NAME=\$KEYCLOAK_ADMIN_CREDS_USR && \
                                                   export VEO_RESTTEST_USERS_ADMIN_PASS=\$KEYCLOAK_ADMIN_CREDS_PSW && \
                                                   export VEO_RESTTEST_USERS_CONTENTCREATOR_NAME=\$KEYCLOAK_CONTENT_CREATOR_CREDS_USR && \
                                                   export VEO_RESTTEST_USERS_CONTENTCREATOR_PASS=\$KEYCLOAK_CONTENT_CREATOR_CREDS_PSW && \
                                                   ./gradlew -Dhttp.nonProxyHosts=\"localhost|veo-${n}\" -PciBuildNumber=\$BUILD_NUMBER -PciJobName=\$JOB_NAME veo-rest:restTest"""
                                                                junit allowEmptyResults: true, testResults: 'veo-rest/build/test-results/restTest/*.xml'
                                                                publishHTML([
                                                                    allowMissing: false,
                                                                    alwaysLinkToLastBuild: false,
                                                                    keepAll: true,
                                                                    reportDir: 'veo-rest/build/reports/tests/restTest/',
                                                                    reportFiles: 'index.html',
                                                                    reportName: 'Test report: veo-rest-integration-test'
                                                                ])
                                                                perfReport failBuildIfNoResultFile: false,
                                                                modePerformancePerTestCase: true,
                                                                showTrendGraphs: true,
                                                                sourceDataFiles: 'veo-rest/build/test-results/restTest/*.xml'
                                                            }
                                                    sh "docker logs ${veo.id} > rest-test-container-logs.log"
                                                    archive 'rest-test-container-logs.log'
                                                }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        stage('Trigger Deployment') {
            agent none
            when {
                anyOf { branch 'master'; branch 'develop' }
            }
            steps {
                build job: 'verinice-veo-deployment/master'
            }
        }
    }
    post {
        failure {
            emailext body: '${JELLY_SCRIPT,template="text"}', subject: '$DEFAULT_SUBJECT', attachLog: false, recipientProviders: [culprits()]
        }
        always {
            node('') {
                recordIssues(enabledForFailure: true, tools: [java()])
                recordIssues(enabledForFailure: true, tools: [javaDoc()])
                recordIssues(
                        enabledForFailure: true,
                        tools: [
                            taskScanner(
                            highTags: 'FIXME',
                            ignoreCase: true,
                            normalTags: 'TODO',
                            excludePattern: 'Jenkinsfile, gradle/wrapper/**, gradle-home/**, .gradle/**, buildSrc/.gradle/**, build/**, */build/**, **/*.pdf, **/*.png, **/*.jpg'
                            )
                        ]
                        )
            }
        }
    }
}
