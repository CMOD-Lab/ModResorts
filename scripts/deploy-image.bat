@echo off
setlocal enabledelayedexpansion

set APP_NAME=bookingcomp
set NAMESPACE=bookingcomp
set K8S_DIR=kubernetes

echo ============================================
echo   ModResorts BookingComp - Deploy to EKS
echo ============================================
echo.

rem ---- Collect deployment inputs ----
set /p AWS_REGION="Enter AWS Region (e.g. us-east-1): "
if "!AWS_REGION!"=="" (
    echo AWS Region is required. Exiting.
    exit /b 1
)

set /p CLUSTER_NAME="Enter EKS Cluster Name: "
if "!CLUSTER_NAME!"=="" (
    echo EKS Cluster Name is required. Exiting.
    exit /b 1
)

set /p IMAGE_URI="Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/bookingcomp:latest): "
if "!IMAGE_URI!"=="" (
    echo Image URI is required. Exiting.
    exit /b 1
)

echo.
echo --- Optional environment variable configuration ---
set /p REDIS_HOST_VAL="Enter REDIS_HOST (or press Enter to skip): "
set /p REDIS_PORT_VAL="Enter REDIS_PORT [6379] (or press Enter to skip): "
if "!REDIS_PORT_VAL!"=="" set REDIS_PORT_VAL=6379
set /p WEATHER_SERVICE_URL_VAL="Enter WEATHER_SERVICE_URL (or press Enter to skip): "
set /p WEATHER_API_KEY_VAL="Enter WEATHER_API_KEY (or press Enter to skip): "

rem ---- Configure kubectl for EKS ----
echo.
echo Configuring kubectl for EKS cluster: !CLUSTER_NAME! in !AWS_REGION! ...
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo Failed to configure kubectl. Exiting.
    exit /b 1
)

echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo Cannot connect to EKS cluster. Exiting.
    exit /b 1
)

rem ---- Copy manifests to temp directory ----
echo.
echo Updating Kubernetes manifests with deployment values...
set TEMP_DIR=%TEMP%\bookingcomp-k8s-deploy
if exist "!TEMP_DIR!" rmdir /s /q "!TEMP_DIR!"
xcopy /s /e /i /q "!K8S_DIR!" "!TEMP_DIR!" >nul

rem ---- Substitute placeholders using PowerShell ----
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '\{\{IMAGE_URI\}\}', '!IMAGE_URI!' -replace '\{\{REDIS_HOST\}\}', '!REDIS_HOST_VAL!' -replace '\{\{REDIS_PORT\}\}', '!REDIS_PORT_VAL!' -replace '\{\{WEATHER_SERVICE_URL\}\}', '!WEATHER_SERVICE_URL_VAL!' -replace '\{\{WEATHER_API_KEY\}\}', '!WEATHER_API_KEY_VAL!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
if !ERRORLEVEL! neq 0 (
    echo Failed to update deployment manifest. Exiting.
    exit /b 1
)

rem ---- Apply manifests ----
echo.
echo Applying Kubernetes manifests...
kubectl apply -f "!TEMP_DIR!\namespace.yaml"
if !ERRORLEVEL! neq 0 ( echo Failed to apply namespace.yaml & exit /b 1 )

kubectl apply -f "!TEMP_DIR!\deployment.yaml"
if !ERRORLEVEL! neq 0 ( echo Failed to apply deployment.yaml & exit /b 1 )

kubectl apply -f "!TEMP_DIR!\service.yaml"
if !ERRORLEVEL! neq 0 ( echo Failed to apply service.yaml & exit /b 1 )

kubectl apply -f "!TEMP_DIR!\ingress.yaml"
if !ERRORLEVEL! neq 0 ( echo Failed to apply ingress.yaml & exit /b 1 )

rem ---- Wait for rollout ----
echo.
echo Waiting for deployment rollout...
kubectl rollout status deployment/!APP_NAME! -n !NAMESPACE! --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo Deployment rollout failed. Check pod logs:
    echo   kubectl logs -l app=!APP_NAME! -n !NAMESPACE!
    exit /b 1
)

rem ---- Verify resources ----
echo.
echo Verifying deployed resources...
kubectl get pods,svc,ingress -n !NAMESPACE!

rem ---- Cleanup temp files ----
rmdir /s /q "!TEMP_DIR!"

echo.
echo ============================================
echo   Deployment complete!
echo   Namespace : !NAMESPACE!
echo   Image     : !IMAGE_URI!
echo ============================================
echo.
echo Rollback command (if needed):
echo   kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!

endlocal
