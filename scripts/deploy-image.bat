@echo off
setlocal enabledelayedexpansion

:: =============================================================================
:: deploy-image.bat - Deploy BookingComp to AWS EKS (Windows)
:: =============================================================================

set APP_NAME=bookingcomp
set NAMESPACE=bookingcomp
set K8S_DIR=kubernetes

echo ==============================================
echo   BookingComp - AWS EKS Deployment Script
echo ==============================================
echo.

:: ---------------------------------------------------------------------------
:: Collect AWS / EKS configuration
:: ---------------------------------------------------------------------------
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

set /p IMAGE_URI="Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/bookingcomp:latest): "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required.
    exit /b 1
)

echo.
echo --- Application Environment Variables ---
echo Press Enter to skip any variable.
echo.

set /p REDIS_HOST_VAL="Enter REDIS_HOST (ElastiCache endpoint): "
set /p REDIS_PORT_VAL="Enter REDIS_PORT [6379]: "
if "!REDIS_PORT_VAL!"=="" set REDIS_PORT_VAL=6379

set /p WEATHER_API_KEY_VAL="Enter WEATHER_API_KEY (or press Enter to skip): "
set /p REMOTE_SERVICE_URL_VAL="Enter REMOTE_SERVICE_URL (or press Enter to skip): "

echo.
echo --- Deployment Summary ---
echo   AWS Region   : !AWS_REGION!
echo   EKS Cluster  : !CLUSTER_NAME!
echo   Image URI    : !IMAGE_URI!
echo   Namespace    : !NAMESPACE!
echo.

:: ---------------------------------------------------------------------------
:: Configure kubectl for EKS
:: ---------------------------------------------------------------------------
echo Configuring kubectl for EKS cluster...
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl. Check AWS credentials and cluster name.
    exit /b 1
)

echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to EKS cluster.
    exit /b 1
)
echo.

:: ---------------------------------------------------------------------------
:: Prepare temporary manifest copies
:: ---------------------------------------------------------------------------
echo Preparing Kubernetes manifests...
set TMP_DIR=%TEMP%\bookingcomp-deploy-%RANDOM%
mkdir "!TMP_DIR!"
xcopy /E /I /Q "!K8S_DIR!" "!TMP_DIR!" >nul

set DEPLOY_YAML=!TMP_DIR!\deployment.yaml

:: Replace IMAGE_URI placeholder using PowerShell
powershell -Command "(Get-Content '!DEPLOY_YAML!') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '!DEPLOY_YAML!'"

if not "!REDIS_HOST_VAL!"=="" (
    powershell -Command "(Get-Content '!DEPLOY_YAML!') -replace '{{REDIS_HOST}}', '!REDIS_HOST_VAL!' | Set-Content '!DEPLOY_YAML!'"
)

powershell -Command "(Get-Content '!DEPLOY_YAML!') -replace '{{REDIS_PORT}}', '!REDIS_PORT_VAL!' | Set-Content '!DEPLOY_YAML!'"

if not "!WEATHER_API_KEY_VAL!"=="" (
    powershell -Command "(Get-Content '!DEPLOY_YAML!') -replace '{{WEATHER_API_KEY}}', '!WEATHER_API_KEY_VAL!' | Set-Content '!DEPLOY_YAML!'"
)

if not "!REMOTE_SERVICE_URL_VAL!"=="" (
    powershell -Command "(Get-Content '!DEPLOY_YAML!') -replace '{{REMOTE_SERVICE_URL}}', '!REMOTE_SERVICE_URL_VAL!' | Set-Content '!DEPLOY_YAML!'"
)

:: ---------------------------------------------------------------------------
:: Apply Kubernetes manifests in order
:: ---------------------------------------------------------------------------
echo Applying Kubernetes manifests...
echo.

echo [1/4] Applying namespace...
kubectl apply -f "!TMP_DIR!\namespace.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply namespace.
    rmdir /s /q "!TMP_DIR!"
    exit /b 1
)

echo [2/4] Applying deployment...
kubectl apply -f "!DEPLOY_YAML!"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply deployment.
    rmdir /s /q "!TMP_DIR!"
    exit /b 1
)

echo [3/4] Applying service...
kubectl apply -f "!TMP_DIR!\service.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply service.
    rmdir /s /q "!TMP_DIR!"
    exit /b 1
)

echo [4/4] Applying ingress...
kubectl apply -f "!TMP_DIR!\ingress.yaml"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to apply ingress.
    rmdir /s /q "!TMP_DIR!"
    exit /b 1
)

echo.

:: ---------------------------------------------------------------------------
:: Wait for rollout
:: ---------------------------------------------------------------------------
echo Waiting for deployment rollout...
kubectl rollout status deployment/!APP_NAME! -n !NAMESPACE! --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo.
    echo ERROR: Deployment rollout failed or timed out.
    echo Run the following to investigate:
    echo   kubectl describe pods -n !NAMESPACE!
    echo   kubectl logs -l app=!APP_NAME! -n !NAMESPACE!
    echo.
    echo To rollback:
    echo   kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!
    rmdir /s /q "!TMP_DIR!"
    exit /b 1
)

:: ---------------------------------------------------------------------------
:: Verify resources
:: ---------------------------------------------------------------------------
echo.
echo Verifying deployed resources...
kubectl get pods,svc,ingress -n !NAMESPACE!

:: ---------------------------------------------------------------------------
:: Display application URL
:: ---------------------------------------------------------------------------
echo.
echo Fetching application URL from Ingress...
for /f "tokens=*" %%i in ('kubectl get ingress !APP_NAME!-ingress -n !NAMESPACE! -o jsonpath^="{.status.loadBalancer.ingress[0].hostname}" 2^>nul') do set INGRESS_HOST=%%i

if not "!INGRESS_HOST!"=="" (
    echo.
    echo ==============================================
    echo   Application is accessible at:
    echo   http://!INGRESS_HOST!
    echo   Health check: http://!INGRESS_HOST!/health
    echo ==============================================
) else (
    echo Ingress hostname not yet assigned. Check again with:
    echo   kubectl get ingress -n !NAMESPACE!
)

rmdir /s /q "!TMP_DIR!"

echo.
echo Deployment complete!
echo.
echo Useful commands:
echo   kubectl get pods -n !NAMESPACE!
echo   kubectl logs -l app=!APP_NAME! -n !NAMESPACE!
echo   kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!

endlocal
exit /b 0
