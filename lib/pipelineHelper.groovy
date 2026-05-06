/**
 * lib/pipelineHelper.groovy
 *
 * Local Groovy helper library loaded at runtime by Jenkinsfile.inactive using
 * the built-in Jenkins `load` step — no plugins required.
 *
 * Usage in Jenkinsfile.inactive:
 *   def helper = load 'lib/pipelineHelper.groovy'
 *   def cfg    = helper.parseConfig('pipeline.yml')
 *   helper.buildImage(cfg.image_name)
 *
 * IMPORTANT: `return this` at the bottom is mandatory for `load` to work.
 */

// ---------------------------------------------------------------------------
// parseConfig
// Reads a simple key: value YAML file and returns a Map.
// Lines starting with # and blank lines are ignored.
// ---------------------------------------------------------------------------
Map parseConfig(String filePath) {
    echo "[pipelineHelper] Reading config from '${filePath}'..."
    def raw    = readFile(file: filePath)
    def config = [:]
    raw.readLines().each { line ->
        def trimmed = line.trim()
        if (trimmed && !trimmed.startsWith('#')) {
            def parts = trimmed.split(':', 2)
            if (parts.size() == 2) {
                config[parts[0].trim()] = parts[1].trim()
            }
        }
    }
    echo "[pipelineHelper] Config loaded: ${config}"
    return config
}

// ---------------------------------------------------------------------------
// printBanner  — decorative tutorial echo
// ---------------------------------------------------------------------------
void printBanner(String appName, String imageName, String containerName) {
    echo "========================================================"
    echo "  PulseLine Flask App — Jenkins Tutorial Pipeline"
    echo "========================================================"
    echo "  App Name      : ${appName}"
    echo "  Image Name    : ${imageName}"
    echo "  Container Name: ${containerName}"
    echo "  Build #       : ${env.BUILD_NUMBER}"
    echo "  Node          : ${env.NODE_NAME}"
    echo "========================================================"
}

// ---------------------------------------------------------------------------
// installDependencies
// ---------------------------------------------------------------------------
void installDependencies(String pythonCmd = 'python3', String venvDir = '.venv') {
    echo "[pipelineHelper] Creating virtual environment at '${venvDir}'..."
    if (isUnix()) {
        sh """
            set -e
            ${pythonCmd} -m venv ${venvDir}
            ${venvDir}/bin/python -m pip install --upgrade pip
            ${venvDir}/bin/python -m pip install -r requirements.txt
            echo 'Installed packages (venv):'
            ${venvDir}/bin/python -m pip list --format=columns
        """
    } else {
        bat """
            python -m venv ${venvDir}
            ${venvDir}\\Scripts\\python -m pip install --upgrade pip
            ${venvDir}\\Scripts\\python -m pip install -r requirements.txt
            echo Installed packages (venv):
            ${venvDir}\\Scripts\\python -m pip list --format=columns
        """
    }
    echo "[pipelineHelper] Dependencies installed in virtual environment."
}

// ---------------------------------------------------------------------------
// buildImage
// ---------------------------------------------------------------------------
void buildImage(String imageName) {
    echo "[pipelineHelper] Building Docker image '${imageName}'..."
    if (isUnix()) {
        sh "docker build --progress=plain -t ${imageName} ."
        sh "docker image inspect ${imageName} --format 'ID: {{.Id}} | Created: {{.Created}}'"
    } else {
        bat "docker build --progress=plain -t ${imageName} ."
    }
    echo "[pipelineHelper] Image '${imageName}' ready."
}

// ---------------------------------------------------------------------------
// deployContainer
// ---------------------------------------------------------------------------
void deployContainer(String containerName, String imageName, String port) {
    echo "[pipelineHelper] Deploying '${containerName}' from '${imageName}' on port ${port}..."
    if (isUnix()) {
        sh """
            docker rm -f ${containerName} || true
            docker run -d --name ${containerName} -p ${port}:${port} ${imageName}
            docker ps --filter name=${containerName} --format "table {{.ID}}\\t{{.Image}}\\t{{.Status}}\\t{{.Ports}}"
        """
    } else {
        bat "docker rm -f ${containerName} 2>nul & docker run -d --name ${containerName} -p ${port}:${port} ${imageName}"
    }
    echo "[pipelineHelper] Waiting for app to start..."
    sleep time: 5, unit: 'SECONDS'
    echo "[pipelineHelper] Container '${containerName}' is running."
}

// ---------------------------------------------------------------------------
// healthCheck
// ---------------------------------------------------------------------------
void healthCheck(String port) {
    echo "[pipelineHelper] Running health checks on port ${port}..."
    if (isUnix()) {
        sh """
            echo "GET http://127.0.0.1:${port}/ ..."
            curl -sf http://127.0.0.1:${port}/ && echo " -> OK"
            echo "GET http://127.0.0.1:${port}/hello ..."
            curl -sf http://127.0.0.1:${port}/hello && echo " -> OK"
        """
    } else {
        bat """
            powershell -NoProfile -Command ^
                "Write-Host 'GET /...'; (Invoke-WebRequest -UseBasicParsing http://127.0.0.1:${port}/).Content; ^
                 Write-Host 'GET /hello...'; (Invoke-WebRequest -UseBasicParsing http://127.0.0.1:${port}/hello).Content"
        """
    }
    echo "[pipelineHelper] All health checks passed."
}

// ---------------------------------------------------------------------------
// cleanup
// ---------------------------------------------------------------------------
void cleanup(String containerName) {
    echo "[pipelineHelper] Removing container '${containerName}'..."
    if (isUnix()) {
        sh "docker rm -f ${containerName} || true"
    } else {
        bat "docker rm -f ${containerName} 2>nul"
    }
    echo "[pipelineHelper] Cleanup done."
}

// ---------------------------------------------------------------------------
// runPipeline
// Orchestrates every stage. Call this from Jenkinsfile.inactive so that
// all stage definitions live inside the helper, not the Jenkinsfile.
// Uses try/catch/finally instead of post{} since this is scripted-style.
// ---------------------------------------------------------------------------
void runPipeline(Map cfg) {
    try {

        stage('Init') {
            echo '=============================='
            echo ' STAGE: Init'
            echo '=============================='
            printBanner(cfg.app_name, cfg.image_name, cfg.container_name)
        }

        stage('Install Dependencies') {
            echo '=============================='
            echo ' STAGE: Install Dependencies'
            echo '=============================='
            installDependencies(cfg.python_cmd ?: 'python3', cfg.venv_dir ?: '.venv')
        }

        stage('Build Image') {
            echo '=============================='
            echo ' STAGE: Build Image'
            echo '=============================='
            buildImage(cfg.image_name)
        }

        stage('Deploy') {
            echo '=============================='
            echo ' STAGE: Deploy'
            echo '=============================='
            deployContainer(cfg.container_name, cfg.image_name, cfg.port)
        }

        stage('Health Check') {
            echo '=============================='
            echo ' STAGE: Health Check'
            echo '=============================='
            healthCheck(cfg.port)
        }

        echo '=============================='
        echo ' BUILD SUCCEEDED'
        echo " App : ${cfg.app_name}"
        echo " URL : http://127.0.0.1:${cfg.port}/"
        echo '=============================='

    } catch (err) {
        echo "Build FAILED: ${err.getMessage()}"
        echo 'Dumping container logs for diagnostics...'
        if (isUnix()) {
            sh "docker logs ${cfg.container_name} || true"
        } else {
            bat "docker logs ${cfg.container_name} 2>nul"
        }
        throw err
    } finally {
        cleanup(cfg.container_name)
        echo "Pipeline complete. Build #${env.BUILD_NUMBER} finished."
    }
}

// REQUIRED — allows Jenkins `load` step to return this script as an object
return this
