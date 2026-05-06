pipeline {
    agent any

    environment {
        IMAGE_NAME = "flask-python-hello:local"
        CONTAINER_NAME = "flask-python-hello-app"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Install Python Dependencies') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'python3 -m pip install --user -r requirements.txt'
                    } else {
                        bat 'python -m pip install --user -r requirements.txt'
                    }
                }
            }
        }

        stage('Build Docker Image Locally') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'docker build -t ${IMAGE_NAME} .'
                    } else {
                        bat 'docker build -t %IMAGE_NAME% .'
                    }
                }
            }
        }

        stage('Deploy Container Locally') {
            steps {
                script {
                    if (isUnix()) {
                        sh '''
                            set -e
                            docker rm -f ${CONTAINER_NAME} || true
                            docker run -d --name ${CONTAINER_NAME} -p 5000:5000 ${IMAGE_NAME}
                            sleep 5
                            curl -f http://127.0.0.1:5000/
                            curl -f http://127.0.0.1:5000/hello
                        '''
                    } else {
                        bat '''
                            powershell -NoProfile -Command "docker rm -f $env:CONTAINER_NAME 2>$null; docker run -d --name $env:CONTAINER_NAME -p 5000:5000 $env:IMAGE_NAME; Start-Sleep -Seconds 5; Invoke-WebRequest -UseBasicParsing http://127.0.0.1:5000/ | Out-Null; Invoke-WebRequest -UseBasicParsing http://127.0.0.1:5000/hello | Out-Null"
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                if (isUnix()) {
                    sh 'docker rm -f ${CONTAINER_NAME} || true'
                } else {
                    bat 'docker rm -f %CONTAINER_NAME% 2>nul'
                }
            }
        }
    }

}
