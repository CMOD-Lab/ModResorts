@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM ModResorts - Deploy to AWS EKS Script (Windows)
REM ============================================================

echo ============================================
echo   ModResorts - Deploy to AWS EKS
echo ============================================
echo.

REM Prompt for AWS region
set /p AWS_REGION="Enter AWS Region (e.g. us-east-1): "
if "!AWS_REGION!"=="" (
    echo ERROR: AWS Region cannot be empty.
    exit /b 1
)

REM Prompt for EKS cluster name
set /p CLUSTER_NAME="Enter EKS Cluster Name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: EKS Cluster Name cannot be empty.
    exit /b 1
)

REM Prompt for Docker image URI
set /p IMAGE_URI="Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI cannot be empty.
    exit /b 1
)

echo.
echo --------------------------------------------
echo   Optional: Application Environment Variables
echo   (Press Enter to skip any variable)
echo --------------------------------------------

set /p WEATHER_API_KEY_VAL="Enter WEATHER_API_KEY value (or press Enter to skip): "
set /p DB_URL_VAL="Enter DB_URL value (or press Enter to skip): "
set /p DB_USERNAME_VAL="Enter DB_USERNAME value (or press Enter to skip): "
set /p DB_PASSWORD_VAL="Enter DB_PASSWORD value (or press Enter to skip): "

echo.
echo --------------------------------------------
echo   Configuring kubectl for EKS cluster...
echo --------------------------------------------
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl for EKS cluster.
    exit /b 1
)

echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to Kubernetes cluster.
    exit /b 1
)

echo.
echo --------------------------------------------
echo   Updating Kubernetes manifests...
echo --------------------------------------------

REM Use PowerShell to replace placeholders in deployment.yaml
powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to update IMAGE_URI in deployment.yaml
    exit /b 1
)

if not "!WEATHER_API_KEY_VAL!"=="" (
    powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{WEATHER_API_KEY}}', '!WEATHER_API_KEY_VAL!' | Set-Content kubernetes\deployment.yaml"
) else (
    powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{WEATHER_API_KEY}}', '' | Set-Content kubernetes\deployment.yaml"
)

if not "!DB_URL_VAL!"=="" (
    powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{DB_URL}}', '!DB_URL_VAL!' | Set-Content kubernetes\deployment.yaml"
) else (
    powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{DB_URL}}', '' | Set-Content kubernetes\deployment.yaml"
)

if not "!DB_USERNAME_VAL!"=="" (
    powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{DB_USERNAME}}', '!DB_USERNAME_VAL!' | Set-Content kubernetes\deployment.yaml"
) else (
    powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{DB_USERNAME}}', '' | Set-Content kubernetes\deployment.yaml"
)

if not "!DB_PASSWORD_VAL!"=="" (
    powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{DB_PASSWORD}}', '!DB_PASSWORD_VAL!' | Set-Content kubernetes\deployment.yaml"
) else (
    powershell -Command "(Get-Content kubernetes\deployment.yaml) -replace '{{DB_PASSWORD}}', '' | Set-Content kubernetes\deployment.yaml"
)

echo.
echo --------------------------------------------
echo   Applying Kubernetes manifests...
echo --------------------------------------------

echo Applying namespace...
kubectl apply -f kubernetes\namespace.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply namespace.
    exit /b 1
)

echo Applying deployment...
kubectl apply -f kubernetes\deployment.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply deployment.
    exit /b 1
)

echo Applying service...
kubectl apply -f kubernetes\service.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply service.
    exit /b 1
)

echo Applying ingress...
kubectl apply -f kubernetes\ingress.yaml
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply ingress.
    exit /b 1
)

echo.
echo --------------------------------------------
echo   Waiting for deployment rollout...
echo --------------------------------------------
kubectl rollout status deployment/modresorts -n modresorts --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed. Rolling back...
    kubectl rollout undo deployment/modresorts -n modresorts
    echo Rollback initiated. Check pod status with: kubectl get pods -n modresorts
    exit /b 1
)

echo.
echo --------------------------------------------
echo   Verifying deployed resources...
echo --------------------------------------------
kubectl get pods,svc,ingress -n modresorts

echo.
echo ============================================
echo   SUCCESS: ModResorts deployed to EKS!
echo ============================================
echo.
echo Useful commands:
echo   kubectl get pods -n modresorts
echo   kubectl logs -f deployment/modresorts -n modresorts
echo   kubectl describe deployment modresorts -n modresorts
echo   kubectl rollout undo deployment/modresorts -n modresorts

endlocal
