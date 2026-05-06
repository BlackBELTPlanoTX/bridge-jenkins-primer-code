pipeline {
    agent any

    environment {
        IMAGE_NAME    = "flask-python-hello:local"
        CONTAINER_NAME = "flask-python-hello-app"
        APP_PORT      = "5000"
        VENV_DIR      = ".venv"
        LOG_FOLLOW_SECONDS = "30"
        KEEP_CONTAINER = "true"
    }

    stages {

        stage('Checkout') {
            steps {
                echo "=============================="
                echo " STAGE: Checkout"
                echo "=============================="
                echo "Cloning source code from SCM..."
                checkout scm
                echo "Workspace ready at: ${env.WORKSPACE}"
                echo "Branch: ${env.GIT_BRANCH ?: 'unknown'}"
                echo "Commit: ${env.GIT_COMMIT ?: 'unknown'}"
            }
        }

        stage('Environment Info') {
            steps {
                echo "=============================="
                echo " STAGE: Environment Info"
                echo "=============================="
                echo "Build Number : ${env.BUILD_NUMBER}"
                echo "Build URL    : ${env.BUILD_URL}"
                echo "Node Name    : ${env.NODE_NAME}"
                echo "Image Name   : ${env.IMAGE_NAME}"
                echo "Container    : ${env.CONTAINER_NAME}"
                echo "App Port     : ${env.APP_PORT}"
                script {
                    if (isUnix()) {
                        sh 'echo "Docker version:"; docker --version'
                        sh 'echo "Python version:"; python3 --version'
                    } else {
                        bat 'echo Docker version: && docker --version'
                        bat 'echo Python version: && python --version'
                    }
                }
            }
        }

        stage('Install Python Dependencies') {
            steps {
                echo "=============================="
                echo " STAGE: Install Python Dependencies"
                echo "=============================="
                echo "Creating virtual environment and installing packages..."
                script {
                    if (isUnix()) {
                        sh '''
                            set -e
                            python3 -m venv ${VENV_DIR}
                            ${VENV_DIR}/bin/python -m pip install --upgrade pip
                            ${VENV_DIR}/bin/python -m pip install -r requirements.txt
                            echo "Installed packages (venv):"
                            ${VENV_DIR}/bin/python -m pip list --format=columns
                        '''
                    } else {
                        bat '''
                            python -m venv %VENV_DIR%
                            %VENV_DIR%\\Scripts\\python -m pip install --upgrade pip
                            %VENV_DIR%\\Scripts\\python -m pip install -r requirements.txt
                            echo Installed packages (venv):
                            %VENV_DIR%\\Scripts\\python -m pip list --format=columns
                        '''
                    }
                }
                echo "Python dependency installation complete."
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "=============================="
                echo " STAGE: Build Docker Image"
                echo "=============================="
                echo "Building image '${env.IMAGE_NAME}' from Dockerfile..."
                script {
                    if (isUnix()) {
                        sh 'docker build --progress=plain -t ${IMAGE_NAME} .'
                        sh 'echo "Image details:"; docker image inspect ${IMAGE_NAME} --format "ID: {{.Id}} | Size: {{.Size}} bytes | Created: {{.Created}}"'
                    } else {
                        bat 'docker build --progress=plain -t %IMAGE_NAME% .'
                        bat 'echo Image details: && docker image inspect %IMAGE_NAME% --format "ID: {{.Id}} | Created: {{.Created}}"'
                    }
                }
                echo "Docker image built successfully."
            }
        }

        stage('Deploy Container') {
            steps {
                echo "=============================="
                echo " STAGE: Deploy Container"
                echo "=============================="
                echo "Removing any existing container named '${env.CONTAINER_NAME}'..."
                script {
                    if (isUnix()) {
                        sh '''
                            set -e
                            docker rm -f ${CONTAINER_NAME} || true
                            echo "Spinning up new container..."
                            docker run -d --name ${CONTAINER_NAME} -p ${APP_PORT}:${APP_PORT} ${IMAGE_NAME}
                            echo "Container started. Listing running containers:"
                            docker ps --filter name=${CONTAINER_NAME} --format "table {{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
                        '''
                    } else {
                        bat '''
                            powershell -NoProfile -Command ^
                                "docker rm -f $env:CONTAINER_NAME 2>$null; ^
                                 Write-Host 'Spinning up new container...'; ^
                                 docker run -d --name $env:CONTAINER_NAME -p $env:APP_PORT:$env:APP_PORT $env:IMAGE_NAME; ^
                                 Write-Host 'Running containers:'; ^
                                 docker ps --filter name=$env:CONTAINER_NAME"
                        '''
                    }
                }
                echo "Container deployed. Waiting for app to start..."
                sleep time: 5, unit: 'SECONDS'
            }
        }

        stage('Observe Container Logs') {
            steps {
                echo "=============================="
                echo " STAGE: Observe Container Logs"
                echo "=============================="
                echo "Following container logs immediately for ${env.LOG_FOLLOW_SECONDS}s..."
                script {
                    if (isUnix()) {
                        sh 'timeout ${LOG_FOLLOW_SECONDS}s docker logs -f ${CONTAINER_NAME} || true'
                    } else {
                        bat 'powershell -NoProfile -Command "$sec=[int]$env:LOG_FOLLOW_SECONDS; $job = Start-Job -ScriptBlock { param($n) docker logs -f $n } -ArgumentList $env:CONTAINER_NAME; Wait-Job -Job $job -Timeout $sec | Out-Null; Stop-Job -Job $job -ErrorAction SilentlyContinue; Receive-Job -Job $job -ErrorAction SilentlyContinue"'
                    }
                }
            }
        }
    }

    post {
        success {
            echo "=============================="
            echo " BUILD SUCCEEDED"
            echo " Image  : ${env.IMAGE_NAME}"
            echo " App URL: http://127.0.0.1:${env.APP_PORT}/"
            echo "=============================="
        }
        failure {
            echo "Build FAILED. Dumping container logs for diagnostics..."
            script {
                if (isUnix()) {
                    sh 'docker logs ${CONTAINER_NAME} || true'
                } else {
                    bat 'docker logs %CONTAINER_NAME% 2>nul || echo No container logs available'
                }
            }
        }
        always {
            echo "Cleanup disabled. Container '${env.CONTAINER_NAME}' is left running."
            echo "Pipeline complete. Build #${env.BUILD_NUMBER} finished."
        }
    }

}
