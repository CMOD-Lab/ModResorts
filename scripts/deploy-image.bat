@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM deploy-image.bat - Deploy ModResorts to AWS EKS (Windows)
REM ============================================================

set APP_NAME=modresorts
set NAMESPACE=modresorts
set K8S_DIR=kubernetes

echo ==============================================
echo   ModResorts - AWS EKS Deployment Script
echo ==============================================
echo.

REM ---- Collect AWS / EKS configuration ----
set /p AWS_REGION="Enter AWS Region (e.g. us-east-1): "
if "!AWS_REGION!"=="" (
    echo ERROR: AWS Region is required.
    exit /b 1
)

set /p CLUSTER_NAME="Enter EKS Cluster Name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: EKS Cluster Name is required.
    exit /b 1
)

set /p IMAGE_URI="Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required.
    exit /b 1
)

echo.
echo ---- Optional Environment Variables ----
echo Press Enter to skip any variable.
echo.

set /p WEATHER_API_KEY_VAL="Enter WEATHER_API_KEY value (or press Enter to skip): "
set /p JNDI_FACTORY_VAL="Enter JNDI_FACTORY value (or press Enter to skip): "
set /p JNDI_PROVIDER_URL_VAL="Enter JNDI_PROVIDER_URL value (or press Enter to skip): "
set /p SERVER_DISPLAY_NAME_VAL="Enter SERVER_DISPLAY_NAME value (or press Enter to skip): "
set /p SERVER_FULL_NAME_VAL="Enter SERVER_FULL_NAME value (or press Enter to skip): "

echo.
echo ---- Configuring kubectl for EKS ----
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to update kubeconfig.
    exit /b 1
)

echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to cluster.
    exit /b 1
)

echo.
echo ---- Copying manifests for deployment ----
if exist "%TEMP%\modresorts-k8s-deploy" rmdir /s /q "%TEMP%\modresorts-k8s-deploy"
xcopy /s /e /i /q "%K8S_DIR%" "%TEMP%\modresorts-k8s-deploy"

echo ---- Updating Kubernetes manifests ----

REM Replace IMAGE_URI placeholder using PowerShell
powershell -Command "(Get-Content '%TEMP%\modresorts-k8s-deploy\deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '%TEMP%\modresorts-k8s-deploy\deployment.yaml'"

if not "!WEATHER_API_KEY_VAL!"=="" (
    powershell -Command "(Get-Content '%TEMP%\modresorts-k8s-deploy\deployment.yaml') -replace '{{WEATHER_API_KEY}}', '!WEATHER_API_KEY_VAL!' | Set-Content '%TEMP%\modresorts-k8s-deploy\deployment.yaml'"
)
if not "!JNDI_FACTORY_VAL!"=="" (
    powershell -Command "(Get-Content '%TEMP%\modresorts-k8s-deploy\deployment.yaml') -replace '{{JNDI_FACTORY}}', '!JNDI_FACTORY_VAL!' | Set-Content '%TEMP%\modresorts-k8s-deploy\deployment.yaml'"
)
if not "!JNDI_PROVIDER_URL_VAL!"=="" (
    powershell -Command "(Get-Content '%TEMP%\modresorts-k8s-deploy\deployment.yaml') -replace '{{JNDI_PROVIDER_URL}}', '!JNDI_PROVIDER_URL_VAL!' | Set-Content '%TEMP%\modresorts-k8s-deploy\deployment.yaml'"
)
if not "!SERVER_DISPLAY_NAME_VAL!"=="" (
    powershell -Command "(Get-Content '%TEMP%\modresorts-k8s-deploy\deployment.yaml') -replace '{{SERVER_DISPLAY_NAME}}', '!SERVER_DISPLAY_NAME_VAL!' | Set-Content '%TEMP%\modresorts-k8s-deploy\deployment.yaml'"
)
if not "!SERVER_FULL_NAME_VAL!"=="" (
    powershell -Command "(Get-Content '%TEMP%\modresorts-k8s-deploy\deployment.yaml') -replace '{{SERVER_FULL_NAME}}', '!SERVER_FULL_NAME_VAL!' | Set-Content '%TEMP%\modresorts-k8s-deploy\deployment.yaml'"
)

echo.
echo ---- Applying Kubernetes manifests ----

echo Applying namespace...
kubectl apply -f "%TEMP%\modresorts-k8s-deploy\namespace.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply namespace.
    exit /b 1
)

echo Applying deployment...
kubectl apply -f "%TEMP%\modresorts-k8s-deploy\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply deployment.
    exit /b 1
)

echo Applying service...
kubectl apply -f "%TEMP%\modresorts-k8s-deploy\service.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply service.
    exit /b 1
)

echo Applying ingress...
kubectl apply -f "%TEMP%\modresorts-k8s-deploy\ingress.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply ingress.
    exit /b 1
)

echo.
echo ---- Waiting for deployment rollout ----
kubectl rollout status deployment/%APP_NAME% -n %NAMESPACE% --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed. Initiating rollback...
    kubectl rollout undo deployment/%APP_NAME% -n %NAMESPACE%
    echo Rollback initiated. Check pod status with: kubectl get pods -n %NAMESPACE%
    exit /b 1
)

echo.
echo ---- Verifying deployed resources ----
kubectl get pods,svc,ingress -n %NAMESPACE%

echo.
echo ==============================================
echo   SUCCESS: ModResorts deployed to EKS!
echo ==============================================
echo.
echo Useful commands:
echo   kubectl get pods -n %NAMESPACE%
echo   kubectl logs -f deployment/%APP_NAME% -n %NAMESPACE%
echo   kubectl rollout undo deployment/%APP_NAME% -n %NAMESPACE%

REM Cleanup temp files
rmdir /s /q "%TEMP%\modresorts-k8s-deploy"

endlocal
