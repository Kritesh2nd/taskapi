pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    parameters {
        booleanParam(
            name: 'PUSH_DOCKER_IMAGE',
            defaultValue: false,
            description: 'Build and push the Docker image after a successful Maven build'
        )

        booleanParam(
            name: 'DEPLOY',
            defaultValue: false,
            description: 'Deploy the pushed Docker image to the target server'
        )

        string(
            name: 'DOCKER_IMAGE_NAME',
            defaultValue: 'youruser/taskapi',
            description: 'Docker image repository without tag, e.g. youruser/taskapi'
        )
    }

    environment {
        DOCKER_CREDENTIALS_ID = 'docker'
        DEPLOY_SSH_CREDENTIALS_ID = 'deploy-ssh'

        DEPLOY_HOST = 'ubuntu@10.0.1.6'
        DEPLOY_DIR = '/opt/taskapi'

        // Example:
        // DOCKER_IMAGE_NAME = youruser/taskapi
        // BUILD_NUMBER = 11
        // IMAGE_TAG = youruser/taskapi:11
        IMAGE_TAG = "${params.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}"
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        skipDefaultCheckout(false)
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh '''
                    echo "Java version:"
                    java -version

                    echo "Maven version:"
                    mvn -version

                    echo "Building application..."
                    mvn -B -ntp clean compile
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    echo "Running tests..."
                    mvn -B -ntp test
                '''
            }

            post {
                always {
                    junit(
                        testResults: 'target/surefire-reports/*.xml',
                        allowEmptyResults: true
                    )
                }
            }
        }

        stage('Package') {
            steps {
                sh '''
                    echo "Packaging Spring Boot application..."
                    mvn -B -ntp package -DskipTests
                '''
            }

            post {
                success {
                    archiveArtifacts(
                        artifacts: 'target/*.jar',
                        fingerprint: true
                    )
                }
            }
        }

        stage('Docker Build') {
            when {
                expression {
                    return params.PUSH_DOCKER_IMAGE
                }
            }

            steps {
                sh '''
                    echo "======================================"
                    echo "Docker Build"
                    echo "======================================"

                    echo "Docker version:"
                    docker --version

                    echo "Building image:"
                    echo "$IMAGE_TAG"

                    docker build -t "$IMAGE_TAG" .

                    echo "Docker image built successfully."
                '''
            }
        }

        // original deploy code
        // stage('Docker Push') {
        //     when {
        //         expression {
        //             return params.PUSH_DOCKER_IMAGE
        //         }
        //     }

        //     steps {
        //         withCredentials([
        //             usernamePassword(
        //                 credentialsId: 'docker',
        //                 usernameVariable: 'DOCKER_USER',
        //                 passwordVariable: 'DOCKER_PASS'
        //             )
        //         ]) {
        //             sh '''
        //                 set -e

        //                 echo "======================================"
        //                 echo "Docker Login"
        //                 echo "======================================"

        //                 echo "$DOCKER_PASS" | docker login \
        //                     --username "$DOCKER_USER" \
        //                     --password-stdin

        //                 echo "======================================"
        //                 echo "Docker Push"
        //                 echo "======================================"

        //                 echo "Pushing image:"
        //                 echo "$IMAGE_TAG"

        //                 docker push "$IMAGE_TAG"

        //                 echo "Docker image pushed successfully."

        //                 docker logout
        //             '''
        //         }
        //     }
        // }

        // for testing
        stage('Docker Push') {
            when {
                expression {
                    return params.PUSH_DOCKER_IMAGE
                }
            }

            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'docker',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh '''
                        echo "Docker credential loaded"
                        echo "Docker username: $DOCKER_USER"

                        echo "$DOCKER_PASS" | docker login \
                            --username "$DOCKER_USER" \
                            --password-stdin

                        docker push "$IMAGE_TAG"

                        docker logout
                    '''
                }
            }
        }

        stage('Deploy') {
            when {
                expression {
                    return params.DEPLOY
                }
            }

            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'docker',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sshagent(credentials: ['deploy-ssh']) {

                        sh """
                            set -e

                            echo "======================================"
                            echo "Deploy"
                            echo "======================================"

                            echo "Creating deployment directory..."
                            ssh -o StrictHostKeyChecking=no \
                                ${DEPLOY_HOST} \
                                'mkdir -p ${DEPLOY_DIR}'

                            echo "Copying docker-compose file..."
                            scp -o StrictHostKeyChecking=no \
                                docker-compose.prod.yml \
                                ${DEPLOY_HOST}:${DEPLOY_DIR}/docker-compose.yml

                            echo "Deploying image..."

                            ssh -o StrictHostKeyChecking=no ${DEPLOY_HOST} '
                                set -e

                                cd ${DEPLOY_DIR}

                                export DOCKER_USER="${DOCKER_USER}"
                                export DOCKER_IMAGE="${params.DOCKER_IMAGE_NAME}"
                                export IMAGE_TAG="${IMAGE_TAG}"

                                echo "Docker image:"
                                echo "\$DOCKER_IMAGE:\$IMAGE_TAG"

                                echo "${DOCKER_PASS}" | docker login \
                                    --username "${DOCKER_USER}" \
                                    --password-stdin

                                docker compose pull
                                docker compose up -d

                                docker logout

                                docker image prune -f

                                echo "Deployment completed successfully."
                            '
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "Build #${env.BUILD_NUMBER} succeeded."
        }

        failure {
            echo "Build #${env.BUILD_NUMBER} failed. Check the stage logs above."
        }

        always {
            cleanWs()
        }
    }
}