// required plugins:
// - OAuth Credentials plugin, org.jenkins-ci.plugins:oauth-credentials:0.4
// - Google Container Registry Auth0, google-container-registry-auth:0.3

def imageForGradleStages = 'openjdk:11-jdk'
def dockerArgsForGradleStages = '-e GRADLE_USER_HOME=$WORKSPACE/gradle-home'
def projectVersion

def withDockerNetwork(Closure inner) {
  try {
    networkId = UUID.randomUUID().toString()
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
        JAVA_HOME='/usr/local/openjdk-11'
        GRADLE_OPTS='-Dhttp.proxyHost=cache.sernet.private -Dhttp.proxyPort=3128 -Dhttps.proxyHost=cache.sernet.private -Dhttps.proxyPort=3128'
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
                sh './gradlew --no-daemon -PciBuildNumber=$BUILD_NUMBER -PciJobName=$JOB_NAME classes generateLicenseReport'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'build/reports/dependency-license/*.*', allowEmptyArchive: true
                }
            }
        }
        stage('Test') {
            options {
                timeout(time: 20, unit: 'MINUTES')
            }
            environment {
                def tag = "${env.BUILD_TAG}".replaceAll("[^A-Za-z0-9]", "_")
                RABBITMQ_CREDS = credentials('veo_rabbit_credentials')
                VEO_TEST_MESSAGE_DISPATCH_ROUTING_KEY_PREFIX =  "VEO.TESTMESSAGE.${tag}."
                VEO_TEST_MESSAGE_CONSUME_QUEUE = "VEO.ENTITY_TEST_QUEUE_${tag}"
                VEO_TEST_MESSAGE_CONSUME_ROUTING_KEY = "VEO.TESTMESSAGE.${tag}.#"
            }
            agent any
            steps {
                 script {
                     withDockerNetwork{ n ->
                         docker.image('postgres:11.7-alpine').withRun("--network ${n} --name database-${n} -e POSTGRES_USER=test -e POSTGRES_PASSWORD=test") { db ->
                             docker.image(imageForGradleStages).inside("${dockerArgsForGradleStages} --network ${n} -e SPRING_DATASOURCE_URL=jdbc:postgresql://database-${n}:5432/postgres -e SPRING_DATASOURCE_DRIVERCLASSNAME=org.postgresql.Driver") {
                                 sh '''export SPRING_RABBITMQ_USERNAME=$RABBITMQ_CREDS_USR && \
                                       export SPRING_RABBITMQ_PASSWORD=$RABBITMQ_CREDS_PSW && \
                                       ./gradlew --no-daemon -PciBuildNumber=$BUILD_NUMBER -PciJobName=$JOB_NAME test'''
                                 jacoco classPattern: '**/build/classes/java/main'
                                 junit allowEmptyResults: true,
                                         testResults: '**/build/test-results/test/*.xml',
                                         testDataPublishers: [[$class: 'StabilityTestDataPublisher']]
                                 [ 'veo-adapter', 'veo-core-entity', 'veo-core-usecase', 'veo-persistence', 'veo-rest', 'veo-test' ].each {
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
          stage('HTTP REST Test') {
                    options {
                        timeout(time: 20, unit: 'MINUTES')
                    }
                    environment {
                        def tag = "${env.BUILD_TAG}".replaceAll("[^A-Za-z0-9]", "_")
                        KEYCLOAK_DEFAULT_CREDS = credentials('veo_authentication_credentials')
                        KEYCLOAK_ADMIN_CREDS = credentials('veo_admin_authentication_credentials')
                        RABBITMQ_CREDS = credentials('veo_rabbit_credentials')
                        VEO_TEST_MESSAGE_DISPATCH_ROUTING_KEY_PREFIX =  "VEO.RESTTESTMESSAGE.${tag}."
                        VEO_TEST_MESSAGE_CONSUME_QUEUE = "VEO.ENTITY_RESTTEST_QUEUE_${tag}"
                        VEO_TEST_MESSAGE_CONSUME_ROUTING_KEY = "VEO.RESTTESTMESSAGE.${tag}.#"
                        VEO_RESTTEST_OIDCURL = "https://keycloak.staging.verinice.com"
                        VEO_RESTTEST_REALM = "verinice-veo"
                        VEO_RESTTEST_CLIENTID = "veo-development-client"
                        VEO_RESTTEST_PROXYHOST = "cache.sernet.private"
                        VEO_RESTTEST_PROXYPORT = 3128
                    }
                    agent any
                    steps {
                         script {
                             withDockerNetwork{ n ->
                                 docker.image('postgres:11.7-alpine').withRun("--network ${n} --name database-${n} -e POSTGRES_USER=test -e POSTGRES_PASSWORD=test") { db ->
                                     docker.image(imageForGradleStages).inside("${dockerArgsForGradleStages} --network ${n} -e SPRING_DATASOURCE_URL=jdbc:postgresql://database-${n}:5432/postgres -e SPRING_DATASOURCE_DRIVERCLASSNAME=org.postgresql.Driver") {
                                         sh '''export SPRING_RABBITMQ_USERNAME=$RABBITMQ_CREDS_USR && \
                                               export SPRING_RABBITMQ_PASSWORD=$RABBITMQ_CREDS_PSW && \
                                               export VEO_RESTTEST_USERS_DEFAULT_NAME=$KEYCLOAK_DEFAULT_CREDS_USR && \
                                               export VEO_RESTTEST_USERS_DEFAULT_PASS=$KEYCLOAK_DEFAULT_CREDS_PSW && \
                                               export VEO_RESTTEST_USERS_ADMIN_NAME=$KEYCLOAK_ADMIN_CREDS_USR && \
                                               export VEO_RESTTEST_USERS_ADMIN_PASS=$KEYCLOAK_ADMIN_CREDS_PSW && \
                                               ./gradlew --no-daemon -PciBuildNumber=$BUILD_NUMBER -PciJobName=$JOB_NAME veo-rest:restTest -Phttp.proxyHost=cache.sernet.private -Phttp.proxyPort=3128 -Phttps.proxyHost=cache.sernet.private -Phttps.proxyPort=3128'''
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
                                 }
                             }
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
                sh './gradlew --no-daemon -PciBuildNumber=$BUILD_NUMBER -PciJobName=$JOB_NAME build -x check'
                archiveArtifacts artifacts: 'veo-rest/build/libs/*.jar', fingerprint: true
            }
        }
        stage('Analyze') {
            options {
                timeout(time: 5, unit: 'MINUTES')
            }
            agent {
                docker {
                    image imageForGradleStages
                    alwaysPull true
                    args dockerArgsForGradleStages
                }
            }
            steps {
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
                 // work around https://github.com/spotbugs/spotbugs-gradle-plugin/issues/391
                sh './gradlew --no-daemon -PciBuildNumber=$BUILD_NUMBER -PciJobName=$JOB_NAME check -x test -x spotbugsTest'
            }
            post {
                failure {
                    recordIssues(enabledForFailure: true, tools: [spotBugs(pattern: '**/build/reports/spotbugs/main.xml', useRankAsPriority: true, trendChartType: 'NONE')])
                    recordIssues(enabledForFailure: true, tools: [pmdParser(pattern: '**/build/reports/pmd/main.xml', trendChartType: 'NONE')])
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
                            dockerImage.push("latest")
                            dockerImage.push("master-build-${env.BUILD_NUMBER}")
                        } else if (env.GIT_BRANCH == 'develop') {
                            dockerImage.push("develop")
                            dockerImage.push("develop-build-${env.BUILD_NUMBER}")
                        }
                    }
                }
            }
        }

        stage('Postman Tests') {
            options {
                timeout(time: 5, unit: 'MINUTES')
            }
            environment {
                KEYCLOAK_CREDS = credentials('veo_authentication_credentials')
                RABBITMQ_CREDS = credentials('veo_rabbit_credentials')
            }
            agent any
            steps {
            unarchive mapping: ["veo-rest/build/libs/veo-rest-${projectVersion}.jar": "veo-rest/build/libs/veo-rest-${projectVersion}.jar"]
                script {
                    def veo = docker.build("veo", "-f postman/Dockerfile .")
                    withDockerNetwork{ n ->
                        docker.image('postgres:11.7-alpine').withRun("--network ${n} --name database-${n} -e POSTGRES_PASSWORD=postgres") { db ->
                            sh 'until pg_isready; do sleep 1; done'
                            veo.inside("--network ${n} --name veo-${n} --entrypoint=''"){
                                sh "java -Dlogging.file.name=${WORKSPACE}/veo-rest.log -Dveo.etag.salt=zuL4Q8JKdy -Dspring.datasource.url=jdbc:postgresql://database-${n}:5432/postgres -Dspring.datasource.username=postgres -Dspring.datasource.password=postgres -Dspring.security.oauth2.resourceserver.jwt.issuer-uri=${env.VEO_AUTH_URL} -Dveo.etag.salt=pleasemrpostman -Dspring.rabbitmq.username=\$RABBITMQ_CREDS_USR -Dspring.rabbitmq.password=\$RABBITMQ_CREDS_PSW -Dspring.rabbitmq.host=${env.SPRING_RABBITMQ_HOST} -Dspring.rabbitmq.port=${env.SPRING_RABBITMQ_PORT} -Dspring.security.oauth2.resourceserver.jwt.jwk-set-uri=${env.VEO_AUTH_URL}/protocol/openid-connect/certs -Dhttp.proxyHost=cache.sernet.private -Dhttp.proxyPort=3128 -Dhttps.proxyHost=cache.sernet.private -Dhttps.proxyPort=3128 -Dhttps.proxySet=true -Dhttp.proxySet=true -jar ${WORKSPACE}/veo-rest/build/libs/veo-rest-${projectVersion}.jar &"
                                echo 'Waiting for application startup'
                                timeout(1) {
                                    waitUntil {
                                        script {
                                            def r = sh returnStatus:true, script: 'wget -q http://localhost:8070/veo -O /dev/null'
                                            return (r == 0);
                                        }
                                    }
                                }
                                def accessToken = sh(
                                  returnStdout:true,
                                  script: 'VEO_USER=$KEYCLOAK_CREDS_USR VEO_USER_PASSWORD=$KEYCLOAK_CREDS_PSW /veo/authenticate -r verinice-veo -c veo-development-client'
                                ).trim()
                                sh "newman run 'postman/verinice.VEO_REST_API.postman_collection.json' --env-var 'accessToken=${accessToken}' --reporters 'cli,junit' --reporter-junit-export='newman-report.xml' --suppress-exit-code"
                                junit allowEmptyResults: true, testResults: 'newman-report.xml'
                                perfReport failBuildIfNoResultFile: false,
                                                    modePerformancePerTestCase: true,
                                                    showTrendGraphs: true,
                                                    sourceDataFiles: 'newman-report.xml'
                            }
                        }
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'veo-rest.log*', fingerprint: false
                    sh 'rm veo-rest.log*'
                }
            }
        }
        stage('Trigger Deployment') {
            agent any
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
                sh 'rm veo-rest/build/libs/*.jar'
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
