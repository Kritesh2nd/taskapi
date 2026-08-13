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
            description: 'Build and push the Docker image'
        )

        booleanParam(
            name: 'DEPLOY',
            defaultValue: false,
            description: 'Deploy the Docker image on this Jenkins server'
        )

        string(
            name: 'DOCKER_IMAGE_NAME',
            defaultValue: 'moudle8848/taskapi',
            description: 'Docker image repository without tag, e.g. moudle8848/taskapi'
        )
    }

    environment {
        DOCKER_CREDENTIALS_ID = 'docker'

        DEPLOY_DIR = '/opt/taskapi'

        /*
         * Example:
         *
         * DOCKER_IMAGE_NAME = moudle8848/taskapi
         * BUILD_NUMBER      = 18
         *
         * IMAGE_TAG         = moudle8848/taskapi:18
         */
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
                    echo "Java"
                    echo "======================================"

                    java -version

                    echo "======================================"
                    echo "Maven"
                    echo "======================================"

                    mvn -version

                    echo "======================================"
                    echo "Maven Build"
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
                    echo "Building:"
                    echo "$IMAGE_TAG"

                    docker build \
                        -t "$IMAGE_TAG" \
                        .

                    echo ""
                    echo "Docker image built successfully."

                    docker images "$DOCKER_IMAGE_NAME"
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
                        credentialsId: 'docker',
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

                        echo "Docker login successful."

                        echo "======================================"
                        echo "Docker Push"
                        echo "======================================"

                        echo "Pushing:"
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
                        credentialsId: 'docker',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                    sh '''
                        set -e

                        echo "======================================"
                        echo "Deployment"
                        echo "======================================"

                        echo "Deploying on Jenkins server itself."

                        echo ""
                        echo "Deployment directory:"
                        echo "$DEPLOY_DIR"

                        echo ""
                        echo "Docker image:"
                        echo "$IMAGE_TAG"

                        echo ""
                        echo "Checking Docker:"
                        docker --version

                        echo ""
                        echo "Creating deployment directory..."

                        mkdir -p "$DEPLOY_DIR"

                        echo ""
                        echo "Copying docker-compose file..."

                        cp docker-compose.prod.yml \
                           "$DEPLOY_DIR/docker-compose.yml"

                        cd "$DEPLOY_DIR"

                        echo ""
                        echo "Deployment directory contents:"
                        ls -la

                        echo ""
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
                        echo "Docker Compose Pull"
                        echo "======================================"

                        export DOCKER_IMAGE="$DOCKER_IMAGE_NAME"
                        export IMAGE_TAG="$IMAGE_TAG"

                        docker compose pull

                        echo ""
                        echo "======================================"
                        echo "Docker Compose Up"
                        echo "======================================"

                        docker compose up -d

                        echo ""
                        echo "======================================"
                        echo "Running Containers"
                        echo "======================================"

                        docker compose ps

                        echo ""
                        echo "======================================"
                        echo "Cleaning Old Docker Images"
                        echo "======================================"

                        docker image prune -f

                        echo ""
                        echo "Logging out from Docker Hub..."

                        docker logout

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