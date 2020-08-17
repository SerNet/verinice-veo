// required plugins:
// - OAuth Credentials plugin, org.jenkins-ci.plugins:oauth-credentials:0.4
// - Google Container Registry Auth0, google-container-registry-auth:0.3

def imageForGradleStages = 'openjdk:11-jdk'
def dockerArgsForGradleStages = '-e GRADLE_USER_HOME=$WORKSPACE/gradle-home -v $HOME/.gradle/caches:/gradle-cache:ro -e GRADLE_RO_DEP_CACHE=/gradle-cache'
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
        sidebarLinks([
            [displayName: 'veoModel', iconFileName: 'document.gif', urlName: 'lastSuccessfulBuild/artifact/doc/model_doc/veoModel.html']
        ])
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
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh 'env'
                buildDescription "${env.GIT_BRANCH} ${env.GIT_COMMIT[0..8]}"
                script {
                    projectVersion = sh(returnStdout: true, script: '''./gradlew properties -q | awk '/^version:/ {print $2}' ''').trim()
                }
            }
        }
        stage('Build') {
            agent {
                docker {
                    image imageForGradleStages
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh './gradlew --no-daemon classes'
            }
        }
        stage('Test') {
            agent {
                docker {
                    image imageForGradleStages
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh './gradlew --no-daemon test'
                jacoco classPattern: '**/build/classes/java/main'
                junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
            }
        }
        stage('Validate Schemas') {
            agent {
                dockerfile {
                    filename 'Dockerfile.ajv'
                }
            }
            steps {
                // It would be better to make veo-adapter:generateEntitySchemas validate the schemas.
                // For now an external tool suffices.
                sh '''
                    find veo-adapter/build/generated_entity_schemas/schemas/entity -type f | {
                        while read schemaFile
                        do
                            if ! ajv compile --all-errors --json-pointers --strict-keywords -s $schemaFile
                            then
                               failure=1
                            fi
                        done
                        [ -z "$failure" ]
                    }
                '''
            }
        }
        stage('Artifacts') {
            agent {
                docker {
                    image imageForGradleStages
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh './gradlew --no-daemon build -x check'
            }
        }
        stage('Analyze') {
            agent {
                docker {
                    image imageForGradleStages
                    args dockerArgsForGradleStages
                }
            }
            steps {
                sh './gradlew --no-daemon check -x test'
                recordIssues(enabledForFailure: true, tools: [spotBugs(pattern: '**/build/reports/spotbugs/main.xml', useRankAsPriority: true, trendChartType: 'NONE')])
                recordIssues(enabledForFailure: true, tools: [pmdParser(pattern: '**/build/reports/pmd/main.xml', trendChartType: 'NONE')])
            }
        }
        stage('Dockerimage') {
            agent any
            steps {
                script {
                    def dockerImage = docker.build('eu.gcr.io/veo-projekt/veo', "--build-arg VEO_VERSION='$projectVersion' --label org.opencontainers.image.version='$projectVersion' --label org.opencontainers.image.revision='$env.GIT_COMMIT' .")
                    // Finally, we'll push the image with several tags:
                    // Pushing multiple tags is cheap, as all the layers are reused.
                    withDockerRegistry(credentialsId: 'gcr:verinice-projekt@gcr', url: 'https://eu.gcr.io') {
                        dockerImage.push("git-${env.GIT_COMMIT}")
                        if (env.GIT_BRANCH == 'master') {
                            dockerImage.push("latest")
                            dockerImage.push("build-${env.BUILD_NUMBER}")
                        } else if (env.GIT_BRANCH ==~ /PR-\d+/) { // we only want to build pull requests
                            // Note that '/' is not allowed in docker tags.
                            def dockertag = env.GIT_BRANCH.replace("/","-")
                            dockerImage.push("${dockertag}")
                            dockerImage.push("${dockertag}-${env.BUILD_NUMBER}")
                        }
                    }
                }
            }
        }
        stage('Postman Tests') {
            environment {
                KEYCLOAK_CREDS = credentials('veo_authentication_credentials')
            }
            agent any
            steps {
                script {
                    def veo = docker.build("veo", "-f postman/Dockerfile .")
                    withDockerNetwork{ n ->
                        docker.image('postgres').withRun("--network ${n} --name database-${n} -e POSTGRES_PASSWORD=postgres") { db ->
                            // TODO check for DB startup
                            sleep 5
                            veo.inside("--network ${n} --name veo-${n} --entrypoint=''"){
                                sh "java -Dlogging.file.name=/veo/veo-rest.log -Dveo-basedir=/veo/veo-data -Dspring.datasource.url=jdbc:postgresql://database-${n}:5432/postgres -Dspring.datasource.username=postgres -Dspring.datasource.password=postgres -Dspring.security.oauth2.resourceserver.jwt.issuer-uri=${env.VEO_AUTH_URL} -Dspring.security.oauth2.resourceserver.jwt.jwk-set-uri=${env.VEO_AUTH_URL}/protocol/openid-connect/certs -Dhttp.proxyHost=cache.sernet.private -Dhttp.proxyPort=3128 -Dhttps.proxyHost=cache.sernet.private -Dhttps.proxyPort=3128 -Dhttps.proxySet=true -Dhttp.proxySet=true -jar ${WORKSPACE}/veo-rest/build/libs/veo-rest-${projectVersion}.jar &"
                                echo 'Waiting for application startup'
                                timeout(1) {
                                    waitUntil {
                                        script {
                                            def r = sh returnStatus:true, script: 'wget -q http://localhost:8070/ -O /dev/null'
                                            return (r == 0);
                                        }
                                    }
                                }
                                def accessToken = sh(
                                  returnStdout:true,
                                  script: 'VEO_USER=$KEYCLOAK_CREDS_USR VEO_USER_PASSWORD=$KEYCLOAK_CREDS_PSW /veo/authenticate -r verinice-veo -c veo-development-client'
                                ).trim()
                                sh "newman run 'postman/verinice.VEO_REST_API.postman_collection.json' --env-var 'accessToken=${accessToken}' --reporters 'cli,junit' --reporter-junit-export='newman-report.xml' --suppress-exit-code"
                                sh "cat /veo/veo-rest.log"
                                junit allowEmptyResults: true, testResults: 'newman-report.xml'
                                sh "cp /veo/veo-rest.log ${WORKSPACE}"
                                archiveArtifacts artifacts: 'veo-rest.log', fingerprint: false
                            }
                        }
                    }
                }
            }
        }
        stage('Trigger Deployment') {
            agent any
            when {
                branch 'master'
            }
            steps {
                build job: 'verinice-veo-deployment/master'
            }
        }
    }
    post {
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
                      excludePattern: 'Jenkinsfile, gradle-home/**, .gradle/**, buildSrc/.gradle/**, */build/**, **/*.pdf, **/*.png, **/*.jpg, **/*.vna'
                    )
                  ]
                )
            }
        }
        success {
            node('') {
                archiveArtifacts artifacts: 'veo-rest/build/libs/*.jar, veo-vna-import/build/libs/*.jar', fingerprint: true
            }
        }
    }
}
