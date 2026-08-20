pipeline {
    agent any

    tools {
        maven 'Maven-3.9'
        jdk 'JDK-17'
    }

    parameters {
        booleanParam(
            name: 'PUSH_DOCKER_IMAGE',
            defaultValue: true,
            description: 'Build and push the Docker image after a successful Maven build'
        )

        booleanParam(
            name: 'DEPLOY',
            defaultValue: true,
            description: 'Deploy the Docker image to the Jenkins agent server'
        )

        string(
            name: 'DOCKER_IMAGE_NAME',
            defaultValue: 'moudle8848/taskapi',
            description: 'Docker image repository without tag, e.g. moudle8848/taskapi'
        )
    }

    environment {
        DOCKER_CREDENTIALS_ID = 'kritesh_docker_key'

        DEPLOY_DIR = '/opt/taskapi-kritesh'

        IMAGE_TAG = "${params.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}"
    }

    options {
        timestamps()

        buildDiscarder(
            logRotator(
                numToKeepStr: '10'
            )
        )

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
                    set -e

                    echo "======================================"
                    echo "Java Version"
                    echo "======================================"

                    java -version

                    echo ""
                    echo "======================================"
                    echo "Maven Version"
                    echo "======================================"

                    mvn -version

                    echo ""
                    echo "======================================"
                    echo "Building Application"
                    echo "======================================"

                    mvn -B -ntp clean compile
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    set -e

                    echo "======================================"
                    echo "Running Tests"
                    echo "======================================"

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
                    set -e

                    echo "======================================"
                    echo "Packaging Spring Boot Application"
                    echo "======================================"

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
                    set -e

                    echo "======================================"
                    echo "Docker Build"
                    echo "======================================"

                    echo "Docker version:"
                    docker --version

                    echo ""
                    echo "Building image:"
                    echo "$IMAGE_TAG"

                    docker build \
                        -t "$IMAGE_TAG" \
                        .

                    echo ""
                    echo "Docker image built successfully."
                '''
            }
        }

        stage('Docker Push') {
            when {
                expression {
                    return params.PUSH_DOCKER_IMAGE
                }
            }

            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: "${DOCKER_CREDENTIALS_ID}",
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh '''
                        set -e

                        echo "======================================"
                        echo "Docker Login"
                        echo "======================================"

                        echo "$DOCKER_PASS" | docker login \
                            --username "$DOCKER_USER" \
                            --password-stdin

                        echo ""
                        echo "Docker login successful."

                        echo ""
                        echo "======================================"
                        echo "Docker Push"
                        echo "======================================"

                        echo "Pushing image:"
                        echo "$IMAGE_TAG"

                        docker push "$IMAGE_TAG"

                        echo ""
                        echo "Docker image pushed successfully."

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
                        credentialsId: "${DOCKER_CREDENTIALS_ID}",
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    ),
                    sshUserPrivateKey(
                        credentialsId: 'kritesh_ec2_cred',
                        keyFileVariable: 'SSH_KEY',
                        usernameVariable: 'SSH_USER'
                    )
                ]) {
                    sh '''
                        set -e

                        DEPLOY_HOST="10.1.67.189"

                        echo "======================================"
                        echo "Deploy"
                        echo "======================================"

                        echo "Deployment server:"
                        ssh -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=no \
                            "$SSH_USER@$DEPLOY_HOST" hostname

                        echo ""
                        echo "Image:"
                        echo "$IMAGE_TAG"

                        echo ""
                        echo "======================================"
                        echo "Preparing Deployment Directory"
                        echo "======================================"

                        ssh -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=no \
                            "$SSH_USER@$DEPLOY_HOST" \
                            "sudo mkdir -p /opt/taskapi-kritesh"

                        echo ""
                        echo "======================================"
                        echo "Copying Compose File"
                        echo "======================================"

                        scp -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=no \
                            docker-compose.prod.yml \
                            "$SSH_USER@$DEPLOY_HOST:/tmp/docker-compose.yml"

                        ssh -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=no \
                            "$SSH_USER@$DEPLOY_HOST" \
                            "sudo mv /tmp/docker-compose.yml /opt/taskapi-kritesh/docker-compose.yml"

                        echo ""
                        echo "======================================"
                        echo "Deploying"
                        echo "======================================"

                        ssh -i "$SSH_KEY" \
                            -o StrictHostKeyChecking=no \
                            "$SSH_USER@$DEPLOY_HOST" \
                            "export IMAGE_TAG='$IMAGE_TAG' && \
                            echo '$DOCKER_PASS' | docker login \
                                --username '$DOCKER_USER' \
                                --password-stdin && \
                            cd /opt/taskapi-kritesh && \
                            docker pull '$IMAGE_TAG' && \
                            docker compose up -d && \
                            docker compose ps && \
                            docker image prune -f && \
                            docker logout"

                        echo ""
                        echo "======================================"
                        echo "Deployment Successful"
                        echo "======================================"
                    '''
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