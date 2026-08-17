@echo off
setlocal enabledelayedexpansion

rem ============================================================
rem deploy-image.bat - Deploy ModResorts to AWS EKS (Windows)
rem ============================================================

set APP_NAME=modresorts
set NAMESPACE=modresorts

echo ============================================
echo   ModResorts - AWS EKS Deployment Script
echo ============================================
echo.

rem Prompt for AWS region
set /p AWS_REGION="Enter AWS Region (e.g. us-east-1): "
if "!AWS_REGION!"=="" (
    echo ERROR: AWS Region is required.
    exit /b 1
)

rem Prompt for EKS cluster name
set /p CLUSTER_NAME="Enter EKS Cluster Name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: EKS Cluster Name is required.
    exit /b 1
)

rem Prompt for Docker image URI
set /p IMAGE_URI="Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required.
    exit /b 1
)

echo.
echo --- Optional Application Environment Variables ---
echo (Press Enter to skip any variable)
echo.

set /p WEATHER_API_KEY_VAL="Enter WEATHER_API_KEY value (or press Enter to skip): "
set /p SERVER_DISPLAY_NAME_VAL="Enter SERVER_DISPLAY_NAME value (or press Enter to skip): "
set /p SERVER_FULL_NAME_VAL="Enter SERVER_FULL_NAME value (or press Enter to skip): "
set /p JNDI_FACTORY_VAL="Enter JNDI_FACTORY value (or press Enter to skip): "
set /p JNDI_PROVIDER_URL_VAL="Enter JNDI_PROVIDER_URL value (or press Enter to skip): "

if "!SERVER_DISPLAY_NAME_VAL!"=="" set SERVER_DISPLAY_NAME_VAL=modresorts-server
if "!SERVER_FULL_NAME_VAL!"=="" set SERVER_FULL_NAME_VAL=modresorts-server-full
if "!JNDI_FACTORY_VAL!"=="" set JNDI_FACTORY_VAL=com.sun.jndi.fscontext.RefFSContextFactory

echo.
echo --- Configuring kubectl for EKS ---
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to update kubeconfig. Check your AWS credentials and cluster name.
    exit /b 1
)

echo.
echo --- Verifying cluster connectivity ---
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to Kubernetes cluster.
    exit /b 1
)

echo.
echo --- Preparing Kubernetes manifests ---

rem Create temp directory for working copies
set WORK_DIR=%TEMP%\modresorts-deploy-%RANDOM%
mkdir "!WORK_DIR!"

rem Copy manifests to temp directory
for %%f in (kubernetes\namespace.yaml kubernetes\deployment.yaml kubernetes\service.yaml kubernetes\ingress.yaml) do (
    copy "%%f" "!WORK_DIR!\" >nul
)

rem Replace placeholders using PowerShell
powershell -Command "(Get-Content '!WORK_DIR!\deployment.yaml') -replace '\{\{IMAGE_URI\}\}', '!IMAGE_URI!' | Set-Content '!WORK_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!WORK_DIR!\deployment.yaml') -replace '\{\{WEATHER_API_KEY\}\}', '!WEATHER_API_KEY_VAL!' | Set-Content '!WORK_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!WORK_DIR!\deployment.yaml') -replace '\{\{SERVER_DISPLAY_NAME\}\}', '!SERVER_DISPLAY_NAME_VAL!' | Set-Content '!WORK_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!WORK_DIR!\deployment.yaml') -replace '\{\{SERVER_FULL_NAME\}\}', '!SERVER_FULL_NAME_VAL!' | Set-Content '!WORK_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!WORK_DIR!\deployment.yaml') -replace '\{\{JNDI_FACTORY\}\}', '!JNDI_FACTORY_VAL!' | Set-Content '!WORK_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!WORK_DIR!\deployment.yaml') -replace '\{\{JNDI_PROVIDER_URL\}\}', '!JNDI_PROVIDER_URL_VAL!' | Set-Content '!WORK_DIR!\deployment.yaml'"

echo.
echo --- Applying Kubernetes manifests ---

echo Applying namespace...
kubectl apply -f "!WORK_DIR!\namespace.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply namespace.
    rmdir /s /q "!WORK_DIR!"
    exit /b 1
)

echo Applying deployment...
kubectl apply -f "!WORK_DIR!\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply deployment.
    rmdir /s /q "!WORK_DIR!"
    exit /b 1
)

echo Applying service...
kubectl apply -f "!WORK_DIR!\service.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply service.
    rmdir /s /q "!WORK_DIR!"
    exit /b 1
)

echo Applying ingress...
kubectl apply -f "!WORK_DIR!\ingress.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply ingress.
    rmdir /s /q "!WORK_DIR!"
    exit /b 1
)

echo.
echo --- Waiting for deployment rollout ---
kubectl rollout status deployment/!APP_NAME! -n !NAMESPACE! --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed or timed out.
    echo.
    echo --- Rollback Instructions ---
    echo To rollback: kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!
    echo To check logs: kubectl logs -l app=!APP_NAME! -n !NAMESPACE!
    rmdir /s /q "!WORK_DIR!"
    exit /b 1
)

echo.
echo --- Verifying deployed resources ---
kubectl get pods,svc,ingress -n !NAMESPACE!

echo.
echo --- Application Access ---
echo Run the following to get the ingress hostname:
echo   kubectl get ingress modresorts-ingress -n !NAMESPACE!
echo.
echo Application path: /resorts/
echo Health Check path: /resorts/health

rem Cleanup temp directory
rmdir /s /q "!WORK_DIR!"

echo.
echo ============================================
echo   SUCCESS: ModResorts deployed to EKS!
echo   Namespace: !NAMESPACE!
echo   Image: !IMAGE_URI!
echo ============================================

endlocal
exit /b 0
