pipeline {
    agent any

    tools {
        maven 'Maven-3.9'   // must match a Maven installation name configured in Jenkins > Global Tool Configuration
        jdk 'JDK-17'        // must match a JDK installation name configured in Jenkins > Global Tool Configuration
    }

    parameters {
        booleanParam(name: 'PUSH_DOCKER_IMAGE', defaultValue: false, description: 'Build and push the Docker image after a successful build')
        string(name: 'DOCKER_IMAGE_NAME', defaultValue: 'taskapi', description: 'Docker image name/repo (without tag)')
    }

    environment {
        DOCKER_CREDENTIALS_ID = 'docker-creds'   // Jenkins credential ID holding Docker registry username/password
        IMAGE_TAG             = "${params.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}"
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
                sh 'mvn -B -ntp clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B -ntp test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B -ntp package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Docker Build') {
            when {
                expression { return params.PUSH_DOCKER_IMAGE }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]){
                    sh "docker build -t ${DOCKER_USER}/${IMAGE_TAG} -t ${DOCKER_USER}/${IMAGE_TAG} ."
                }
                
            }
        }

        stage('Docker Push') {
            when {
                expression { return params.PUSH_DOCKER_IMAGE }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIALS_ID, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push "${DOCKER_USER}/${IMAGE_TAG}"
                        docker logout
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